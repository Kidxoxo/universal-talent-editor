package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.AttributeModifierAction;
import com.kidxoxo.talenteditor.domain.DynamicAttributeModifierAction;
import com.kidxoxo.talenteditor.domain.DynamicAttributeSource;
import com.kidxoxo.talenteditor.domain.RarityDefinition;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.domain.LocalizedText;
import com.kidxoxo.talenteditor.domain.HudResourceAction;
import com.kidxoxo.talenteditor.domain.PlayerRenderAction;
import com.kidxoxo.talenteditor.domain.ConditionalAction;
import com.kidxoxo.talenteditor.domain.CounterRuleDefinition;
import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;
import com.kidxoxo.talenteditor.runtime.ConditionEvaluator;
import com.kidxoxo.talenteditor.runtime.ActionConditionRuntime;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;

public final class PlayerTalentStateFactory {
    public static final int MAX_TALENTS = 2_048;
    public static final int MAX_DESCRIPTIONS = 64;
    public static final int MAX_EFFECTS = 256;
    public static final int MAX_SYNTHESES = 512;
    public static final int MAX_COUNTERS = 4_096;
    public static final int MAX_SYNTHESIS_COSTS = 256;
    public static final int MAX_TEXT_CHARACTERS = 1_024;
    private static final ResourceLocation UNKNOWN_RARITY =
            new ResourceLocation(TalentEditorMod.MOD_ID, "unknown");

    private PlayerTalentStateFactory() {
    }

    public static PlayerTalentState build(
            RuntimeSnapshot snapshot,
            Collection<ResourceLocation> ownedTalents
    ) {
        return build(snapshot, ownedTalents, 0);
    }

    public static PlayerTalentState build(
            RuntimeSnapshot snapshot,
            Collection<ResourceLocation> ownedTalents,
            int drawTokens
    ) {
        List<ResourceLocation> owned = ownedTalents.stream()
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        boolean truncated = owned.size() > MAX_TALENTS;
        ArrayList<PlayerTalentState.TalentView> views = new ArrayList<>();
        owned.stream().limit(MAX_TALENTS).forEach(id -> views.add(view(snapshot, id)));
        return new PlayerTalentState(snapshot.revision(), Math.max(0, drawTokens), truncated, views);
    }

