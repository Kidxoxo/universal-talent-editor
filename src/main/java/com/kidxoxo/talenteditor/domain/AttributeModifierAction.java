package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record AttributeModifierAction(
        ResourceLocation attribute,
        double amount,
        AttributeOperation operation,
        String modifierKey,
        boolean showInDescription
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "attribute_modifier");

    public AttributeModifierAction {
        attribute = Objects.requireNonNull(attribute, "attribute");
        operation = Objects.requireNonNull(operation, "operation");
        if (modifierKey == null || modifierKey.isBlank()) {
            throw new IllegalArgumentException("modifierKey must not be blank");
        }
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}

