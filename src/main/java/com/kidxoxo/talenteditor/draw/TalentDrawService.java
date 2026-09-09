package com.kidxoxo.talenteditor.draw;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.RarityDefinition;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.domain.TalentPoolDefinition;
import com.kidxoxo.talenteditor.domain.DuplicatePolicy;
import com.kidxoxo.talenteditor.domain.DrawRefreshBonusAction;
import com.kidxoxo.talenteditor.domain.QueueDrawConstraintAction;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;
import com.kidxoxo.talenteditor.runtime.ConditionEvaluator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

public final class TalentDrawService {
    private final Map<UUID, DrawSession> sessions = new LinkedHashMap<>();

    public synchronized DrawResult open(ServerPlayer player, ResourceLocation poolId) {
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        TalentPoolDefinition pool = snapshot.pools().get(poolId);
        DrawResult validation = validateStart(player, pool);
        if (validation != null) return validation;

        DrawSession existing = sessions.get(player.getUUID());
        Set<ResourceLocation> owned = PlayerTalents.owned(player);
        ConditionEvaluator.Context conditionContext = ConditionEvaluator.context(player);
        if (existing != null
                && existing.poolId().equals(poolId)
                && existing.definitionRevision() == snapshot.revision()
                && (!pool.excludeOwned() || existing.offers().stream().noneMatch(owned::contains))) {
            return DrawResult.session(DrawResult.Status.READY, existing);
        }
        double luck = luckValue(player, pool);
        boolean pityActive = pityActive(player, pool);
        List<ResourceLocation> offers = generateOffers(
                snapshot, pool, owned, conditionContext, luck, pityActive,
                new Random(player.getRandom().nextLong())
        );
        if (offers.isEmpty()) return DrawResult.failed(DrawResult.Status.NO_ELIGIBLE_TALENT);
        DrawSession session = new DrawSession(
                UUID.randomUUID(), player.getUUID(), poolId, snapshot.revision(),
                offers, Math.min(1_000, pool.refreshes() + refreshBonus(snapshot, owned, poolId))
        );
        sessions.put(player.getUUID(), session);
        return DrawResult.session(DrawResult.Status.READY, session);
    }

    public synchronized DrawResult refresh(ServerPlayer player, UUID sessionId) {
        DrawSession current = validSession(player, sessionId);
        if (current == null) return DrawResult.failed(DrawResult.Status.SESSION_EXPIRED);
        boolean freeRefresh = current.refreshesRemaining() > 0;
        if (!freeRefresh && PlayerTalents.drawTokens(player) < 2) {
            return DrawResult.failed(DrawResult.Status.PAID_REFRESH_REQUIRES_TWO_TOKENS);
        }
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        TalentPoolDefinition pool = snapshot.pools().get(current.poolId());
        DrawResult validation = validateStart(player, pool);
        if (validation != null) return validation;
        Set<ResourceLocation> owned = PlayerTalents.owned(player);
        ConditionEvaluator.Context context = ConditionEvaluator.context(player);
        double luck = luckValue(player, pool);
        boolean pity = pityActive(player, pool);
        Random random = new Random(player.getRandom().nextLong());
        var constraint = PlayerTalents.nextDrawConstraint(player, pool.id());
        List<ResourceLocation> offers = constraint
                .map(value -> generateOffersWithConstraint(
                        snapshot, pool, owned, context, luck, pity, value, random
                ))
                .orElseGet(() -> generateOffers(snapshot, pool, owned, context, luck, pity, random));
        if (offers.isEmpty()) return DrawResult.failed(DrawResult.Status.NO_ELIGIBLE_TALENT);
        if (!freeRefresh && !PlayerTalents.consumeDrawToken(player)) {
            return DrawResult.failed(DrawResult.Status.PAID_REFRESH_REQUIRES_TWO_TOKENS);
        }
        if (constraint.isPresent()) PlayerTalents.consumeNextDrawConstraint(player, pool.id());
        DrawSession refreshed = new DrawSession(
                current.id(), current.playerId(), current.poolId(), current.definitionRevision(),
                offers, freeRefresh ? current.refreshesRemaining() - 1 : 0
        );
        sessions.put(player.getUUID(), refreshed);
        return DrawResult.session(DrawResult.Status.REFRESHED, refreshed);
    }

