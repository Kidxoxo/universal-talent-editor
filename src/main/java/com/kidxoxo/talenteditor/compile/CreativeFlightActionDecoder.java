package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.CreativeFlightAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class CreativeFlightActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type");

    @Override
    public ResourceLocation id() {
        return CreativeFlightAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.ACTIVE);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        return Optional.of(new CreativeFlightAction());
    }
}
