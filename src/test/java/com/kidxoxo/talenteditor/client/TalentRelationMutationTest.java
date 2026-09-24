package com.kidxoxo.talenteditor.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentRelationMutationTest {
    private static final ResourceLocation TARGET = new ResourceLocation("example", "advanced");
    private static final ResourceLocation ROOT = new ResourceLocation("example", "root");

    @Test
    void addsAndRemovesAcquisitionPrerequisiteWithoutChangingOtherRows() {
        TalentDraft draft = TalentDraft.parse("""
                {"schema":2,"text":{"name":"Advanced"},"rarity":"talent_editor:common",
                 "acquireCondition":{"type":"all","children":[
                   {"type":"has_item","item":"minecraft:diamond","count":2}
                 ]}}
                """);

        assertEquals(TalentRelationMutation.Result.CHANGED, TalentRelationMutation.apply(
                draft, TalentTreeEditRequest.mutate(TARGET, TalentTreeEditRequest.Page.ACQUISITION,
                        TalentTreeEditRequest.Mutation.ADD, ROOT)
        ));
        assertTrue(draft.requirements().rows().stream().anyMatch(row -> row.target().equals(ROOT)));
        assertEquals(2, draft.requirements().rows().size());

        assertEquals(TalentRelationMutation.Result.CHANGED, TalentRelationMutation.apply(
                draft, TalentTreeEditRequest.mutate(TARGET, TalentTreeEditRequest.Page.ACQUISITION,
                        TalentTreeEditRequest.Mutation.REMOVE, ROOT)
        ));
        assertEquals(1, draft.requirements().rows().size());
    }

    @Test
    void addingSynthesisPrerequisiteEnablesSynthesisAndRejectsSelfReference() {
        TalentDraft draft = TalentDraft.parse("""
                {"schema":2,"text":{"name":"Advanced"},"rarity":"talent_editor:common"}
                """);

        assertEquals(TalentRelationMutation.Result.CHANGED, TalentRelationMutation.apply(
                draft, TalentTreeEditRequest.mutate(TARGET, TalentTreeEditRequest.Page.SYNTHESIS,
                        TalentTreeEditRequest.Mutation.ADD, ROOT)
        ));
        assertTrue(draft.synthesis().enabled());
        assertEquals(java.util.List.of(ROOT), draft.synthesis().consumeTalents());
        assertEquals(TalentRelationMutation.Result.SELF_REFERENCE, TalentRelationMutation.apply(
                draft, TalentTreeEditRequest.mutate(TARGET, TalentTreeEditRequest.Page.SYNTHESIS,
                        TalentTreeEditRequest.Mutation.ADD, TARGET)
        ));
    }
}
