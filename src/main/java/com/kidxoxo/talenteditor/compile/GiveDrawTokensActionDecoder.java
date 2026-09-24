package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.GiveDrawTokensAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class GiveDrawTokensActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "amount");
    @Override public ResourceLocation id() { return GiveDrawTokensAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE,
                ActionPlacement.ON_SYNTHESIZE, ActionPlacement.ON_REMOVE);
    }
    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        return Optional.of(new GiveDrawTokensAction(
                ExpressionDecoderSupport.expression(context, json, "amount", "1")
        ));
    }
}