    public synchronized DrawResult select(ServerPlayer player, UUID sessionId, ResourceLocation talentId) {
        DrawSession current = validSession(player, sessionId);
        if (current == null) return DrawResult.failed(DrawResult.Status.SESSION_EXPIRED);
        if (!current.offers().contains(talentId)) return DrawResult.failed(DrawResult.Status.OFFER_INVALID);
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        TalentPoolDefinition pool = snapshot.pools().get(current.poolId());
        DrawResult validation = validateStart(player, pool);
        if (validation != null) return validation;
        TalentDefinition talent = snapshot.talents().get(talentId);
        if (talent == null || !eligibleForPool(
                talent, snapshot, pool, PlayerTalents.owned(player), ConditionEvaluator.context(player)
        )) {
            return DrawResult.failed(DrawResult.Status.OFFER_INVALID);
        }
        if (PlayerTalents.owned(player).contains(talentId)) {
            if (pool.duplicatePolicy() == DuplicatePolicy.EXCLUDE) {
                return DrawResult.failed(DrawResult.Status.OFFER_INVALID);
            }
            if (pool.duplicatePolicy() == DuplicatePolicy.REFUND_TOKEN) {
                sessions.remove(player.getUUID());
                return DrawResult.resolved(DrawResult.Status.DUPLICATE_REFUNDED, talentId);
            }
            ResourceLocation counter = pool.duplicateCounter().orElseThrow();
            if (!PlayerTalents.exchangeDrawTokenForCounter(player, counter, pool.duplicateCounterAmount())) {
                return DrawResult.resolved(DrawResult.Status.DUPLICATE_REWARD_FULL, talentId);
            }
            sessions.remove(player.getUUID());
            recordPityOutcome(player, snapshot, pool, talent);
            return DrawResult.resolved(DrawResult.Status.DUPLICATE_COUNTER_GRANTED, talentId);
        }
        if (!PlayerTalents.acquireWithDrawToken(player, talentId)) {
            return DrawResult.failed(DrawResult.Status.NO_DRAW_TOKEN);
        }
        sessions.remove(player.getUUID());
        recordPityOutcome(player, snapshot, pool, talent);
        TalentEditorMod.PRESENTATIONS.acquired(player, talent, false);
        return DrawResult.acquired(talentId);
    }

    public synchronized void removeSession(ServerPlayer player) {
        sessions.remove(player.getUUID());
    }

    public static List<ResourceLocation> generateOffers(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            Set<ResourceLocation> owned,
            RandomGenerator random
    ) {
        return generateOffers(snapshot, pool, owned, ConditionEvaluator.Context.empty(owned), random);
    }

    public static List<ResourceLocation> generateOffers(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            Set<ResourceLocation> owned,
            ConditionEvaluator.Context conditionContext,
            RandomGenerator random
    ) {
        return generateOffers(snapshot, pool, owned, conditionContext, 0.0D, false, random);
    }

    public static List<ResourceLocation> generateOffers(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            Set<ResourceLocation> owned,
            ConditionEvaluator.Context conditionContext,
            double luck,
            boolean pityActive,
            RandomGenerator random
    ) {
        ArrayList<TalentDefinition> candidates = eligibleCandidates(
                snapshot, pool, owned, conditionContext
        );
        return selectCandidates(snapshot, pool, candidates, pool.choices(), luck, pityActive, random).stream()
                .map(TalentDefinition::id)
                .toList();
    }

