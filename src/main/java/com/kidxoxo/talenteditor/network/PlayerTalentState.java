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
        List<CounterProgressView> counters,
        List<HudResourceView> hudResources,
        RenderView render
) {
    public PlayerTalentState {
        talents = List.copyOf(talents);
        syntheses = List.copyOf(syntheses);
        counters = List.copyOf(counters);
        hudResources = List.copyOf(hudResources);
        render = render == null ? RenderView.DEFAULT : render;
    }

    public PlayerTalentState(long definitionRevision, int drawTokens, boolean truncated, List<TalentView> talents) {
        this(definitionRevision, drawTokens, truncated, talents, false, List.of(), false, List.of(),
                List.of(), RenderView.DEFAULT);
    }

    public PlayerTalentState(
            long definitionRevision, int drawTokens, boolean truncated, List<TalentView> talents,
            boolean synthesesTruncated, List<SynthesisView> syntheses
    ) {
        this(definitionRevision, drawTokens, truncated, talents, synthesesTruncated, syntheses, false, List.of(),
                List.of(), RenderView.DEFAULT);
    }

    public PlayerTalentState(
            long definitionRevision, int drawTokens, boolean truncated, List<TalentView> talents,
            boolean synthesesTruncated, List<SynthesisView> syntheses,
            boolean countersTruncated, List<CounterProgressView> counters
    ) {
        this(definitionRevision, drawTokens, truncated, talents, synthesesTruncated, syntheses,
                countersTruncated, counters, List.of(), RenderView.DEFAULT);
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
            StyledTextView name,
            long required,
            long current
    ) {
        public SynthesisCostView(SynthesisCostType type, ResourceLocation id, long required, long current) {
            this(type, id, new StyledTextView(id.getPath(), "", null, false, false), required, current);
        }
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

    public record HudResourceView(ResourceLocation state, StyledTextView name, double value, double maximum,
                                  int color, int order, boolean showWhenFull) {
        public HudResourceView(ResourceLocation state, double value, double maximum,
                               int color, int order, boolean showWhenFull) {
            this(state, new StyledTextView(state.getPath(), "", null, false, false),
                    value, maximum, color, order, showWhenFull);
        }
        public HudResourceView {
            name = name == null ? new StyledTextView(state.getPath(), "", null, false, false) : name;
            if (!Double.isFinite(value)) value = 0.0D;
            if (!Double.isFinite(maximum) || maximum <= 0.0D) maximum = 1.0D;
            color &= 0xFFFFFF;
        }
    }

    public record RenderView(double alpha, double scale, boolean hideNameplate) {
        public static final RenderView DEFAULT = new RenderView(1.0D, 1.0D, false);
        public RenderView {
            if (!Double.isFinite(alpha)) alpha = 1.0D;
            if (!Double.isFinite(scale)) scale = 1.0D;
            alpha = Math.max(0.05D, Math.min(1.0D, alpha));
            scale = Math.max(0.1D, Math.min(4.0D, scale));
        }
    }

    public enum SynthesisCostType {
        TALENT,
        COUNTER,
        ITEM
    }
}
