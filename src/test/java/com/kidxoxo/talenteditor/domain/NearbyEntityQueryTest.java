package com.kidxoxo.talenteditor.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NearbyEntityQueryTest {
    @Test
    void parsesBoundedComposableQuery() {
        NearbyEntityQuery query = NearbyEntityQuery.parse(
                "radius=100;kind=player;relation=ally;type=minecraft:player;include_self=false"
        );
        assertEquals(100.0D, query.radius());
        assertEquals(NearbyEntityQuery.Kind.PLAYER, query.kind());
        assertEquals(EntityRelation.ALLY, query.relation());
        assertEquals("minecraft:player", query.type().orElseThrow().toString());
        assertFalse(query.includeSelf());
    }

    @Test
    void rejectsUnboundedOrUnknownQueries() {
        assertThrows(IllegalArgumentException.class,
                () -> NearbyEntityQuery.parse("kind=player"));
        assertThrows(IllegalArgumentException.class,
                () -> NearbyEntityQuery.parse("radius=8;unknown=value"));
    }
}
