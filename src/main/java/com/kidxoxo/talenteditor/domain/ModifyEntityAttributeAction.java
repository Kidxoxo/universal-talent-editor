package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Changes the base value of a registered attribute on selected living entities. */
public record ModifyEntityAttributeAction(
        ResourceLocation attribute,
        DamageOperation operation,
        double amount,
        double minimum,
        double maximum,
        LivingEntityTarget target,
        double radius
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "modify_entity_attribute");
    public static final double LIMIT = 1.0E9D;

    public ModifyEntityAttributeAction {
        attribute = Objects.requireNonNull(attribute, "attribute");
        operation = Objects.requireNonNull(operation, "operation");
        target = Objects.requireNonNull(target, "target");
        if (!Double.isFinite(amount) || !Double.isFinite(minimum) || !Double.isFinite(maximum)
                || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("Attribute action numbers must be finite");
        }
        if (minimum < -LIMIT || maximum > LIMIT || minimum > maximum) {
            throw new IllegalArgumentException("Attribute bounds must stay within -1e9..1e9");
        }
        if (operation == DamageOperation.ADD && Math.abs(amount) > LIMIT
                || operation == DamageOperation.MULTIPLY && (amount < 0.0D || amount > 1_000.0D)) {
            throw new IllegalArgumentException("Invalid attribute modification amount");
        }
        if (radius < 0.5D || radius > 64.0D) throw new IllegalArgumentException("radius must be 0.5..64");
    }

    public double apply(double current) {
        double safe = Double.isFinite(current) ? current : 0.0D;
        double changed = operation == DamageOperation.ADD ? safe + amount : safe * amount;
        return Math.max(minimum, Math.min(maximum, changed));
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
