package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.AttributeModifierAction;
import com.kidxoxo.talenteditor.domain.AttributeOperation;
import com.kidxoxo.talenteditor.domain.CreativeFlightAction;
import com.kidxoxo.talenteditor.domain.DynamicAttributeModifierAction;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TalentAttributeRuntime {
    private static final String FLIGHT_MARKER = TalentEditorMod.MOD_ID + ":creative_flight_granted";
    private static final Map<UUID, List<AppliedModifier>> STATIC_APPLIED = new HashMap<>();
    private static final Map<UUID, List<AppliedModifier>> DYNAMIC_APPLIED = new HashMap<>();
    private static final Set<UUID> FLIGHT_GRANTED = new HashSet<>();

    private TalentAttributeRuntime() {
    }

    public static void refreshAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(TalentAttributeRuntime::refresh);
    }

    public static void refresh(ServerPlayer player) {
        removeModifiers(player, STATIC_APPLIED);
        Set<ResourceLocation> owned = PlayerTalents.owned(player);
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        ArrayList<AppliedModifier> applied = new ArrayList<>();
        if (!owned.isEmpty()) {
            snapshot.attributeBindings().forEach((attributeId, bindings) -> {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
                AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
                if (instance == null) return;
                for (ActiveAttributeBinding binding : bindings) {
                    if (!owned.contains(binding.talentId())) continue;
                    AttributeModifierAction action = binding.action();
                    UUID modifierId = modifierId(binding.talentId(), attributeId, action.modifierKey());
                    instance.removeModifier(modifierId);
                    instance.addTransientModifier(new AttributeModifier(
                            modifierId,
                            modifierName(binding.talentId(), action.modifierKey()),
                            action.amount(),
                            operation(action.operation())
                    ));
                    applied.add(new AppliedModifier(attributeId, modifierId));
                }
            });
        }
        if (!applied.isEmpty()) STATIC_APPLIED.put(player.getUUID(), List.copyOf(applied));
        refreshDynamic(player);
    }

    /** Reconciles changing formula values and flight without rebuilding constant attribute modifiers. */
    public static void refreshDynamic(ServerPlayer player) {
        Set<ResourceLocation> owned = PlayerTalents.owned(player);
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        ArrayList<DesiredModifier> desired = new ArrayList<>();
        boolean creativeFlight = false;
        for (ResourceLocation talentId : owned) {
            var talent = snapshot.talents().get(talentId);
            if (talent == null || !talent.enabled() || !talent.available()) continue;
            for (var action : talent.activeEffects()) {
                if (action instanceof DynamicAttributeModifierAction dynamic) {
                    double source = switch (dynamic.source()) {
                        case COUNTER -> PlayerTalents.counter(player, dynamic.counter().orElseThrow());
                        case HEALTH_PERCENT -> player.getMaxHealth() <= 0.0F ? 0.0D
                                : clamp01(player.getHealth() / player.getMaxHealth());
                        case HUNGER_PERCENT -> clamp01(player.getFoodData().getFoodLevel() / 20.0D);
                    };
                    desired.add(new DesiredModifier(
                            talentId, dynamic.attribute(), dynamic.modifierKey(),
                            dynamic.amount(source), dynamic.operation()
                    ));
                } else if (action instanceof CreativeFlightAction) {
                    creativeFlight = true;
                }
            }
        }
        reconcileDynamicModifiers(player, desired);
        reconcileCreativeFlight(player, creativeFlight);
    }

    public static void removeTracked(ServerPlayer player) {
        removeModifiers(player, STATIC_APPLIED);
        removeModifiers(player, DYNAMIC_APPLIED);
        revokeCreativeFlight(player);
    }

    private static void removeModifiers(ServerPlayer player, Map<UUID, List<AppliedModifier>> tracked) {
        List<AppliedModifier> previous = tracked.remove(player.getUUID());
        if (previous == null) return;
        for (AppliedModifier applied : previous) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(applied.attribute());
            AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
            if (instance != null) instance.removeModifier(applied.modifierId());
        }
    }

    private static void reconcileDynamicModifiers(ServerPlayer player, List<DesiredModifier> desired) {
        Map<ModifierIdentity, DesiredModifier> wanted = new HashMap<>();
        for (DesiredModifier modifier : desired) {
            UUID id = modifierId(modifier.talent(), modifier.attribute(), modifier.modifierKey());
            wanted.put(new ModifierIdentity(modifier.attribute(), id), modifier);
        }
        List<AppliedModifier> previous = DYNAMIC_APPLIED.getOrDefault(player.getUUID(), List.of());
        for (AppliedModifier applied : previous) {
            ModifierIdentity identity = new ModifierIdentity(applied.attribute(), applied.modifierId());
            if (wanted.containsKey(identity)) continue;
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(applied.attribute());
            AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
            if (instance != null) instance.removeModifier(applied.modifierId());
        }

        ArrayList<AppliedModifier> applied = new ArrayList<>();
        wanted.forEach((identity, modifier) -> {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(identity.attribute());
            AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
            if (instance == null) return;
            AttributeModifier existing = instance.getModifier(identity.modifierId());
            AttributeModifier.Operation wantedOperation = operation(modifier.operation());
            if (existing == null || Double.compare(existing.getAmount(), modifier.amount()) != 0
                    || existing.getOperation() != wantedOperation) {
                instance.removeModifier(identity.modifierId());
                instance.addTransientModifier(new AttributeModifier(
                        identity.modifierId(), modifierName(modifier.talent(), modifier.modifierKey()),
                        modifier.amount(), wantedOperation
                ));
            }
            applied.add(new AppliedModifier(identity.attribute(), identity.modifierId()));
        });
        if (applied.isEmpty()) DYNAMIC_APPLIED.remove(player.getUUID());
        else DYNAMIC_APPLIED.put(player.getUUID(), List.copyOf(applied));
    }

    private static void reconcileCreativeFlight(ServerPlayer player, boolean shouldFly) {
        UUID id = player.getUUID();
        if (shouldFly) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.getPersistentData().putBoolean(FLIGHT_MARKER, true);
                FLIGHT_GRANTED.add(id);
                player.onUpdateAbilities();
            } else if (player.getPersistentData().getBoolean(FLIGHT_MARKER)) {
                FLIGHT_GRANTED.add(id);
            }
            return;
        }
        revokeCreativeFlight(player);
    }

    private static void revokeCreativeFlight(ServerPlayer player) {
        boolean tracked = FLIGHT_GRANTED.remove(player.getUUID());
        boolean persisted = player.getPersistentData().getBoolean(FLIGHT_MARKER);
        if (!tracked && !persisted) return;
        player.getPersistentData().remove(FLIGHT_MARKER);
        if (player.isCreative() || player.isSpectator()) return;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    public static UUID modifierId(ResourceLocation talent, ResourceLocation attribute, String modifierKey) {
        String seed = TalentEditorMod.MOD_ID + "|" + talent + "|" + attribute + "|" + modifierKey;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private static AttributeModifier.Operation operation(AttributeOperation operation) {
        return switch (operation) {
            case ADDITION -> AttributeModifier.Operation.ADDITION;
            case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
            case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
        };
    }

    private static String modifierName(ResourceLocation talent, String modifierKey) {
        return TalentEditorMod.MOD_ID + ":" + talent + "/" + modifierKey;
    }

    static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private record AppliedModifier(ResourceLocation attribute, UUID modifierId) {
    }

    private record ModifierIdentity(ResourceLocation attribute, UUID modifierId) {
    }

    private record DesiredModifier(
            ResourceLocation talent,
            ResourceLocation attribute,
            String modifierKey,
            double amount,
            AttributeOperation operation
    ) {
    }
}
