package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.RevokeTalentAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class RevokeTalentActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "talent", "retainEffects", "retainCategoryContribution", "retainTriggers"
    );
    @Override public ResourceLocation id() { return RevokeTalentAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() { return Set.of(ActionPlacement.TRIGGER); }
    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        return Optional.of(new RevokeTalentAction(
                JsonAccess.requiredId(json, "talent", context.talentId().toString(), true,
                        context.source(), context.path(), context.report()),
                JsonAccess.bool(json, "retainEffects", false, context.source(), context.path(), context.report()),
                JsonAccess.bool(json, "retainCategoryContribution", false,
                        context.source(), context.path(), context.report()),
                JsonAccess.bool(json, "retainTriggers", false,
                        context.source(), context.path(), context.report())
        ));
    }
}
