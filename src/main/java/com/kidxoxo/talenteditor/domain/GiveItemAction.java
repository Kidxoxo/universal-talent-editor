package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record GiveItemAction(
        ResourceLocation item,
        NumericExpression count,
        boolean dropOverflow
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "give_item");
    public GiveItemAction {
        item = Objects.requireNonNull(item, "item");
        count = Objects.requireNonNull(count, "count");
    }
    @Override public ResourceLocation type() { return TYPE; }
}
