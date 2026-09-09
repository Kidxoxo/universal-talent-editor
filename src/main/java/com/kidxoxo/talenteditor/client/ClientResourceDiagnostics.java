package com.kidxoxo.talenteditor.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Predicate;

final class ClientResourceDiagnostics {
    private ClientResourceDiagnostics() {
    }

    static Result inspect(String value, Predicate<ResourceLocation> available) {
        Objects.requireNonNull(available, "available");
        String raw = value == null ? "" : value.strip();
        if (raw.isBlank()) return new Result(State.EMPTY, null);
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) return new Result(State.INVALID, null);
        return new Result(available.test(id) ? State.AVAILABLE : State.MISSING, id);
    }

    enum State {
        EMPTY,
        INVALID,
        AVAILABLE,
        MISSING
    }

    record Result(State state, ResourceLocation id) {
        boolean present() {
            return state == State.AVAILABLE;
        }
    }
}
