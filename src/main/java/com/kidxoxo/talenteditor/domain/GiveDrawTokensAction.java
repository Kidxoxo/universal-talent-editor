package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record GiveDrawTokensAction(NumericExpression amount) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "give_draw_tokens");
    public GiveDrawTokensAction { amount = Objects.requireNonNull(amount, "amount"); }
    @Override public ResourceLocation type() { return TYPE; }
}
