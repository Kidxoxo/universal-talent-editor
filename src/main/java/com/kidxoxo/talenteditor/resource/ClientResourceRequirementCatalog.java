package com.kidxoxo.talenteditor.resource;

import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.PlaySoundAction;
import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/** Deterministically derives the bounded asset contract sent to connected clients. */
public final class ClientResourceRequirementCatalog {
    public static final int MAX_REQUIREMENTS = 4_096;

    private ClientResourceRequirementCatalog() {
    }

    public static Result from(RuntimeSnapshot snapshot) {
        LinkedHashSet<ClientResourceRequirement> found = new LinkedHashSet<>();
        snapshot.talents().values().stream()
                .filter(talent -> talent.enabled() && talent.available())
                .sorted(Comparator.comparing(talent -> talent.id().toString()))
                .forEach(talent -> {
                    talent.icon().ifPresent(id -> found.add(requirement(
                            ClientResourceRequirement.Kind.TEXTURE, id
                    )));
                    collectSounds(found, talent.activeEffects());
                    collectSounds(found, talent.onAcquire());
                    collectSounds(found, talent.onSynthesize());
                    collectSounds(found, talent.onRemove());
                    talent.triggers().forEach(trigger -> {
                        collectSounds(found, trigger.actions());
                        collectSounds(found, trigger.failureActions());
                    });
                });

        var layout = snapshot.bookLayout();
        if (layout.enabled()) {
            layout.background().ifPresent(id -> found.add(requirement(
                    ClientResourceRequirement.Kind.TEXTURE, id
            )));
            layout.pages().forEach(page -> page.background().ifPresent(id -> found.add(requirement(
                    ClientResourceRequirement.Kind.TEXTURE, id
            ))));
        }

        snapshot.configuredItems().values().stream()
                .filter(item -> item.enabled() && item.available() && item.use().enabled())
                .sorted(Comparator.comparing(item -> item.id().toString()))
                .forEach(item -> collectSounds(found, item.use().actions()));

        List<ClientResourceRequirement> sorted = found.stream()
                .sorted(Comparator.comparing((ClientResourceRequirement value) -> value.kind().ordinal())
                        .thenComparing(value -> value.id().toString()))
                .toList();
        boolean truncated = sorted.size() > MAX_REQUIREMENTS;
        return new Result(sorted.stream().limit(MAX_REQUIREMENTS).toList(), truncated);
    }

    private static void collectSounds(
            LinkedHashSet<ClientResourceRequirement> target,
            List<CompiledAction> actions
    ) {
        actions.forEach(action -> {
            if (action instanceof PlaySoundAction sound) {
                target.add(requirement(ClientResourceRequirement.Kind.SOUND, sound.sound()));
            }
        });
    }

    private static ClientResourceRequirement requirement(
            ClientResourceRequirement.Kind kind,
            net.minecraft.resources.ResourceLocation id
    ) {
        return new ClientResourceRequirement(kind, id);
    }

    public record Result(List<ClientResourceRequirement> requirements, boolean truncated) {
        public Result {
            requirements = List.copyOf(requirements);
        }

        public long textureCount() {
            return requirements.stream().filter(value ->
                    value.kind() == ClientResourceRequirement.Kind.TEXTURE).count();
        }

        public long soundCount() {
            return requirements.stream().filter(value ->
                    value.kind() == ClientResourceRequirement.Kind.SOUND).count();
        }
    }
}
