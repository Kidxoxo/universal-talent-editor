package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentTreeLayoutTest {
    @Test
    void placesPrerequisitesBeforeDependentTalents() {
        EditorBootstrap.TalentSummary root = talent("root", List.of(), List.of());
        EditorBootstrap.TalentSummary middle = talent("middle", List.of(root.id()), List.of());
        EditorBootstrap.TalentSummary finalTalent = talent("final", List.of(), List.of(middle.id()));

        TalentTreeLayout.Result result = TalentTreeLayout.arrange(List.of(finalTalent, middle, root));

        assertTrue(result.positions().get(root.id()).depth()
                < result.positions().get(middle.id()).depth());
        assertTrue(result.positions().get(middle.id()).depth()
                < result.positions().get(finalTalent.id()).depth());
        assertTrue(result.width() >= TalentTreeLayout.NODE_WIDTH);
    }

    @Test
    void laysOutCyclesDeterministicallyWithoutDroppingNodes() {
        EditorBootstrap.TalentSummary one = talent(
                "one", List.of(new ResourceLocation("example", "two")), List.of()
        );
        EditorBootstrap.TalentSummary two = talent("two", List.of(one.id()), List.of());

        TalentTreeLayout.Result first = TalentTreeLayout.arrange(List.of(one, two));
        TalentTreeLayout.Result second = TalentTreeLayout.arrange(List.of(two, one));

        assertEquals(first.positions(), second.positions());
        assertEquals(2, first.positions().size());
    }

    @Test
    void separatesDisconnectedFamiliesAndAvoidsNodeCollisions() {
        EditorBootstrap.TalentSummary a = talent("a", List.of(), List.of());
        EditorBootstrap.TalentSummary b = talent("b", List.of(a.id()), List.of());
        EditorBootstrap.TalentSummary c = talent("c", List.of(), List.of());
        EditorBootstrap.TalentSummary d = talent("d", List.of(c.id()), List.of());
        TalentTreeLayout.Result result = TalentTreeLayout.arrange(List.of(d, b, c, a));

        assertTrue(result.positions().get(a.id()).y() < result.positions().get(c.id()).y());
        assertEquals(0, result.positions().get(a.id()).x());
        assertEquals(0, result.positions().get(c.id()).x());
        result.positions().values().forEach(left -> result.positions().values().stream()
                .filter(right -> right != left)
                .forEach(right -> assertTrue(
                        left.x() + TalentTreeLayout.NODE_WIDTH <= right.x()
                                || right.x() + TalentTreeLayout.NODE_WIDTH <= left.x()
                                || left.y() + TalentTreeLayout.NODE_HEIGHT <= right.y()
                                || right.y() + TalentTreeLayout.NODE_HEIGHT <= left.y()
                )));
    }

    private static EditorBootstrap.TalentSummary talent(
            String path, List<ResourceLocation> acquire, List<ResourceLocation> synthesis
    ) {
        return new EditorBootstrap.TalentSummary(
                new ResourceLocation("example", path), path,
                new ResourceLocation("talent_editor", "common"), true, true,
                List.of(), "test", 0, java.util.Optional.empty(), acquire,
                true, "all", 0, !synthesis.isEmpty(), synthesis, 0
        );
    }
}
