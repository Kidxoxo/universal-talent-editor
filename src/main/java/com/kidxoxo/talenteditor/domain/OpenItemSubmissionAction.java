package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Opens a server-authoritative, click-to-confirm item submission session. */
public record OpenItemSubmissionAction(
        ResourceLocation item,
        NumericExpression count,
        int timeoutTicks,
        boolean consume
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "open_item_submission");

    public OpenItemSubmissionAction {
        item = Objects.requireNonNull(item, "item");
        count = Objects.requireNonNull(count, "count");
        if (timeoutTicks < 20 || timeoutTicks > 72_000) throw new IllegalArgumentException("Invalid submission timeout");
    }

    @Override public ResourceLocation type() { return TYPE; }
}
