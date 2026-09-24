package com.kidxoxo.talenteditor.network;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentRelationSummaryTest {
    @Test
    void extractsEditableTalentPrerequisitesAndCountsOtherConditions() {
        TalentRelationSummary summary = TalentRelationSummary.from(JsonParser.parseString("""
                {
                  "acquireCondition": {
                    "type": "talent_editor:any",
                    "children": [
                      {"type":"has_talent","talent":"example:root"},
                      {"type":"has_item","item":"minecraft:diamond","count":2}
                    ]
                  }
                }
                """).getAsJsonObject());

        assertEquals(List.of(new ResourceLocation("example", "root")), summary.acquireTalents());
        assertTrue(summary.acquireEditable());
        assertEquals("any", summary.acquireMode());
        assertEquals(1, summary.acquireOtherConditions());
    }

    @Test
    void preservesPositiveLinksButMarksNestedAndNegativeConditionsAdvanced() {
        TalentRelationSummary summary = TalentRelationSummary.from(JsonParser.parseString("""
                {
                  "acquireCondition": {
                    "type": "all",
                    "children": [
                      {"type":"has_talent","talent":"example:root"},
                      {"type":"not","child":{"type":"has_talent","talent":"example:blocked"}}
                    ]
                  }
                }
                """).getAsJsonObject());

        assertEquals(List.of(new ResourceLocation("example", "root")), summary.acquireTalents());
        assertFalse(summary.acquireEditable());
        assertEquals("all", summary.acquireMode());
        assertEquals(1, summary.acquireOtherConditions());
    }
}
