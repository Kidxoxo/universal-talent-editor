package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.TakeItemAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class TakeItemActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "item", "count");
    @Override public ResourceLocation id() { return TakeItemAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE,
                ActionPlacement.ON_SYNTHESIZE, ActionPlacement.ON_REMOVE);
    }

    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation item = JsonAccess.requiredId(json, "item", "minecraft:air", true,
                context.source(), context.path(), context.report());
        if (!context.environment().hasItem(item)) {
            context.report().error("UNKNOWN_ITEM", context.source(), JsonAccess.child(context.path(), "item"),
                    "Item is not registered in the current server environment: " + item);
        }
        return Optional.of(new TakeItemAction(
                item, ExpressionDecoderSupport.expression(context, json, "count", "1")
        ));
    }
}
