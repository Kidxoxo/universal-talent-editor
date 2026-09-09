package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record HealEntityAction(LivingEntityTarget target, float amount, double radius) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "heal_entity");

    public HealEntityAction {
        target = Objects.requireNonNull(target, "target");
        if (!Float.isFinite(amount) || amount <= 0.0F || amount > 1_000_000.0F) {
            throw new IllegalArgumentException("healing amount must be in range (0, 1000000]");
        }
        if (!Double.isFinite(radius) || radius < 0.5D || radius > 64.0D) {
            throw new IllegalArgumentException("radius must be 0.5..64");
        }
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
