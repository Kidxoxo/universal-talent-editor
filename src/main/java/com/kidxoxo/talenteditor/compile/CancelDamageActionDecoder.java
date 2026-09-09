package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CancelDamageAction;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class CancelDamageActionDecoder implements ActionDecoder {
    @Override
    public ResourceLocation id() {
        return CancelDamageAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, Set.of("type"), context.source(), context.path(), context.report());
        return Optional.of(new CancelDamageAction());
    }
}
