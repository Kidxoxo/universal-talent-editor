package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.GrantTalentAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class GrantTalentActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "talent", "onlyIfNeverAcquired", "synthesized");
    @Override public ResourceLocation id() { return GrantTalentAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE);
    }
    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        return Optional.of(new GrantTalentAction(
                JsonAccess.requiredId(json, "talent", "talent_editor:missing", true,
                        context.source(), context.path(), context.report()),
                JsonAccess.bool(json, "onlyIfNeverAcquired", true,
                        context.source(), context.path(), context.report()),
                JsonAccess.bool(json, "synthesized", false,
                        context.source(), context.path(), context.report())
        ));
    }
}
