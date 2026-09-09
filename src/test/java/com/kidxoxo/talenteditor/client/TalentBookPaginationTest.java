package com.kidxoxo.talenteditor.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition.PageDefinition;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition.SlotDefinition;
import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TalentBookPaginationTest {
    @Test
    void reservesLaterFixedTalentsBeforeFillingEarlierAutomaticSlots() {
        var items = List.of(id("a"), id("b"), id("c"), id("d"), id("e"));
        var plan = TalentBookPagination.place(items, List.of(
                page("first", slot(null), slot("missing")),
                page("second", slot("a"), slot(null))
        ), 2);
        assertEquals(List.of(List.of(1, -1), List.of(0, 2), List.of(3, 4)), plan.pages());
        assertEquals(1, plan.pageFor(0));
        assertEquals(0, plan.indexAt(1, 0));
        assertEquals(-1, plan.indexAt(0, 1));
        assertEquals(3, plan.firstIndex(2));
    }

    @Test
    void fixedOnlyPagesLeaveUnownedHolesAndStillAllowOverflowAndRemoval() {
        var pages = List.of(page("fixed", slot("a"), slot("missing")), page("empty", slot("later")));
        var before = TalentBookPagination.place(List.of(id("a"), id("b")), pages, 1);
        assertEquals(List.of(List.of(0, -1), List.of(-1), List.of(1)), before.pages());
        assertEquals(-1, before.firstIndex(1));
        var after = TalentBookPagination.place(List.of(id("b")), pages, 1);
        assertEquals(List.of(List.of(-1, -1), List.of(-1), List.of(0)), after.pages());
        assertEquals(List.of(List.of(0, 1)), TalentBookPagination.place(
                List.of(id("a"), id("b")), List.of(), 2).pages());
    }

    @Test
    void placementNeverLosesOrDuplicatesVisibleEntriesAcrossPageCapacities() {
        for (int count = 0; count <= 80; count++) {
            var items = java.util.stream.IntStream.range(0, count).mapToObj(i -> id("t" + i)).toList();
            for (int capacity : List.of(1, 3, 8, 80)) {
                var plan = TalentBookPagination.place(items, List.of(
                        page("one", slot(null), slot("t7"), slot("absent")),
                        page("two", slot("t0"), slot(null), slot("t50"))
                ), capacity);
                var assigned = plan.pages().stream().flatMap(List::stream).filter(i -> i >= 0).sorted().toList();
                assertEquals(java.util.stream.IntStream.range(0, count).boxed().toList(), assigned);
                for (int i = 0; i < count; i++) {
                    org.junit.jupiter.api.Assertions.assertTrue(plan.pageFor(i) >= 0);
                }
            }
        }
    }

    private static ResourceLocation id(String path) { return new ResourceLocation("example", path); }

    private static SlotDefinition slot(String talent) {
        return new SlotDefinition("slot", 0, 0, 58, 30, 16,
                Optional.ofNullable(talent).map(TalentBookPaginationTest::id));
    }

    private static PageDefinition page(String name, SlotDefinition... slots) {
        return new PageDefinition(name, Optional.empty(), List.of(slots));
    }

    @Test
    void mapsVariableConfiguredPagesBeforeRepeatingFallbackCapacity() {
        List<Integer> configured = List.of(1, 3);

        assertEquals(4, TalentBookPagination.pageCount(8, configured, 2));
        assertEquals(0, TalentBookPagination.pageStart(0, configured, 2));
        assertEquals(1, TalentBookPagination.pageStart(1, configured, 2));
        assertEquals(4, TalentBookPagination.pageStart(2, configured, 2));
        assertEquals(6, TalentBookPagination.pageStart(3, configured, 2));
        assertEquals(0, TalentBookPagination.pageForIndex(0, configured, 2));
        assertEquals(1, TalentBookPagination.pageForIndex(1, configured, 2));
        assertEquals(1, TalentBookPagination.pageForIndex(3, configured, 2));
        assertEquals(2, TalentBookPagination.pageForIndex(4, configured, 2));
        assertEquals(3, TalentBookPagination.pageForIndex(7, configured, 2));
    }

    @Test
    void preservesLegacyFallbackOnlyPaginationAndRejectsZeroCapacity() {
        assertEquals(1, TalentBookPagination.pageCount(0, List.of(), 8));
        assertEquals(3, TalentBookPagination.pageCount(17, List.of(), 8));
        assertEquals(16, TalentBookPagination.pageStart(2, List.of(), 8));
        assertEquals(2, TalentBookPagination.pageForIndex(16, List.of(), 8));
        assertThrows(IllegalArgumentException.class,
                () -> TalentBookPagination.pageCount(1, List.of(), 0));
    }
}
