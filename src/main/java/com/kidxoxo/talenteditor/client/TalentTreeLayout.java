package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Deterministic left-to-right layout for acquisition and synthesis relationships. */
final class TalentTreeLayout {
    static final int NODE_WIDTH = 132;
    static final int NODE_HEIGHT = 54;
    private static final int COLUMN_GAP = 58;
    private static final int ROW_GAP = 28;
    private static final int COMPONENT_GAP = 64;

    private TalentTreeLayout() {
    }

    static Result arrange(List<EditorBootstrap.TalentSummary> summaries) {
        ArrayList<EditorBootstrap.TalentSummary> talents = new ArrayList<>(summaries);
        talents.sort(Comparator.comparing(value -> value.id().toString()));
        LinkedHashMap<ResourceLocation, EditorBootstrap.TalentSummary> byId = new LinkedHashMap<>();
        talents.forEach(talent -> byId.put(talent.id(), talent));

        Map<ResourceLocation, LinkedHashSet<ResourceLocation>> outgoing = new HashMap<>();
        Map<ResourceLocation, LinkedHashSet<ResourceLocation>> incoming = new HashMap<>();
        byId.keySet().forEach(id -> {
            outgoing.put(id, new LinkedHashSet<>());
            incoming.put(id, new LinkedHashSet<>());
        });
        for (EditorBootstrap.TalentSummary target : talents) {
            LinkedHashSet<ResourceLocation> sources = new LinkedHashSet<>(target.acquireTalents());
            sources.addAll(target.synthesisTalents());
            for (ResourceLocation source : sources) {
                if (!byId.containsKey(source) || source.equals(target.id())) continue;
                if (outgoing.get(source).add(target.id())) {
                    incoming.get(target.id()).add(source);
                }
            }
        }
        LinkedHashMap<ResourceLocation, Position> positions = new LinkedHashMap<>();
        LinkedHashSet<ResourceLocation> unvisited = new LinkedHashSet<>(byId.keySet());
        int graphWidth = NODE_WIDTH;
        int graphHeight = 0;
        while (!unvisited.isEmpty()) {
            ResourceLocation seed = unvisited.iterator().next();
            LinkedHashSet<ResourceLocation> component = new LinkedHashSet<>();
            ArrayDeque<ResourceLocation> connected = new ArrayDeque<>();
            connected.add(seed);
            while (!connected.isEmpty()) {
                ResourceLocation id = connected.removeFirst();
                if (!unvisited.remove(id)) continue;
                component.add(id);
                outgoing.get(id).forEach(connected::addLast);
                incoming.get(id).forEach(connected::addLast);
            }
            ComponentSize size = arrangeComponent(component, byId, outgoing, incoming,
                    positions, graphHeight);
            graphWidth = Math.max(graphWidth, size.width());
            graphHeight += size.height() + COMPONENT_GAP;
        }
        return new Result(positions, graphWidth, Math.max(NODE_HEIGHT, graphHeight - COMPONENT_GAP));
    }

    private static ComponentSize arrangeComponent(
            LinkedHashSet<ResourceLocation> component,
            Map<ResourceLocation, EditorBootstrap.TalentSummary> byId,
            Map<ResourceLocation, LinkedHashSet<ResourceLocation>> outgoing,
            Map<ResourceLocation, LinkedHashSet<ResourceLocation>> incoming,
            Map<ResourceLocation, Position> positions,
            int yOffset
    ) {
        Map<ResourceLocation, Integer> indegree = new HashMap<>();
        Map<ResourceLocation, Integer> depth = new HashMap<>();
        component.forEach(id -> {
            indegree.put(id, incoming.get(id).size());
            depth.put(id, 0);
        });
        ArrayDeque<ResourceLocation> ready = new ArrayDeque<>();
        component.stream().filter(id -> indegree.get(id) == 0).forEach(ready::addLast);
        LinkedHashSet<ResourceLocation> processed = new LinkedHashSet<>();
        while (!ready.isEmpty()) {
            ResourceLocation source = ready.removeFirst();
            if (!processed.add(source)) continue;
            outgoing.get(source).stream().sorted(Comparator.comparing(ResourceLocation::toString))
                    .forEach(target -> {
                        depth.put(target, Math.max(depth.get(target), depth.get(source) + 1));
                        if (indegree.compute(target, (ignored, value) -> value - 1) == 0) ready.addLast(target);
                    });
        }
        int cycleDepth = processed.stream().mapToInt(depth::get).max().orElse(-1) + 1;
        component.stream().filter(id -> !processed.contains(id)).forEach(id -> depth.put(id, cycleDepth));

        Map<Integer, ArrayList<ResourceLocation>> layers = new HashMap<>();
        component.forEach(id -> layers.computeIfAbsent(depth.get(id), ignored -> new ArrayList<>()).add(id));
        Comparator<ResourceLocation> byName = Comparator
                .comparing((ResourceLocation id) -> byId.get(id).name(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ResourceLocation::toString);
        Map<ResourceLocation, Integer> row = new HashMap<>();
        int maxDepth = layers.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        int maxRows = 1;
        for (int layerIndex = 0; layerIndex <= maxDepth; layerIndex++) {
            ArrayList<ResourceLocation> layer = layers.get(layerIndex);
            if (layer == null) continue;
            layer.sort(Comparator.comparingDouble((ResourceLocation id) -> incoming.get(id).stream()
                            .filter(parent -> depth.get(parent) < depth.get(id) && row.containsKey(parent))
                            .mapToInt(row::get).average().orElse(Double.MAX_VALUE))
                    .thenComparing(byName));
            maxRows = Math.max(maxRows, layer.size());
            for (int index = 0; index < layer.size(); index++) {
                ResourceLocation id = layer.get(index);
                row.put(id, index);
                positions.put(id, new Position(
                        layerIndex * (NODE_WIDTH + COLUMN_GAP),
                        yOffset + index * (NODE_HEIGHT + ROW_GAP), layerIndex, index
                ));
            }
        }
        return new ComponentSize(maxDepth * (NODE_WIDTH + COLUMN_GAP) + NODE_WIDTH,
                (maxRows - 1) * (NODE_HEIGHT + ROW_GAP) + NODE_HEIGHT);
    }

    private record ComponentSize(int width, int height) {
    }

    record Position(int x, int y, int depth, int row) {
    }

    record Result(Map<ResourceLocation, Position> positions, int width, int height) {
        Result {
            positions = Map.copyOf(positions);
        }
    }
}
