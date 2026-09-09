package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.catalog.AttributeDescriptor;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReferenceDiagnosticsTest {
    private static final ResourceLocation COMMON = id("talent_editor:common");

    @Test
    void summarizesDistinctReferencesAndKeepsMissingSeparateFromMalformed() {
        EditorBootstrap bootstrap = bootstrap();

        ReferenceDiagnostics.Summary summary = ReferenceDiagnostics.summarize(
                bootstrap,
                ResourceReferencePickerScreen.Source.TALENT,
                "example:active, example:active, example:disabled, example:missing, bad id",
                false
        );

        assertEquals(new ReferenceDiagnostics.Summary(1, 1, 0, 1, 1), summary);
    }

    @Test
    void distinguishesUnavailableDefinitionsAndCurrentTagCatalogEntries() {
        EditorBootstrap bootstrap = bootstrap();

        assertEquals(ReferenceDiagnostics.Status.UNAVAILABLE, ReferenceDiagnostics.inspect(
                bootstrap, ResourceReferencePickerScreen.Source.COUNTER, id("example:counter")
        ));
        assertEquals(ReferenceDiagnostics.Status.AVAILABLE, ReferenceDiagnostics.inspect(
                bootstrap, ResourceReferencePickerScreen.Source.COUNTER_CATEGORY,
                id("example:combat_events")
        ));
        assertEquals(ReferenceDiagnostics.Status.AVAILABLE, ReferenceDiagnostics.inspect(
                bootstrap, ResourceReferencePickerScreen.Source.ITEM_TAG, id("minecraft:swords")
        ));
        assertEquals(ReferenceDiagnostics.Status.NOT_PRESENT, ReferenceDiagnostics.inspect(
                bootstrap, ResourceReferencePickerScreen.Source.ITEM_TAG, id("example:future_tag")
        ));
        assertEquals(ReferenceDiagnostics.Status.UNAVAILABLE, ReferenceDiagnostics.inspect(
                bootstrap, ResourceReferencePickerScreen.Source.ATTRIBUTE,
                id("example:not_attached")
        ));
        assertEquals(ReferenceDiagnostics.Status.UNAVAILABLE, ReferenceDiagnostics.inspect(
                bootstrap, ResourceReferencePickerScreen.Source.ITEM, id("example:optional_relic")
        ));
        assertEquals(
                "example:rare=0.2, example:epic=0",
                PoolAdvancedScreen.appendMapping("example:rare=0.2", id("example:epic"), "0")
        );
        assertEquals(
                "example:rare=0.2",
                PoolAdvancedScreen.appendMapping("example:rare=0.2", id("example:rare"), "0")
        );
    }

    private static EditorBootstrap bootstrap() {
        return new EditorBootstrap(
                1L,
                "hash",
                List.of(new EditorBootstrap.RaritySummary(
                        COMMON, "Common", 0xFFFFFFFF, 0, true, true, 1.0D
                )),
                List.of(
                        new EditorBootstrap.TalentSummary(
                                id("example:active"), "Active", COMMON, true, true,
                                List.of(id("example:combat"))
                        ),
                        new EditorBootstrap.TalentSummary(
                                id("example:disabled"), "Disabled", COMMON, false, true, List.of()
                        )
                ),
                List.of(new EditorBootstrap.PoolSummary(
                        id("example:main"), "Main", true, 3, 1, 24, true, Map.of()
                )),
                List.of(new EditorBootstrap.CounterSummary(
                        id("example:counter"), "Counter", "kill", true, false,
                        List.of(id("example:combat_events")), "condition_only"
                )),
                List.of(),
                List.of(),
                false,
                List.of(id("minecraft:skeletons")),
                List.of(id("minecraft:swords")),
                List.of(id("minecraft:is_fire")),
                List.of(new AttributeDescriptor(
                        id("example:not_attached"), "attribute.example.not_attached", 0.0D,
                        null, null, true, false
                )),
                List.of(new EditorBootstrap.ConfiguredItemSummary(
                        id("example:optional_relic"), "Optional Relic",
                        true, false, 7, false, "file/example_pack", 0
                ))
        );
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
