package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Removes inventory items only when the complete requested amount is available. */
public record TakeItemAction(ResourceLocation item, NumericExpression count) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "take_item");
    public TakeItemAction {
        item = Objects.requireNonNull(item, "item");
        count = Objects.requireNonNull(count, "count");
    }
    @Override public ResourceLocation type() { return TYPE; }
}
