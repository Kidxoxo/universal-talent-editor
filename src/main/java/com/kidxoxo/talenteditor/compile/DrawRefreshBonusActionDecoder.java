package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.DrawRefreshBonusAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class DrawRefreshBonusActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "amount", "pools");

    @Override
    public ResourceLocation id() {
        return DrawRefreshBonusAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.ACTIVE);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        int amount = JsonAccess.integer(
                json, "amount", 1, 1, 100, context.source(), context.path(), context.report()
        );
        return Optional.of(new DrawRefreshBonusAction(
                amount, JsonAccess.idList(json, "pools", true, context.source(), context.path(), context.report())
        ));
    }
}
