package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Set;

final class ReferenceDiagnostics {
    private ReferenceDiagnostics() {
    }

    static Summary summarize(
            EditorBootstrap bootstrap,
            ResourceReferencePickerScreen.Source source,
            String rawValue,
            boolean valuesAfterEquals
    ) {
        Summary result = Summary.EMPTY;
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        for (String raw : rawValue.split(",")) {
            String entry = raw.strip();
            if (entry.isEmpty()) continue;
            int equals = valuesAfterEquals ? entry.indexOf('=') : -1;
            String rawId = equals < 0 ? entry : entry.substring(0, equals).strip();
            ResourceLocation id = ResourceLocation.tryParse(rawId);
            if (id == null) {
                result = result.add(Status.MALFORMED);
            } else if (seen.add(id)) {
                result = result.add(inspect(bootstrap, source, id));
            }
        }
        return result;
    }

    static Status inspect(
            EditorBootstrap bootstrap,
            ResourceReferencePickerScreen.Source source,
            ResourceLocation id
    ) {
        return switch (source) {
            case TALENT -> bootstrap.talents().stream().filter(value -> value.id().equals(id)).findFirst()
                    .map(value -> status(value.enabled(), value.available())).orElse(Status.NOT_PRESENT);
            case COUNTER -> bootstrap.counters().stream().filter(value -> value.id().equals(id)).findFirst()
                    .map(value -> status(value.enabled(), value.available())).orElse(Status.NOT_PRESENT);
            case NUMERIC_STATE -> bootstrap.numericStates().contains(id)
                    ? Status.AVAILABLE : Status.NOT_PRESENT;
            case COUNTER_CATEGORY -> bootstrap.counters().stream()
                    .anyMatch(value -> value.categories().contains(id)) ? Status.AVAILABLE : Status.NOT_PRESENT;
            case CATEGORY -> bootstrap.talents().stream()
                    .anyMatch(value -> value.categories().contains(id)) ? Status.AVAILABLE : Status.NOT_PRESENT;
            case ITEM -> bootstrap.configuredItems().stream().filter(value -> value.id().equals(id)).findFirst()
                    .map(value -> status(value.enabled(), value.available()))
                    .orElseGet(() -> ForgeRegistries.ITEMS.containsKey(id)
                            ? Status.AVAILABLE : Status.NOT_PRESENT);
            case BLOCK -> ForgeRegistries.BLOCKS.containsKey(id) ? Status.AVAILABLE : Status.NOT_PRESENT;
            case SOUND_EVENT, TEXTURE -> ResourceReferencePickerScreen.clientResourceStatus(source, id);
            case POOL -> bootstrap.pools().stream().filter(value -> value.id().equals(id)).findFirst()
                    .map(value -> value.enabled() ? Status.AVAILABLE : Status.DISABLED)
                    .orElse(Status.NOT_PRESENT);
            case RARITY -> bootstrap.rarities().stream().filter(value -> value.id().equals(id)).findFirst()
                    .map(value -> value.enabled() ? Status.AVAILABLE : Status.DISABLED)
                    .orElse(Status.NOT_PRESENT);
            case MOB_EFFECT -> ForgeRegistries.MOB_EFFECTS.containsKey(id)
                    ? Status.AVAILABLE : Status.NOT_PRESENT;
            case ATTRIBUTE -> bootstrap.attributes().stream().filter(value -> value.id().equals(id)).findFirst()
                    .map(value -> value.attachedToTarget() ? Status.AVAILABLE : Status.UNAVAILABLE)
                    .orElse(Status.NOT_PRESENT);
            case ENTITY_TYPE -> ForgeRegistries.ENTITY_TYPES.containsKey(id)
                    ? Status.AVAILABLE : Status.NOT_PRESENT;
            case ENTITY_TYPE_TAG -> bootstrap.entityTypeTags().contains(id)
                    ? Status.AVAILABLE : Status.NOT_PRESENT;
            case ITEM_TAG -> bootstrap.itemTags().contains(id)
                    ? Status.AVAILABLE : Status.NOT_PRESENT;
            case DAMAGE_TYPE_TAG -> bootstrap.damageTypeTags().contains(id)
                    ? Status.AVAILABLE : Status.NOT_PRESENT;
            case BLOCK_TAG -> ResourceReferencePickerScreen.clientBlockTags().contains(id)
                    ? Status.AVAILABLE : Status.NOT_PRESENT;
        };
    }

    static Status status(boolean enabled, boolean available) {
        if (!enabled) return Status.DISABLED;
        return available ? Status.AVAILABLE : Status.UNAVAILABLE;
    }

    static Component label(Summary summary) {
        if (summary.total() == 0) {
            return Component.translatable("screen.talent_editor.reference_diagnostics.none");
        }
        return Component.translatable(
                "screen.talent_editor.reference_diagnostics.summary",
                summary.available(), summary.disabled(), summary.unavailable(),
                summary.notPresent(), summary.malformed()
        );
    }

    static int color(Summary summary) {
        return summary.hasWarnings() ? 0xFFFFAA55 : 0xFF77CC77;
    }

    enum Status {
        AVAILABLE("available", 0xFF77CC77),
        DISABLED("disabled", 0xFFFFAA55),
        UNAVAILABLE("unavailable", 0xFFFF7777),
        NOT_PRESENT("not_present", 0xFFFFAA55),
        MALFORMED("malformed", 0xFFFF5555);

        private final String serializedName;
        private final int color;

        Status(String serializedName, int color) {
            this.serializedName = serializedName;
            this.color = color;
        }

        Component label() {
            return Component.translatable("screen.talent_editor.reference_status." + serializedName);
        }

        int color() {
            return color;
        }
    }

    record Summary(int available, int disabled, int unavailable, int notPresent, int malformed) {
        private static final Summary EMPTY = new Summary(0, 0, 0, 0, 0);

        int total() {
            return available + disabled + unavailable + notPresent + malformed;
        }

        boolean hasWarnings() {
            return disabled > 0 || unavailable > 0 || notPresent > 0 || malformed > 0;
        }

        Summary add(Status status) {
            return switch (status) {
                case AVAILABLE -> new Summary(available + 1, disabled, unavailable, notPresent, malformed);
                case DISABLED -> new Summary(available, disabled + 1, unavailable, notPresent, malformed);
                case UNAVAILABLE -> new Summary(available, disabled, unavailable + 1, notPresent, malformed);
                case NOT_PRESENT -> new Summary(available, disabled, unavailable, notPresent + 1, malformed);
                case MALFORMED -> new Summary(available, disabled, unavailable, notPresent, malformed + 1);
            };
        }

        Summary plus(Summary other) {
            return new Summary(
                    available + other.available,
                    disabled + other.disabled,
                    unavailable + other.unavailable,
                    notPresent + other.notPresent,
                    malformed + other.malformed
            );
        }
    }
}
