package com.kidxoxo.talenteditor.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Optional;

public record PlayerTalentState(
        long definitionRevision,
        int drawTokens,
        boolean truncated,
        List<TalentView> talents,
        boolean synthesesTruncated,
        List<SynthesisView> syntheses,
        boolean countersTruncated,
        List<CounterProgressView> counters
) {
    public PlayerTalentState {
        talents = List.copyOf(talents);
        syntheses = List.copyOf(syntheses);
        counters = List.copyOf(counters);
    }

    public PlayerTalentState(long definitionRevision, int drawTokens, boolean truncated, List<TalentView> talents) {
        this(definitionRevision, drawTokens, truncated, talents, false, List.of(), false, List.of());
    }

    public PlayerTalentState(
            long definitionRevision, int drawTokens, boolean truncated, List<TalentView> talents,
            boolean synthesesTruncated, List<SynthesisView> syntheses
    ) {
        this(definitionRevision, drawTokens, truncated, talents, synthesesTruncated, syntheses, false, List.of());
    }

    public record TalentView(
            ResourceLocation id,
            StyledTextView name,
            ResourceLocation rarity,
            StyledTextView rarityName,
            int rarityColor,
            Optional<ResourceLocation> icon,
            boolean defined,
            boolean available,
            boolean showGeneratedEffects,
            List<StyledTextView> description,
            List<AttributeEffectView> attributeEffects
    ) {
        public TalentView {
            icon = icon == null ? Optional.empty() : icon;
            description = List.copyOf(description);
            attributeEffects = List.copyOf(attributeEffects);
        }
    }

    public record StyledTextView(
            String literal,
            String translation,
            Integer color,
            boolean bold,
            boolean italic
    ) {
        public StyledTextView {
            literal = literal == null ? "" : literal;
            translation = translation == null ? "" : translation;
        }

        public String fallbackText() {
            return literal.isBlank() ? translation : literal;
        }

        public MutableComponent component(int fallbackColor) {
            MutableComponent value = literal.isBlank()
                    ? Component.translatable(translation)
                    : Component.literal(literal);
            int effectiveColor = color == null ? fallbackColor : color;
            return value.withStyle(style -> style
                    .withColor(effectiveColor & 0xFFFFFF)
                    .withBold(bold)
                    .withItalic(italic));
        }
    }

    public record AttributeEffectView(
            ResourceLocation attribute,
            double amount,
            String operation,
            boolean showInDescription,
            String formula
    ) {
        public AttributeEffectView {
            formula = formula == null ? "" : formula;
        }
    }

    public record SynthesisView(
            TalentView target,
            boolean conditionMet,
            boolean ready,
            List<SynthesisCostView> costs
    ) {
        public SynthesisView {
            costs = List.copyOf(costs);
        }
    }

    public record SynthesisCostView(
            SynthesisCostType type,
            ResourceLocation id,
            long required,
            long current
    ) {
        public boolean satisfied() {
            return current >= required;
        }
    }

    public record CounterProgressView(
            ResourceLocation id,
            StyledTextView name,
            String event,
            long current,
            long maximum,
            boolean defined,
            boolean active
    ) {
    }

    public enum SynthesisCostType {
        TALENT,
        COUNTER,
        ITEM
    }
}
