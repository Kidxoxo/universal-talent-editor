package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.OpenItemSubmissionAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class OpenItemSubmissionActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "item", "count", "timeoutTicks", "consume");
    @Override public ResourceLocation id() { return OpenItemSubmissionAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() { return Set.of(ActionPlacement.TRIGGER); }
    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation item = JsonAccess.requiredId(json, "item", "minecraft:emerald", true,
                context.source(), context.path(), context.report());
        if (!context.environment().hasItem(item)) {
            if (context.missingMods().isEmpty()) context.report().error("UNKNOWN_SUBMISSION_ITEM", context.source(),
                    JsonAccess.child(context.path(), "item"), "Submission item is not registered: " + item);
            else context.report().warning("SUBMISSION_ITEM_UNAVAILABLE_WITH_MISSING_MOD", context.source(),
                    JsonAccess.child(context.path(), "item"), "Submission item is unavailable while required mods are missing: " + item);
        }
        return Optional.of(new OpenItemSubmissionAction(
                item, ExpressionDecoderSupport.expression(context, json, "count", "1"),
                JsonAccess.integer(json, "timeoutTicks", 600, 20, 72_000,
                        context.source(), context.path(), context.report()),
                JsonAccess.bool(json, "consume", true, context.source(), context.path(), context.report())
        ));
    }
}
