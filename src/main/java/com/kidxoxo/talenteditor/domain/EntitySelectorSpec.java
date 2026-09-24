package com.kidxoxo.talenteditor.domain;

import java.util.Objects;

public record EntitySelectorSpec(
        LivingEntityTarget target,
        double radius,
        EntityRelation relation,
        EntityOrder order,
        int limit
) {
    public EntitySelectorSpec {
        target = Objects.requireNonNull(target, "target");
        relation = Objects.requireNonNull(relation, "relation");
        order = Objects.requireNonNull(order, "order");
        if (!Double.isFinite(radius) || radius < 0.5D || radius > 64.0D) {
            throw new IllegalArgumentException("radius must be 0.5..64");
        }
        if (limit < 1 || limit > 256) throw new IllegalArgumentException("limit must be 1..256");
    }
}
