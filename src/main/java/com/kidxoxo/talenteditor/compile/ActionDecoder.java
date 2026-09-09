package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

public interface ActionDecoder {
    ResourceLocation id();

    Set<ActionPlacement> supportedPlacements();

    Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json);
}

