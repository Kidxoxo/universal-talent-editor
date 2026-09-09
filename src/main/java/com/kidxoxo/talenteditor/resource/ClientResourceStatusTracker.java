package com.kidxoxo.talenteditor.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-memory-only status for online clients. It never accepts or distributes file content. */
public final class ClientResourceStatusTracker {
    private static final int MAX_RETAINED_MISSING = 64;
    private final Map<UUID, Report> reports = new LinkedHashMap<>();

    public synchronized void begin(
            UUID player,
            String playerName,
            long revision,
            String contentHash,
            List<ClientResourceRequirement> expected,
            boolean catalogTruncated
    ) {
        reports.put(player, new Report(
                player, playerName, revision, contentHash, State.PENDING, expected.size(), 0,
                expected.size(), 0, 0, catalogTruncated,
                expected.stream().limit(MAX_RETAINED_MISSING).toList(), List.of()
        ));
    }

    public synchronized boolean accept(
            UUID player,
            long revision,
            String contentHash,
            List<ClientResourceStatus> statuses,
            List<ClientResourceRequirement> expected
    ) {
        Report pending = reports.get(player);
        if (pending == null || pending.revision() != revision || !pending.contentHash().equals(contentHash)) {
            return false;
        }
        return acceptChecked(pending, statuses, new HashSet<>(expected));
    }

    private boolean acceptChecked(
            Report pending,
            List<ClientResourceStatus> statuses,
            Set<ClientResourceRequirement> expected
    ) {
        LinkedHashMap<ClientResourceRequirement, ClientResourceStatus> unique = new LinkedHashMap<>();
        int duplicates = 0;
        for (ClientResourceStatus status : statuses) {
            if (unique.putIfAbsent(status.requirement(), status) != null) duplicates++;
        }
        int unexpected = (int) unique.keySet().stream().filter(value -> !expected.contains(value)).count();
        ArrayList<ClientResourceRequirement> missing = new ArrayList<>();
        int available = 0;
        int hashed = 0;
        for (ClientResourceRequirement requirement : expected) {
            ClientResourceStatus status = unique.get(requirement);
            if (status != null && status.available()) {
                available++;
                if (!status.sha256().isEmpty()) hashed++;
            } else if (missing.size() < MAX_RETAINED_MISSING) {
                missing.add(requirement);
            }
        }
        missing.sort(Comparator.comparing((ClientResourceRequirement value) -> value.kind().ordinal())
                .thenComparing(value -> value.id().toString()));
        int missingCount = Math.max(0, expected.size() - available);
        State state = duplicates > 0 || unexpected > 0 || unique.size() != expected.size()
                ? State.INCOMPLETE : missingCount > 0 ? State.MISSING : State.READY;
        reports.put(pending.player(), new Report(
                pending.player(), pending.playerName(), pending.revision(), pending.contentHash(), state,
                expected.size(), available, missingCount, unexpected, hashed, pending.catalogTruncated(),
                missing, List.copyOf(unique.values())
        ));
        return true;
    }

    public synchronized void remove(UUID player) {
        reports.remove(player);
    }

    public synchronized List<Report> snapshot() {
        return reports.values().stream()
                .sorted(Comparator.comparing(Report::playerName).thenComparing(Report::player))
                .toList();
    }

    public enum State {
        PENDING,
        READY,
        MISSING,
        INCOMPLETE
    }

    public record Report(
            UUID player,
            String playerName,
            long revision,
            String contentHash,
            State state,
            int required,
            int available,
            int missing,
            int unexpected,
            int hashed,
            boolean catalogTruncated,
            List<ClientResourceRequirement> missingEntries,
            List<ClientResourceStatus> statuses
    ) {
        public Report {
            missingEntries = List.copyOf(missingEntries);
            statuses = List.copyOf(statuses);
        }
    }
}
