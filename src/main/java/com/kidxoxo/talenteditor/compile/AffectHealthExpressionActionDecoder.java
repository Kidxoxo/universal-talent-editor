package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.AffectHealthExpressionAction;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.HealthOperation;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class AffectHealthExpressionActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "operation", "expression", "maximum", "invertNegative",
            "target", "radius", "relation", "order", "limit"
    );
    @Override public ResourceLocation id() { return AffectHealthExpressionAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() { return Set.of(ActionPlacement.TRIGGER); }

    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        HealthOperation operation;
        try {
            operation = HealthOperation.parse(JsonAccess.string(
                    json, "operation", "damage", false, context.source(), context.path(), context.report()
            ));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_HEALTH_OPERATION", context.source(),
                    JsonAccess.child(context.path(), "operation"), "Expected damage or heal");
            operation = HealthOperation.DAMAGE;
        }
        double maximum = JsonAccess.decimal(json, "maximum", 1.0E9D, 0.0D, 1.0E9D,
                context.source(), context.path(), context.report());
        return Optional.of(new AffectHealthExpressionAction(
                operation, ExpressionDecoderSupport.expression(context, json, "expression", "0"), maximum,
                JsonAccess.bool(json, "invertNegative", false,
                        context.source(), context.path(), context.report()),
                EntitySelectorDecoderSupport.selector(context, json)
        ));
    }
}
