package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.AttributeOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class TimedAttributeModifierService {
    private static final Map<LeaseKey, Lease> LEASES = new HashMap<>();

    private TimedAttributeModifierService() {
    }

    public static boolean apply(
            LivingEntity target,
            ResourceLocation attributeId,
            double amount,
            AttributeOperation operation,
            String modifierKey,
            UUID owner,
            ResourceLocation talent,
            int durationTicks
    ) {
        if (!Double.isFinite(amount)) return false;
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        AttributeInstance instance = attribute == null ? null : target.getAttribute(attribute);
        if (instance == null) return false;
        String seed = TalentEditorMod.MOD_ID + "|timed|" + owner + "|" + talent + "|" + modifierKey;
        UUID modifierId = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
        instance.removeModifier(modifierId);
        instance.addTransientModifier(new AttributeModifier(
                modifierId, TalentEditorMod.MOD_ID + ":timed/" + modifierKey,
                amount, operation(operation)
        ));
        long expires = target.level().getGameTime() + durationTicks;
        LEASES.put(new LeaseKey(target.getUUID(), attributeId, modifierId), new Lease(new WeakReference<>(target), expires));
        return true;
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<LeaseKey, Lease>> iterator = LEASES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LeaseKey, Lease> entry = iterator.next();
            Lease lease = entry.getValue();
            LivingEntity target = lease.target().get();
            if (target != null && target.isAlive() && now < lease.expiresAt()) continue;
            if (target != null) {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(entry.getKey().attribute());
                AttributeInstance instance = attribute == null ? null : target.getAttribute(attribute);
                if (instance != null) instance.removeModifier(entry.getKey().modifier());
            }
            iterator.remove();
        }
    }

    private static AttributeModifier.Operation operation(AttributeOperation operation) {
        return switch (operation) {
            case ADDITION -> AttributeModifier.Operation.ADDITION;
            case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
            case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
        };
    }

    private record LeaseKey(UUID entity, ResourceLocation attribute, UUID modifier) {}
    private record Lease(WeakReference<LivingEntity> target, long expiresAt) {}
}
