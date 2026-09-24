package com.kidxoxo.talenteditor.client;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;

final class TalentRelationMutation {
    private TalentRelationMutation() {
    }

    static Result apply(TalentDraft draft, TalentTreeEditRequest request) {
        if (request.mutation() == TalentTreeEditRequest.Mutation.NONE) return Result.UNCHANGED;
        ResourceLocation related = request.relatedTalent().orElse(null);
        if (related == null) return Result.UNCHANGED;
        if (related.equals(request.target())) return Result.SELF_REFERENCE;
        return switch (request.page()) {
            case TALENT -> Result.UNCHANGED;
            case ACQUISITION -> acquisition(draft, request.mutation(), related);
            case SYNTHESIS -> synthesis(draft, request.mutation(), related);
        };
    }

    private static Result acquisition(
            TalentDraft draft, TalentTreeEditRequest.Mutation mutation, ResourceLocation related
    ) {
        TalentDraft.RequirementsDraft requirements = draft.requirements();
        if (!requirements.editable()) return Result.ADVANCED_READ_ONLY;
        ArrayList<TalentDraft.RequirementRow> rows = new ArrayList<>(requirements.rows());
        boolean changed;
        if (mutation == TalentTreeEditRequest.Mutation.ADD) {
            boolean present = rows.stream().anyMatch(row -> row.type() == TalentDraft.RequirementType.TALENT
                    && row.target().equals(related));
            changed = !present;
            if (changed) rows.add(new TalentDraft.RequirementRow(
                    TalentDraft.RequirementType.TALENT, related, 1L
            ));
        } else {
            changed = rows.removeIf(row -> row.type() == TalentDraft.RequirementType.TALENT
                    && row.target().equals(related));
        }
        if (!changed) return Result.UNCHANGED;
        draft.updateRequirements(requirements.mode(), rows);
        return Result.CHANGED;
    }

    private static Result synthesis(
            TalentDraft draft, TalentTreeEditRequest.Mutation mutation, ResourceLocation related
    ) {
        TalentDraft.SynthesisDraft synthesis = draft.synthesis();
        LinkedHashSet<ResourceLocation> talents = new LinkedHashSet<>(synthesis.consumeTalents());
        boolean changed = mutation == TalentTreeEditRequest.Mutation.ADD
                ? talents.add(related) : talents.remove(related);
        if (!changed) return Result.UNCHANGED;
        draft.updateSynthesis(
                mutation == TalentTreeEditRequest.Mutation.ADD || synthesis.enabled(),
                new ArrayList<>(talents), synthesis.consumeCounters(), synthesis.consumeItems()
        );
        return Result.CHANGED;
    }

    enum Result {
        CHANGED,
        UNCHANGED,
        ADVANCED_READ_ONLY,
        SELF_REFERENCE
    }
}
