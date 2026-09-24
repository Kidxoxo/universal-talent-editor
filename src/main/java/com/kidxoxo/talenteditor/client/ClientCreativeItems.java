package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.item.ConfiguredItem;
import com.kidxoxo.talenteditor.item.ModItems;
import com.kidxoxo.talenteditor.item.ModCreativeTabs;
import com.kidxoxo.talenteditor.network.ConfiguredItemCatalogPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import java.util.List;

/** Client-only creative inventory projection of the server's configured item definitions. */
public final class ClientCreativeItems {
    private static List<ConfiguredItemCatalogPacket.Entry> items = List.of();
    private static boolean catalogReceived;
    private static boolean rebuildPending;

    private ClientCreativeItems() {
    }

    public static void accept(ConfiguredItemCatalogPacket packet) {
        items = packet.items();
        catalogReceived = true;
        rebuildPending = true;
        refreshIfNeeded();
    }

    public static void clear() {
        items = List.of();
        catalogReceived = false;
        rebuildPending = false;
    }

    public static void refreshIfNeeded() {
        if (rebuildPending && rebuildTabs()) rebuildPending = false;
    }

    public static void addTo(BuildCreativeModeTabContentsEvent event) {
        if (!catalogReceived) {
            event.accept(ModItems.TALENT_BOOK);
            event.accept(ModItems.TALENT_POINT);
            return;
        }
        for (ConfiguredItemCatalogPacket.Entry entry : items) event.accept(preview(entry));
    }

    static ItemStack preview(ConfiguredItemCatalogPacket.Entry entry) {
        ResourceLocation id = entry.id();
        Item item = id.equals(ModItems.TALENT_BOOK_ID) ? ModItems.TALENT_BOOK.get()
                : id.equals(ModItems.TALENT_POINT_ID) ? ModItems.TALENT_POINT.get()
                : ModItems.CONFIGURED_ITEM.get();
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString(ConfiguredItem.DEFINITION_TAG, id.toString());
        stack.getOrCreateTag().putBoolean(ConfiguredItem.GLINT_TAG, entry.glint());
        if (entry.customModelData() > 0) {
            stack.getOrCreateTag().putInt("CustomModelData", entry.customModelData());
        }
        stack.setHoverName(Component.literal(entry.name()));
        if (!entry.lore().isEmpty()) {
            ListTag lore = new ListTag();
            entry.lore().forEach(line -> lore.add(StringTag.valueOf(
                    Component.Serializer.toJson(Component.literal(line)))));
            stack.getOrCreateTagElement("display").put("Lore", lore);
        }
        // No revision tag: the server re-applies the full definition after a creative pick-up.
        return stack;
    }

    private static boolean rebuildTabs() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) return false;
        CreativeModeTabs.tryRebuildTabContents(minecraft.getConnection().enabledFeatures(),
                minecraft.player.hasPermissions(2), minecraft.level.registryAccess());
        // Vanilla skips a rebuild when features/permissions are unchanged. Item definitions
        // can change independently, so force only our own tab to rebuild from the new catalog.
        ModCreativeTabs.ITEMS.get().buildContents(new CreativeModeTab.ItemDisplayParameters(
                minecraft.getConnection().enabledFeatures(), minecraft.player.hasPermissions(2),
                minecraft.level.registryAccess()
        ));
        return true;
    }
}
