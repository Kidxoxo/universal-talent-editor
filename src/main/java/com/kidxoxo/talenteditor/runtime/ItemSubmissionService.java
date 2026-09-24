package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.ItemSubmissionOpenPacket;
import com.kidxoxo.talenteditor.network.ItemSubmissionResultPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One pending, server-authoritative submission session per player. */
public final class ItemSubmissionService {
    private final Map<UUID, Session> sessions = new HashMap<>();

    public boolean open(ServerPlayer player, ResourceLocation itemId, int count, int timeoutTicks, boolean consume) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null || count < 1) return false;
        Session session = new Session(UUID.randomUUID(), itemId, count, consume,
                player.serverLevel().getGameTime() + timeoutTicks);
        sessions.put(player.getUUID(), session);
        EditorNetwork.sendItemSubmissionOpen(player, new ItemSubmissionOpenPacket(
                session.id(), session.item(), session.count(), session.consume(), timeoutTicks
        ));
        return true;
    }

    public void submit(ServerPlayer player, UUID sessionId) {
        Session session = sessions.get(player.getUUID());
        if (session == null || !session.id().equals(sessionId)) {
            EditorNetwork.sendItemSubmissionResult(player,
                    new ItemSubmissionResultPacket(sessionId, ItemSubmissionResultPacket.Status.NOT_FOUND));
            return;
        }
        if (player.serverLevel().getGameTime() > session.expiresAt()) {
            sessions.remove(player.getUUID());
            EditorNetwork.sendItemSubmissionResult(player,
                    new ItemSubmissionResultPacket(sessionId, ItemSubmissionResultPacket.Status.EXPIRED));
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(session.item());
        if (item == null || count(player, item) < session.count()) {
            EditorNetwork.sendItemSubmissionResult(player,
                    new ItemSubmissionResultPacket(sessionId, ItemSubmissionResultPacket.Status.INSUFFICIENT_ITEMS));
            return;
        }
        if (session.consume()) remove(player, item, session.count());
        sessions.remove(player.getUUID());
        EditorNetwork.sendItemSubmissionResult(player,
                new ItemSubmissionResultPacket(sessionId, ItemSubmissionResultPacket.Status.ACCEPTED));
        ItemStack eventStack = new ItemStack(item, Math.min(item.getMaxStackSize(), session.count()));
        TalentEditorMod.TRIGGERS.fireSubmission(player, eventStack, session.count());
    }

    public void cancel(ServerPlayer player, UUID sessionId) {
        Session session = sessions.get(player.getUUID());
        if (session != null && session.id().equals(sessionId)) sessions.remove(player.getUUID());
    }

    public void remove(ServerPlayer player) { sessions.remove(player.getUUID()); }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void remove(ServerPlayer player, Item item, int count) {
        int remaining = count;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private record Session(UUID id, ResourceLocation item, int count, boolean consume, long expiresAt) { }
}
