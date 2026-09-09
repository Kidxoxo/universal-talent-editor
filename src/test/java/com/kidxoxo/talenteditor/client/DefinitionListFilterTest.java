package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefinitionListFilterTest {
    private static final ResourceLocation COMMON = id("talent_editor:common");

    @Test
    void filtersTalentsByTokensStateAndActualPackSource() {
        EditorBootstrap bootstrap = bootstrap();

        assertEquals(List.of(id("example:blade")), DefinitionListFilter.talents(
                bootstrap, "combat blade", DefinitionListFilter.State.ACTIVE, "file/custom_pack"
        ).stream().map(EditorBootstrap.TalentSummary::id).toList());
        assertEquals(List.of(id("example:locked")), DefinitionListFilter.talents(
                bootstrap, "", DefinitionListFilter.State.UNAVAILABLE, ""
        ).stream().map(EditorBootstrap.TalentSummary::id).toList());
        assertEquals(List.of(id("example:disabled")), DefinitionListFilter.talents(
                bootstrap, "", DefinitionListFilter.State.DISABLED, ""
        ).stream().map(EditorBootstrap.TalentSummary::id).toList());
    }

    @Test
    void searchesTypeSpecificMetadata() {
        EditorBootstrap bootstrap = bootstrap();

        assertEquals(1, DefinitionListFilter.pools(
                bootstrap, "rare", DefinitionListFilter.State.ALL, ""
        ).size());
        assertEquals(1, DefinitionListFilter.counters(
                bootstrap, "kill combat_events", DefinitionListFilter.State.ACTIVE, ""
        ).size());
        assertEquals(0, DefinitionListFilter.rarities(
                bootstrap, "", DefinitionListFilter.State.UNAVAILABLE, ""
        ).size());
    }

    @Test
    void cyclesSortedSourcesAndReturnsToAll() {
        List<String> sources = DefinitionListFilter.sources(bootstrap());

        assertEquals(List.of("builtin/talent_editor", "file/custom_pack", "talent_editor_generated"), sources);
        assertEquals("builtin/talent_editor", DefinitionListFilter.nextSource("", sources));
        assertEquals("file/custom_pack", DefinitionListFilter.nextSource("builtin/talent_editor", sources));
        assertEquals("", DefinitionListFilter.nextSource("talent_editor_generated", sources));
        assertEquals("custom_pack", DefinitionListFilter.compactSource("file/custom_pack"));
    }

    @Test
    void filtersLayoutsByTextureStateAndActualSource() {
        EditorBootstrap base = bootstrap();
        EditorBootstrap withLayouts = new EditorBootstrap(
                base.revision(), base.contentHash(), base.rarities(), base.talents(), base.pools(), base.counters(),
                List.of(
                        new EditorBootstrap.LayoutSummary(
                                id("example:illustrated"), true, true, 10,
                                Optional.of(id("example:textures/gui/book.png")),
                                48, 3, 5, 40, 30, 16, 4, 2, true, "file/custom_pack", 0
                        ),
                        new EditorBootstrap.LayoutSummary(
                                id("example:disabled"), false, false, 0, Optional.empty(),
                                42, 2, 4, 58, 30, 16, 0, 0, true, "talent_editor_generated", 0
                        )
                ),
                base.references(), base.referencesTruncated(), base.entityTypeTags(), base.itemTags(),
                base.damageTypeTags(), base.attributes()
        );

        assertEquals(List.of(id("example:illustrated")), DefinitionListFilter.layouts(
                withLayouts, "book.png 10", DefinitionListFilter.State.ACTIVE, "file/custom_pack"
        ).stream().map(EditorBootstrap.LayoutSummary::id).toList());
        assertEquals(List.of(id("example:illustrated")), DefinitionListFilter.layouts(
                withLayouts, "2 4", DefinitionListFilter.State.ALL, ""
        ).stream().map(EditorBootstrap.LayoutSummary::id).toList());
        assertEquals(List.of(id("example:disabled")), DefinitionListFilter.layouts(
                withLayouts, "", DefinitionListFilter.State.DISABLED, ""
        ).stream().map(EditorBootstrap.LayoutSummary::id).toList());
    }

    @Test
    void filtersConfiguredItemsByNameModelStateAndSource() {
        EditorBootstrap base = bootstrap();
        EditorBootstrap withItems = new EditorBootstrap(
                base.revision(), base.contentHash(), base.rarities(), base.talents(), base.pools(), base.counters(),
                base.layouts(), base.references(), base.referencesTruncated(), base.entityTypeTags(), base.itemTags(),
                base.damageTypeTags(), base.attributes(),
                List.of(
                        new EditorBootstrap.ConfiguredItemSummary(
                                id("example:shard"), "Talent Shard", true, true, 1001, true,
                                "file/custom_pack", 0
                        ),
                        new EditorBootstrap.ConfiguredItemSummary(
                                id("example:locked"), "Locked Relic", true, false, 2002, false,
                                "talent_editor_generated", 0
                        )
                )
        );

        assertEquals(List.of(id("example:shard")), DefinitionListFilter.configuredItems(
                withItems, "shard 1001", DefinitionListFilter.State.ACTIVE, "file/custom_pack"
        ).stream().map(EditorBootstrap.ConfiguredItemSummary::id).toList());
        assertEquals(List.of(id("example:locked")), DefinitionListFilter.configuredItems(
                withItems, "", DefinitionListFilter.State.UNAVAILABLE, ""
        ).stream().map(EditorBootstrap.ConfiguredItemSummary::id).toList());
        assertEquals(List.of("file/custom_pack", "talent_editor_generated"),
                DefinitionListFilter.configuredItemSources(withItems));
    }

    private static EditorBootstrap bootstrap() {
        return new EditorBootstrap(
                1L,
                "hash",
                List.of(
                        new EditorBootstrap.RaritySummary(
                                COMMON, "Common", 0xFFFFFF, 0, true, true, 1.0D, "builtin/talent_editor"
                        ),
                        new EditorBootstrap.RaritySummary(
                                id("example:rare"), "Rare", 0xFF00FF, 1, false, true, 1.0D, "file/custom_pack"
                        )
                ),
                List.of(
                        new EditorBootstrap.TalentSummary(
                                id("example:blade"), "Blade", COMMON, true, true,
                                List.of(id("example:combat")), "file/custom_pack"
                        ),
                        new EditorBootstrap.TalentSummary(
                                id("example:locked"), "Locked", COMMON, true, false, List.of(),
                                "talent_editor_generated"
                        ),
                        new EditorBootstrap.TalentSummary(
                                id("example:disabled"), "Disabled", COMMON, false, true, List.of(),
                                "talent_editor_generated"
                        )
                ),
                List.of(new EditorBootstrap.PoolSummary(
                        id("example:main"), "Main", true, 3, 1, 10, true,
                        Map.of(id("example:rare"), 1.0D), "file/custom_pack"
                )),
                List.of(new EditorBootstrap.CounterSummary(
                        id("example:kills"), "Kills", "kill", true, true,
                        List.of(id("example:combat_events")), "full", "talent_editor_generated"
                )),
                List.of()
        );
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
