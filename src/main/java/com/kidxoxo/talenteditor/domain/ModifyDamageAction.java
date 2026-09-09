package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record ModifyDamageAction(
        DamageOperation operation,
        double amount,
        double minimum,
        double maximum
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "modify_damage");
    public static final double MAX_DAMAGE = 1.0E9D;

    public ModifyDamageAction {
        operation = Objects.requireNonNull(operation, "operation");
        if (!Double.isFinite(amount) || !Double.isFinite(minimum) || !Double.isFinite(maximum)) {
            throw new IllegalArgumentException("Damage values must be finite");
        }
        if (minimum < 0.0D || maximum > MAX_DAMAGE || minimum > maximum) {
            throw new IllegalArgumentException("Damage bounds must satisfy 0 <= minimum <= maximum <= 1e9");
        }
        if (operation == DamageOperation.ADD && (amount < -MAX_DAMAGE || amount > MAX_DAMAGE)) {
            throw new IllegalArgumentException("Add amount must be in range -1e9..1e9");
        }
        if (operation == DamageOperation.MULTIPLY && (amount < 0.0D || amount > 1_000.0D)) {
            throw new IllegalArgumentException("Multiply amount must be in range 0..1000");
        }
    }

    public float apply(float current) {
        double safeCurrent = Float.isFinite(current) ? Math.max(0.0D, current) : 0.0D;
        double modified = operation == DamageOperation.ADD
                ? safeCurrent + amount
                : safeCurrent * amount;
        return (float) Math.max(minimum, Math.min(maximum, modified));
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
