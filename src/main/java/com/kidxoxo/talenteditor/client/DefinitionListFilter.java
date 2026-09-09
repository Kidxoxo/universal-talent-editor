package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DefinitionListFilter {
    private DefinitionListFilter() {
    }

    enum State {
        ALL("all"),
        ACTIVE("active"),
        DISABLED("disabled"),
        UNAVAILABLE("unavailable");

        private final String translationSuffix;

        State(String translationSuffix) {
            this.translationSuffix = translationSuffix;
        }

        String translationSuffix() {
            return translationSuffix;
        }

        State next() {
            State[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        State next(boolean supportsUnavailable) {
            State next = next();
            return !supportsUnavailable && next == UNAVAILABLE ? ALL : next;
        }
    }

    static List<EditorBootstrap.TalentSummary> talents(
            EditorBootstrap bootstrap, String query, State state, String source
    ) {
        return filter(
                bootstrap.talents(), query, state, source,
                talent -> join(
                        talent.id().toString(), talent.name(), talent.rarity().toString(),
                        ids(talent.categories()), talent.source()
                ),
                EditorBootstrap.TalentSummary::enabled,
                EditorBootstrap.TalentSummary::available,
                EditorBootstrap.TalentSummary::source
        );
    }

    static List<EditorBootstrap.RaritySummary> rarities(
            EditorBootstrap bootstrap, String query, State state, String source
    ) {
        return filter(
                bootstrap.rarities(), query, state, source,
                rarity -> join(rarity.id().toString(), rarity.name(), rarity.source()),
                EditorBootstrap.RaritySummary::enabled,
                ignored -> true,
                EditorBootstrap.RaritySummary::source
        );
    }

    static List<EditorBootstrap.PoolSummary> pools(
            EditorBootstrap bootstrap, String query, State state, String source
    ) {
        return filter(
                bootstrap.pools(), query, state, source,
                pool -> join(
                        pool.id().toString(), pool.name(), ids(pool.rarityWeights().keySet()), pool.source()
                ),
                EditorBootstrap.PoolSummary::enabled,
                ignored -> true,
                EditorBootstrap.PoolSummary::source
        );
    }

    static List<EditorBootstrap.CounterSummary> counters(
            EditorBootstrap bootstrap, String query, State state, String source
    ) {
        return filter(
                bootstrap.counters(), query, state, source,
                counter -> join(
                        counter.id().toString(), counter.name(), counter.event(), counter.visibility(),
                        ids(counter.categories()), counter.source()
                ),
                EditorBootstrap.CounterSummary::enabled,
                EditorBootstrap.CounterSummary::available,
                EditorBootstrap.CounterSummary::source
        );
    }

    static List<EditorBootstrap.LayoutSummary> layouts(
            EditorBootstrap bootstrap, String query, State state, String source
    ) {
        return filter(
                bootstrap.layouts(), query, state, source,
                layout -> join(
                        layout.id().toString(), layout.background().map(ResourceLocation::toString).orElse(""),
                        Integer.toString(layout.priority()), Integer.toString(layout.freeSlotCount()),
                        Integer.toString(layout.pageCount()), layout.source()
                ),
                EditorBootstrap.LayoutSummary::enabled,
                ignored -> true,
                EditorBootstrap.LayoutSummary::source
        );
    }

    static List<EditorBootstrap.ConfiguredItemSummary> configuredItems(
            EditorBootstrap bootstrap, String query, State state, String source
    ) {
        return filter(
                bootstrap.configuredItems(), query, state, source,
                item -> join(
                        item.id().toString(), item.name(), Integer.toString(item.customModelData()), item.source()
                ),
                EditorBootstrap.ConfiguredItemSummary::enabled,
                EditorBootstrap.ConfiguredItemSummary::available,
                EditorBootstrap.ConfiguredItemSummary::source
        );
    }

    static List<String> sources(EditorBootstrap bootstrap) {
        Set<String> sources = new LinkedHashSet<>();
        bootstrap.rarities().forEach(value -> addSource(sources, value.source()));
        bootstrap.talents().forEach(value -> addSource(sources, value.source()));
        bootstrap.pools().forEach(value -> addSource(sources, value.source()));
        bootstrap.counters().forEach(value -> addSource(sources, value.source()));
        bootstrap.layouts().forEach(value -> addSource(sources, value.source()));
        bootstrap.configuredItems().forEach(value -> addSource(sources, value.source()));
        return sources.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    static List<String> talentSources(EditorBootstrap bootstrap) {
        return sortedSources(bootstrap.talents().stream().map(EditorBootstrap.TalentSummary::source).toList());
    }

    static List<String> raritySources(EditorBootstrap bootstrap) {
        return sortedSources(bootstrap.rarities().stream().map(EditorBootstrap.RaritySummary::source).toList());
    }

    static List<String> poolSources(EditorBootstrap bootstrap) {
        return sortedSources(bootstrap.pools().stream().map(EditorBootstrap.PoolSummary::source).toList());
    }

    static List<String> counterSources(EditorBootstrap bootstrap) {
        return sortedSources(bootstrap.counters().stream().map(EditorBootstrap.CounterSummary::source).toList());
    }

    static List<String> layoutSources(EditorBootstrap bootstrap) {
        return sortedSources(bootstrap.layouts().stream().map(EditorBootstrap.LayoutSummary::source).toList());
    }

    static List<String> configuredItemSources(EditorBootstrap bootstrap) {
        return sortedSources(bootstrap.configuredItems().stream()
                .map(EditorBootstrap.ConfiguredItemSummary::source).toList());
    }

    static String nextSource(String current, List<String> sources) {
        if (sources.isEmpty()) return "";
        if (current == null || current.isBlank()) return sources.get(0);
        int index = sources.indexOf(current);
        return index < 0 || index + 1 >= sources.size() ? "" : sources.get(index + 1);
    }

    static String compactSource(String source) {
        if (source == null || source.isBlank()) return "?";
        int slash = Math.max(source.lastIndexOf('/'), source.lastIndexOf('\\'));
        return slash >= 0 && slash + 1 < source.length() ? source.substring(slash + 1) : source;
    }

    private static <T> List<T> filter(
            List<T> values,
            String query,
            State state,
            String source,
            Function<T, String> searchableText,
            Function<T, Boolean> enabled,
            Function<T, Boolean> available,
            Function<T, String> sourceGetter
    ) {
        String selectedSource = source == null ? "" : source;
        return values.stream()
                .filter(value -> selectedSource.isBlank() || selectedSource.equals(sourceGetter.apply(value)))
                .filter(value -> matchesState(state, enabled.apply(value), available.apply(value)))
                .filter(value -> matchesQuery(query, searchableText.apply(value)))
                .toList();
    }

    private static boolean matchesState(State state, boolean enabled, boolean available) {
        return switch (state) {
            case ALL -> true;
            case ACTIVE -> enabled && available;
            case DISABLED -> !enabled;
            case UNAVAILABLE -> enabled && !available;
        };
    }

    private static boolean matchesQuery(String query, String searchableText) {
        if (query == null || query.isBlank()) return true;
        String haystack = searchableText.toLowerCase(Locale.ROOT);
        for (String token : query.strip().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!haystack.contains(token)) return false;
        }
        return true;
    }

    private static String ids(Iterable<ResourceLocation> ids) {
        ArrayList<String> values = new ArrayList<>();
        ids.forEach(id -> values.add(id.toString()));
        return String.join(" ", values);
    }

    private static String join(String... values) {
        return java.util.Arrays.stream(values).collect(Collectors.joining(" "));
    }

    private static void addSource(Set<String> sources, String source) {
        if (source != null && !source.isBlank()) sources.add(source);
    }

    private static List<String> sortedSources(List<String> values) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        values.forEach(value -> addSource(sources, value));
        return sources.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