    public static List<ResourceLocation> generateOffersWithConstraint(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            Set<ResourceLocation> owned,
            ConditionEvaluator.Context conditionContext,
            double luck,
            boolean pityActive,
            QueueDrawConstraintAction constraint,
            RandomGenerator random
    ) {
        if (!constraint.appliesTo(pool.id())) return List.of();
        ArrayList<TalentDefinition> candidates = eligibleCandidates(
                snapshot, pool, owned, conditionContext
        );
        ArrayList<TalentDefinition> matches = candidates.stream()
                .filter(constraint::matches)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (matches.isEmpty()) return List.of();

        int requiredCount = Math.min(pool.choices(), constraint.guaranteed());
        ArrayList<TalentDefinition> selected = new ArrayList<>(selectCandidates(
                snapshot, pool, matches, requiredCount, luck, pityActive, random
        ));
        boolean pitySatisfied = pityActive && selected.stream().anyMatch(
                talent -> satisfiesPity(snapshot, pool, talent)
        );
        candidates.removeAll(selected);
        if (selected.size() < pool.choices()) {
            selected.addAll(selectCandidates(
                    snapshot, pool, candidates, pool.choices() - selected.size(), luck,
                    pityActive && !pitySatisfied, random
            ));
        }
        return selected.stream().map(TalentDefinition::id).toList();
    }

