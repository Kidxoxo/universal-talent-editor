package com.kidxoxo.talenteditor.domain;

import java.util.List;

public record ConfiguredItemUseDefinition(
        boolean enabled,
        int cooldownTicks,
        int consumeCount,
        List<CompiledAction> actions
) {
    public static final ConfiguredItemUseDefinition DISABLED =
            new ConfiguredItemUseDefinition(false, 0, 0, List.of());

    public ConfiguredItemUseDefinition {
        if (cooldownTicks < 0) throw new IllegalArgumentException("cooldownTicks must not be negative");
        if (consumeCount < 0) throw new IllegalArgumentException("consumeCount must not be negative");
        actions = List.copyOf(actions);
    }
}
