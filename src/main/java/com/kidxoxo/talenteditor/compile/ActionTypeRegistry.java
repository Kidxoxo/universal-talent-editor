package com.kidxoxo.talenteditor.compile;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ActionTypeRegistry {
    private final Map<ResourceLocation, ActionDecoder> decoders;

    private ActionTypeRegistry(Map<ResourceLocation, ActionDecoder> decoders) {
        this.decoders = Collections.unmodifiableMap(new LinkedHashMap<>(decoders));
    }

    public ActionDecoder get(ResourceLocation id) {
        return decoders.get(id);
    }

    public Map<ResourceLocation, ActionDecoder> entries() {
        return decoders;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final LinkedHashMap<ResourceLocation, ActionDecoder> decoders = new LinkedHashMap<>();

        public Builder register(ActionDecoder decoder) {
            ActionDecoder previous = decoders.putIfAbsent(decoder.id(), decoder);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate action type: " + decoder.id());
            }
            return this;
        }

        public ActionTypeRegistry build() {
            return new ActionTypeRegistry(decoders);
        }
    }
}