    private static ArrayList<TalentDefinition> eligibleCandidates(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            Set<ResourceLocation> owned,
            ConditionEvaluator.Context conditionContext
    ) {
        return snapshot.talents().values().stream()
                .filter(talent -> eligibleForPool(talent, snapshot, pool, owned, conditionContext))
                .sorted(Comparator.comparing(talent -> talent.id().toString()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static List<TalentDefinition> selectCandidates(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            List<TalentDefinition> source,
            int limit,
            double luck,
            boolean pityActive,
            RandomGenerator random
    ) {
        ArrayList<TalentDefinition> candidates = new ArrayList<>(source);
        ArrayList<TalentDefinition> result = new ArrayList<>();
        while (!candidates.isEmpty() && result.size() < limit) {
            LinkedHashMap<ResourceLocation, List<TalentDefinition>> byRarity = new LinkedHashMap<>();
            for (TalentDefinition talent : candidates) {
                byRarity.computeIfAbsent(talent.rarity(), ignored -> new ArrayList<>()).add(talent);
            }
            ResourceLocation selectedRarity = null;
            if (result.isEmpty() && pityActive && pool.pityRarity().isPresent()) {
                int minimumOrder = snapshot.rarities().get(pool.pityRarity().orElseThrow()).order();
                LinkedHashMap<ResourceLocation, List<TalentDefinition>> pityCandidates = new LinkedHashMap<>();
                byRarity.forEach((rarityId, values) -> {
                    RarityDefinition rarity = snapshot.rarities().get(rarityId);
                    if (rarity != null && rarity.order() >= minimumOrder) pityCandidates.put(rarityId, values);
                });
                selectedRarity = weightedRarity(snapshot, pool, pityCandidates, luck, random);
            }
            if (selectedRarity == null) selectedRarity = weightedRarity(snapshot, pool, byRarity, luck, random);
            if (selectedRarity == null) break;
            TalentDefinition selected = weightedTalent(byRarity.get(selectedRarity), random);
            if (selected == null) {
                ResourceLocation emptyRarity = selectedRarity;
                candidates.removeIf(talent -> talent.rarity().equals(emptyRarity));
                continue;
            }
            result.add(selected);
            candidates.remove(selected);
        }
        return List.copyOf(result);
    }

    private static boolean satisfiesPity(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            TalentDefinition talent
    ) {
        if (pool.pityRarity().isEmpty()) return false;
        RarityDefinition threshold = snapshot.rarities().get(pool.pityRarity().orElseThrow());
        RarityDefinition rarity = snapshot.rarities().get(talent.rarity());
        return threshold != null && rarity != null && rarity.order() >= threshold.order();
    }

    public static double luckValue(ServerPlayer player, TalentPoolDefinition pool) {
        if (pool.luckAttribute().isEmpty()) return 0.0D;
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(pool.luckAttribute().orElseThrow());
        if (attribute == null || player.getAttribute(attribute) == null) return 0.0D;
        return clampLuck(player.getAttribute(attribute).getValue());
    }

    public static int refreshBonus(
            RuntimeSnapshot snapshot,
            Set<ResourceLocation> owned,
            ResourceLocation poolId
    ) {
        long total = 0L;
        for (ResourceLocation talentId : owned) {
            TalentDefinition talent = snapshot.talents().get(talentId);
            if (talent == null || !talent.enabled() || !talent.available()) continue;
            for (var action : talent.activeEffects()) {
                if (action instanceof DrawRefreshBonusAction bonus && bonus.appliesTo(poolId)) {
                    total += bonus.amount();
                    if (total >= 1_000L) return 1_000;
                }
            }
        }
        return (int) total;
    }

    public static Map<ResourceLocation, Double> rarityProbabilities(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            double luck
    ) {
        return rarityProbabilities(snapshot, pool, luck, null);
    }

    public static Map<ResourceLocation, Double> rarityProbabilitiesForPlayer(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            ServerPlayer player
    ) {
        Set<ResourceLocation> owned = PlayerTalents.owned(player);
        ConditionEvaluator.Context context = ConditionEvaluator.context(player);
        Set<ResourceLocation> eligibleRarities = snapshot.talents().values().stream()
                .filter(talent -> eligibleForPool(talent, snapshot, pool, owned, context))
                .map(TalentDefinition::rarity)
                .collect(java.util.stream.Collectors.toSet());
        return rarityProbabilities(snapshot, pool, luckValue(player, pool), eligibleRarities);
    }

    private static Map<ResourceLocation, Double> rarityProbabilities(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            double luck,
            Set<ResourceLocation> eligibleRarities
    ) {
        LinkedHashMap<ResourceLocation, Double> weights = new LinkedHashMap<>();
        snapshot.rarities().values().stream()
                .filter(RarityDefinition::enabled)
                .filter(RarityDefinition::offerable)
                .filter(rarity -> eligibleRarities == null || eligibleRarities.contains(rarity.id()))
                .sorted(Comparator.comparingInt(RarityDefinition::order)
                        .thenComparing(rarity -> rarity.id().toString()))
                .forEach(rarity -> {
                    double weight = effectiveRarityWeight(pool, rarity, luck);
                    if (weight > 0.0D) weights.put(rarity.id(), weight);
                });
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (!(total > 0.0D) || !Double.isFinite(total)) return Map.of();
        LinkedHashMap<ResourceLocation, Double> probabilities = new LinkedHashMap<>();
        weights.forEach((rarity, weight) -> probabilities.put(rarity, weight / total));
        return Map.copyOf(probabilities);
    }

    private DrawSession validSession(ServerPlayer player, UUID sessionId) {
        DrawSession current = sessions.get(player.getUUID());
        if (current == null || !current.id().equals(sessionId)
                || current.definitionRevision() != TalentEditorMod.SNAPSHOTS.active().revision()) {
            sessions.remove(player.getUUID());
            return null;
        }
        return current;
    }

    private DrawResult validateStart(ServerPlayer player, TalentPoolDefinition pool) {
        if (pool == null) return DrawResult.failed(DrawResult.Status.POOL_NOT_FOUND);
        if (!pool.enabled()) return DrawResult.failed(DrawResult.Status.POOL_DISABLED);
        if (PlayerTalents.drawTokens(player) <= 0) return DrawResult.failed(DrawResult.Status.NO_DRAW_TOKEN);
        if (PlayerTalents.owned(player).size() >= pool.maxOwned()) {
            return DrawResult.failed(DrawResult.Status.OWNED_LIMIT_REACHED);
        }
        return null;
    }

    private static boolean eligibleForPool(
            TalentDefinition talent,
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            Set<ResourceLocation> owned,
            ConditionEvaluator.Context conditionContext
    ) {
        if (!talent.enabled() || !talent.available() || !talent.offer().enabled()
                || talent.offer().weight() <= 0.0D) return false;
        if (!talent.offer().pools().isEmpty() && !talent.offer().pools().contains(pool.id())) return false;
        if (pool.duplicatePolicy() == DuplicatePolicy.EXCLUDE && owned.contains(talent.id())) return false;
        if (!ConditionEvaluator.test(talent.acquireCondition(), conditionContext)) return false;
        RarityDefinition rarity = snapshot.rarities().get(talent.rarity());
        return rarity != null && rarity.enabled() && rarity.offerable() && pool.rarityWeight(rarity) > 0.0D;
    }

    private static ResourceLocation weightedRarity(
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            Map<ResourceLocation, List<TalentDefinition>> byRarity,
            double luck,
            RandomGenerator random
    ) {
        ArrayList<Map.Entry<ResourceLocation, Double>> weighted = new ArrayList<>();
        for (ResourceLocation rarityId : byRarity.keySet()) {
            RarityDefinition rarity = snapshot.rarities().get(rarityId);
            if (rarity == null) continue;
            double weight = effectiveRarityWeight(pool, rarity, luck);
            if (weight > 0.0D) weighted.add(Map.entry(rarityId, weight));
        }
        return weightedChoice(weighted, random);
    }

    public static double effectiveRarityWeight(
            TalentPoolDefinition pool,
            RarityDefinition rarity,
            double luck
    ) {
        double boundedLuck = clampLuck(luck);
        double multiplier = Math.max(0.0D, Math.min(1_000_000.0D,
                1.0D + boundedLuck * pool.luckFactor(rarity.id())));
        double result = pool.rarityWeight(rarity) * multiplier;
        return Double.isFinite(result) ? result : 0.0D;
    }

    private static double clampLuck(double luck) {
        if (!Double.isFinite(luck)) return 0.0D;
        return Math.max(-1_024.0D, Math.min(1_024.0D, luck));
    }

    private static boolean pityActive(ServerPlayer player, TalentPoolDefinition pool) {
        return pool.pityAfter() > 0 && pool.pityRarity().isPresent()
                && PlayerTalents.drawPity(player, pool.id()) >= pool.pityAfter() - 1;
    }

    private static void recordPityOutcome(
            ServerPlayer player,
            RuntimeSnapshot snapshot,
            TalentPoolDefinition pool,
            TalentDefinition selected
    ) {
        if (pool.pityAfter() <= 0 || pool.pityRarity().isEmpty()) return;
        RarityDefinition threshold = snapshot.rarities().get(pool.pityRarity().orElseThrow());
        RarityDefinition rarity = snapshot.rarities().get(selected.rarity());
        boolean satisfied = threshold != null && rarity != null && rarity.order() >= threshold.order();
        PlayerTalents.recordDrawPity(player, pool.id(), satisfied, pool.pityAfter());
    }

    private static TalentDefinition weightedTalent(List<TalentDefinition> talents, RandomGenerator random) {
        ArrayList<Map.Entry<TalentDefinition, Double>> weighted = new ArrayList<>();
        for (TalentDefinition talent : talents) {
            if (talent.offer().weight() > 0.0D) weighted.add(Map.entry(talent, talent.offer().weight()));
        }
        return weightedChoice(weighted, random);
    }

    private static <T> T weightedChoice(List<Map.Entry<T, Double>> values, RandomGenerator random) {
        double total = values.stream().mapToDouble(Map.Entry::getValue).sum();
        if (!(total > 0.0D) || !Double.isFinite(total)) return null;
        double cursor = random.nextDouble(total);
        for (Map.Entry<T, Double> value : values) {
            cursor -= value.getValue();
            if (cursor < 0.0D) return value.getKey();
        }
        return values.isEmpty() ? null : values.get(values.size() - 1).getKey();
    }
}