    public static PlayerTalentState buildForPlayer(RuntimeSnapshot snapshot, ServerPlayer player) {
        Set<ResourceLocation> owned = PlayerTalents.owned(player);
        Set<ResourceLocation> effectSources = PlayerTalents.activeSources(player);
        Set<ResourceLocation> categorySources = PlayerTalents.categorySources(player);
        PlayerTalentState base = build(snapshot, owned, PlayerTalents.drawTokens(player));
        ConditionEvaluator.Context context = ConditionEvaluator.context(player);
        List<TalentDefinition> targets = snapshot.talents().values().stream()
                .filter(talent -> talent.synthesis().enabled())
                .filter(TalentDefinition::enabled)
                .filter(TalentDefinition::available)
                .filter(talent -> !owned.contains(talent.id()))
                .sorted(Comparator.comparing(talent -> talent.id().toString()))
                .toList();
        boolean truncated = targets.size() > MAX_SYNTHESES;
        List<PlayerTalentState.SynthesisView> syntheses = targets.stream()
                .limit(MAX_SYNTHESES)
                .map(target -> synthesisView(snapshot, target, context))
                .toList();
        Map<ResourceLocation, Long> storedCounters = PlayerTalents.counters(player);
        Set<ResourceLocation> history = PlayerTalents.talentHistory(player);
        LinkedHashSet<ResourceLocation> counterIds = new LinkedHashSet<>(snapshot.counterBookIds().stream()
                .filter(id -> shouldDisplayCounter(snapshot, id, storedCounters, history, categorySources))
                .sorted(Comparator.comparing(ResourceLocation::toString)).toList());
        storedCounters.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString))
                .filter(id -> !snapshot.counters().containsKey(id))
                .filter(id -> storedCounters.getOrDefault(id, 0L) != 0L)
                .forEach(counterIds::add);
        boolean countersTruncated = counterIds.size() > MAX_COUNTERS;
        List<PlayerTalentState.CounterProgressView> counters = counterIds.stream()
                .limit(MAX_COUNTERS)
                .map(id -> counterView(snapshot, id, counterValue(
                        snapshot, id, storedCounters.getOrDefault(id, 0L), categorySources
                )))
                .toList();
        List<PlayerTalentState.HudResourceView> hudResources = hudResources(snapshot, player, effectSources);
        PlayerTalentState.RenderView render = renderView(snapshot, player, effectSources);
        return new PlayerTalentState(
                base.definitionRevision(), base.drawTokens(), base.truncated(), base.talents(), truncated, syntheses,
                countersTruncated, counters, hudResources, render
        );
    }

    public static PlayerTalentState.RenderView renderView(
            RuntimeSnapshot snapshot, ServerPlayer player, Set<ResourceLocation> sources
    ) {
        double alpha = 1.0D;
        double scale = 1.0D;
        boolean hideNameplate = false;
        for (ResourceLocation id : sources) {
            TalentDefinition talent = snapshot.talents().get(id);
            if (talent == null || !talent.enabled() || !talent.available()) continue;
            for (var configured : talent.activeEffects()) {
                if (!ActionConditionRuntime.active(configured, player, id)) continue;
                var action = ConditionalAction.unwrap(configured);
                if (!(action instanceof PlayerRenderAction render)) continue;
                alpha *= com.kidxoxo.talenteditor.runtime.NumericExpressionRuntime.evaluate(
                        render.alpha(), player, player, 0.0D, 0.0D, id
                );
                scale *= com.kidxoxo.talenteditor.runtime.NumericExpressionRuntime.evaluate(
                        render.scale(), player, player, 0.0D, 0.0D, id
                );
                hideNameplate |= render.hideNameplate();
            }
        }
        return new PlayerTalentState.RenderView(alpha, scale, hideNameplate);
    }

    public static boolean hasRenderAction(RuntimeSnapshot snapshot, Set<ResourceLocation> sources) {
        for (ResourceLocation id : sources) {
            TalentDefinition talent = snapshot.talents().get(id);
            if (talent != null && talent.enabled() && talent.available()
                    && talent.activeEffects().stream().map(ConditionalAction::unwrap)
                    .anyMatch(PlayerRenderAction.class::isInstance)) return true;
        }
        return false;
    }

    public static List<PlayerTalentState.HudResourceView> hudResources(
            RuntimeSnapshot snapshot, ServerPlayer player, Set<ResourceLocation> sources
    ) {
        ArrayList<PlayerTalentState.HudResourceView> values = new ArrayList<>();
        for (ResourceLocation id : sources) {
            TalentDefinition talent = snapshot.talents().get(id);
            if (talent == null || !talent.enabled() || !talent.available()) continue;
            for (var configured : talent.activeEffects()) {
                if (!ActionConditionRuntime.active(configured, player, id)) continue;
                var action = ConditionalAction.unwrap(configured);
                if (!(action instanceof HudResourceAction hud)) continue;
                double maximum = com.kidxoxo.talenteditor.runtime.NumericExpressionRuntime.evaluate(
                        hud.maximum(), player, player, 0.0D, 0.0D, id
                );
                values.add(new PlayerTalentState.HudResourceView(
                        hud.state(), styled(talent.name()), PlayerTalents.numericState(player, hud.state()), maximum,
                        hud.color(), hud.order(), hud.showWhenFull()
                ));
            }
        }
        java.util.LinkedHashMap<ResourceLocation, PlayerTalentState.HudResourceView> unique = new java.util.LinkedHashMap<>();
        values.stream().sorted(Comparator.comparingInt(PlayerTalentState.HudResourceView::order)
                        .thenComparing(value -> value.state().toString()))
                .forEach(value -> unique.putIfAbsent(value.state(), value));
        return unique.values().stream().limit(32).toList();
    }

    public static PlayerTalentState.CounterProgressView counterView(
            RuntimeSnapshot snapshot, ResourceLocation id, long current
    ) {
        CounterRuleDefinition counter = snapshot.counters().get(id);
        if (counter == null) {
            return new PlayerTalentState.CounterProgressView(
                    id, plain("未知计数器"), "", Math.max(0L, current),
                    com.kidxoxo.talenteditor.player.PlayerTalentData.MAX_COUNTER_VALUE, false, false
            );
        }
        return new PlayerTalentState.CounterProgressView(
                id, styled(counter.name()), counter.event().serializedName(), Math.max(0L, current),
                counter.maximum(), true, counter.enabled() && counter.available()
        );
    }

    private static boolean shouldDisplayCounter(
            RuntimeSnapshot snapshot,
            ResourceLocation id,
            Map<ResourceLocation, Long> storedCounters,
            Set<ResourceLocation> history,
            Set<ResourceLocation> categorySources
    ) {
        CounterRuleDefinition counter = snapshot.counters().get(id);
        if (counter == null) return storedCounters.getOrDefault(id, 0L) != 0L;
        if (counter.displayCategory().isPresent()) {
            ResourceLocation category = counter.displayCategory().orElseThrow();
            return history.stream()
                    .map(snapshot.talents()::get)
                    .filter(java.util.Objects::nonNull)
                    .anyMatch(talent -> talent.categories().contains(category));
        }
        if (!counter.requiredTalents().isEmpty()) {
            return counter.requiredTalents().stream().anyMatch(history::contains);
        }
        return true;
    }

    private static long counterValue(
            RuntimeSnapshot snapshot,
            ResourceLocation id,
            long stored,
            Set<ResourceLocation> categorySources
    ) {
        CounterRuleDefinition counter = snapshot.counters().get(id);
        return counter != null && counter.displayCategory().isPresent()
                ? categoryCount(snapshot, categorySources, counter.displayCategory().orElseThrow())
                : stored;
    }

    private static long categoryCount(
            RuntimeSnapshot snapshot,
            Set<ResourceLocation> sources,
            ResourceLocation category
    ) {
        return sources.stream()
                .map(snapshot.talents()::get)
                .filter(java.util.Objects::nonNull)
                .filter(talent -> talent.enabled() && talent.available() && talent.categories().contains(category))
                .count();
    }

    public static PlayerTalentState.TalentView view(RuntimeSnapshot snapshot, ResourceLocation id) {
        TalentDefinition talent = snapshot.talents().get(id);
        if (talent == null) {
            return new PlayerTalentState.TalentView(
                    id, plain("未知天赋"), UNKNOWN_RARITY, plain("未知稀有度"), 0xFF808080,
                    Optional.empty(), false, false, false, List.of(), List.of()
            );
        }
        RarityDefinition rarity = snapshot.rarities().get(talent.rarity());
        PlayerTalentState.StyledTextView rarityName = rarity == null
                ? plain("未知稀有度") : styled(rarity.name());
        int rarityColor = rarity == null ? 0xFF808080 : rarity.color();
        List<PlayerTalentState.StyledTextView> description = talent.description().stream()
                .limit(MAX_DESCRIPTIONS)
                .map(PlayerTalentStateFactory::styled)
                .toList();
        ArrayList<PlayerTalentState.AttributeEffectView> effects = new ArrayList<>();
        for (var configured : talent.activeEffects()) {
            var action = ConditionalAction.unwrap(configured);
            if (effects.size() >= MAX_EFFECTS) break;
            if (action instanceof AttributeModifierAction attribute) {
                effects.add(new PlayerTalentState.AttributeEffectView(
                        attribute.attribute(), attribute.amount(),
                        attribute.operation().name().toLowerCase(Locale.ROOT),
                        attribute.showInDescription(), ""
                ));
            } else if (action instanceof DynamicAttributeModifierAction dynamic) {
                effects.add(new PlayerTalentState.AttributeEffectView(
                        dynamic.attribute(), dynamic.base(),
                        dynamic.operation().name().toLowerCase(Locale.ROOT),
                        dynamic.showInDescription(), dynamicFormula(dynamic)
                ));
            }
        }
        return new PlayerTalentState.TalentView(
                talent.id(), styled(talent.name()), talent.rarity(), rarityName, rarityColor,
                talent.icon(), true, talent.enabled() && talent.available(),
                talent.showGeneratedEffects(), description, effects
        );
    }

    public static String clipText(String value) {
        if (value.length() <= MAX_TEXT_CHARACTERS) return value;
        return value.substring(0, MAX_TEXT_CHARACTERS - 1) + "…";
    }

    private static String dynamicFormula(DynamicAttributeModifierAction action) {
        String source = action.source() == DynamicAttributeSource.COUNTER
                ? "counter[" + action.counter().orElseThrow() + "]"
                : action.source().serializedName();
        return "clamp(" + compact(action.base()) + " + " + source + " × " + compact(action.scale())
                + ", " + compact(action.minimum()) + ", " + compact(action.maximum()) + ")";
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.8f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }

    private static PlayerTalentState.SynthesisView synthesisView(
            RuntimeSnapshot snapshot,
            TalentDefinition target,
            ConditionEvaluator.Context context
    ) {
        ArrayList<PlayerTalentState.SynthesisCostView> costs = new ArrayList<>();
        target.synthesis().consumeTalents().stream()
                .limit(MAX_SYNTHESIS_COSTS)
                .forEach(id -> costs.add(new PlayerTalentState.SynthesisCostView(
                        PlayerTalentState.SynthesisCostType.TALENT, id, synthesisCostName(snapshot,
                                PlayerTalentState.SynthesisCostType.TALENT, id), 1L,
                        context.talents().contains(id) ? 1L : 0L
                )));
        int remaining = MAX_SYNTHESIS_COSTS - costs.size();
        target.synthesis().consumeCounters().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .limit(remaining)
                .forEach(entry -> costs.add(new PlayerTalentState.SynthesisCostView(
                        PlayerTalentState.SynthesisCostType.COUNTER, entry.getKey(), synthesisCostName(snapshot,
                                PlayerTalentState.SynthesisCostType.COUNTER, entry.getKey()), entry.getValue(),
                        context.counters().getOrDefault(entry.getKey(), 0L)
                )));
        remaining = MAX_SYNTHESIS_COSTS - costs.size();
        target.synthesis().consumeItems().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .limit(remaining)
                .forEach(entry -> costs.add(new PlayerTalentState.SynthesisCostView(
                        PlayerTalentState.SynthesisCostType.ITEM, entry.getKey(), synthesisCostName(snapshot,
                                PlayerTalentState.SynthesisCostType.ITEM, entry.getKey()), entry.getValue(),
                        context.itemCounts().getOrDefault(entry.getKey(), 0)
                )));
        boolean conditionMet = ConditionEvaluator.test(target.acquireCondition(), context);
        boolean ready = conditionMet && costs.stream().allMatch(PlayerTalentState.SynthesisCostView::satisfied)
                && costs.size() == target.synthesis().consumeTalents().size()
                + target.synthesis().consumeCounters().size() + target.synthesis().consumeItems().size();
        return new PlayerTalentState.SynthesisView(view(snapshot, target.id()), conditionMet, ready, costs);
    }

    private static PlayerTalentState.StyledTextView synthesisCostName(
            RuntimeSnapshot snapshot, PlayerTalentState.SynthesisCostType type, ResourceLocation id
    ) {
        if (type == PlayerTalentState.SynthesisCostType.TALENT) {
            TalentDefinition definition = snapshot.talents().get(id);
            return definition == null ? plain("未知天赋") : styled(definition.name());
        }
        if (type == PlayerTalentState.SynthesisCostType.COUNTER) {
            CounterRuleDefinition definition = snapshot.counters().get(id);
            return definition == null ? plain("未知计数器") : styled(definition.name());
        }
        var configured = snapshot.configuredItems().get(id);
        if (configured != null) return styled(configured.name());
        var item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? plain("未知物品")
                : new PlayerTalentState.StyledTextView("", item.getDescriptionId(), null, false, false);
    }

    private static PlayerTalentState.StyledTextView styled(LocalizedText text) {
        return new PlayerTalentState.StyledTextView(
                clipText(text.literal()), clipText(text.translation()), text.color(), text.bold(), text.italic()
        );
    }

    private static PlayerTalentState.StyledTextView plain(String text) {
        return new PlayerTalentState.StyledTextView(clipText(text), "", null, false, false);
    }
}
