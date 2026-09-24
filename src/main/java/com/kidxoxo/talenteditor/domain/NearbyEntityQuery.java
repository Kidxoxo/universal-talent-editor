package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Optional;

/**
 * Bounded query used by {@code nearby_count("...")} expressions.
 *
 * <p>Format: {@code radius=16;relation=any;kind=player;type=minecraft:player;
 * tag=minecraft:undead;include_self=false}. Every key except {@code radius}
 * is optional. Type and tag filters may be combined.</p>
 */
public record NearbyEntityQuery(
        double radius,
        EntityRelation relation,
        Kind kind,
        Optional<ResourceLocation> type,
        Optional<ResourceLocation> tag,
        boolean includeSelf
) {
    public NearbyEntityQuery {
        if (!Double.isFinite(radius) || radius < 0.5D || radius > 256.0D) {
            throw new IllegalArgumentException("nearby_count radius must be 0.5..256");
        }
        relation = relation == null ? EntityRelation.ANY : relation;
        kind = kind == null ? Kind.LIVING : kind;
        type = type == null ? Optional.empty() : type;
        tag = tag == null ? Optional.empty() : tag;
    }

    public static NearbyEntityQuery parse(String source) {
        double radius = -1.0D;
        EntityRelation relation = EntityRelation.ANY;
        Kind kind = Kind.LIVING;
        ResourceLocation type = null;
        ResourceLocation tag = null;
        boolean includeSelf = false;
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("nearby_count query must not be blank");
        }
        for (String rawPart : source.split(";")) {
            String part = rawPart.strip();
            if (part.isEmpty()) continue;
            int equals = part.indexOf('=');
            if (equals <= 0 || equals == part.length() - 1) {
                throw new IllegalArgumentException("Invalid nearby_count entry: " + part);
            }
            String key = part.substring(0, equals).strip().toLowerCase(Locale.ROOT);
            String value = part.substring(equals + 1).strip();
            try {
                switch (key) {
                    case "radius" -> radius = Double.parseDouble(value);
                    case "relation" -> relation = EntityRelation.parse(value);
                    case "kind" -> kind = Kind.parse(value);
                    case "type" -> type = requiredId(value, "type");
                    case "tag" -> tag = requiredId(value, "tag");
                    case "include_self" -> {
                        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                            throw new IllegalArgumentException("include_self must be true or false");
                        }
                        includeSelf = Boolean.parseBoolean(value);
                    }
                    default -> throw new IllegalArgumentException("Unknown nearby_count key: " + key);
                }
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid nearby_count number for " + key, exception);
            }
        }
        return new NearbyEntityQuery(radius, relation, kind,
                Optional.ofNullable(type), Optional.ofNullable(tag), includeSelf);
    }

    private static ResourceLocation requiredId(String value, String field) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !value.contains(":")) {
            throw new IllegalArgumentException("nearby_count " + field + " must be a namespaced ID");
        }
        return id;
    }

    public enum Kind {
        LIVING,
        PLAYER;

        public static Kind parse(String value) {
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("nearby_count kind must be living or player", exception);
            }
        }
    }
}
