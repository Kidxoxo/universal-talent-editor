package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayDeque;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ConfiguredItemLootRule {
    public static final int MAX_TABLE_EXPRESSION_LENGTH = 256;
    private static final Pattern PREFIX_EXPRESSION = Pattern.compile(
            "[a-z0-9_.-]+:[a-z0-9_./-]*"
    );

    private final ConfiguredItemLootTableMatch tableMatch;
    private final String tableExpression;
    private final Pattern compiledPattern;
    private final double chance;
    private final double chancePerLuck;
    private final int minCount;
    private final int maxCount;
    private final List<ResourceLocation> dimensions;
    private final ConfiguredItemLootEntityTarget entityTarget;
    private final List<ResourceLocation> entityTypes;
    private final List<ResourceLocation> entityTags;

    public ConfiguredItemLootRule(
            ConfiguredItemLootTableMatch tableMatch,
            String tableExpression,
            double chance,
            double chancePerLuck,
            int minCount,
            int maxCount,
            List<ResourceLocation> dimensions,
            ConfiguredItemLootEntityTarget entityTarget,
            List<ResourceLocation> entityTypes,
            List<ResourceLocation> entityTags
    ) {
        this.tableMatch = Objects.requireNonNull(tableMatch, "tableMatch");
        this.tableExpression = Objects.requireNonNull(tableExpression, "tableExpression").strip();
        this.compiledPattern = compileTableExpression(tableMatch, this.tableExpression);
        if (!Double.isFinite(chance) || chance < 0.0D || chance > 1.0D) {
            throw new IllegalArgumentException("chance must be finite and in range 0..1");
        }
        if (!Double.isFinite(chancePerLuck) || chancePerLuck < -1.0D || chancePerLuck > 1.0D) {
            throw new IllegalArgumentException("chancePerLuck must be finite and in range -1..1");
        }
        if (minCount < 1 || maxCount < minCount || maxCount > 64) {
            throw new IllegalArgumentException("counts must satisfy 1 <= minCount <= maxCount <= 64");
        }
        this.chance = chance;
        this.chancePerLuck = chancePerLuck;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.dimensions = List.copyOf(dimensions);
        this.entityTarget = Objects.requireNonNull(entityTarget, "entityTarget");
        this.entityTypes = List.copyOf(entityTypes);
        this.entityTags = List.copyOf(entityTags);
    }

    public ConfiguredItemLootRule(
            ResourceLocation table,
            double chance,
            double chancePerLuck,
            int minCount,
            int maxCount,
            List<ResourceLocation> dimensions,
            ConfiguredItemLootEntityTarget entityTarget,
            List<ResourceLocation> entityTypes,
            List<ResourceLocation> entityTags
    ) {
        this(ConfiguredItemLootTableMatch.EXACT, Objects.requireNonNull(table, "table").toString(),
                chance, chancePerLuck, minCount, maxCount, dimensions, entityTarget, entityTypes, entityTags);
    }

    public ConfiguredItemLootRule(ResourceLocation table, double chance, int minCount, int maxCount) {
        this(table, chance, 0.0D, minCount, maxCount, List.of(),
                ConfiguredItemLootEntityTarget.THIS_ENTITY, List.of(), List.of());
    }

    public ConfiguredItemLootTableMatch tableMatch() {
        return tableMatch;
    }

    public String tableExpression() {
        return tableExpression;
    }

    public Optional<ResourceLocation> exactTable() {
        return tableMatch == ConfiguredItemLootTableMatch.EXACT
                ? Optional.ofNullable(ResourceLocation.tryParse(tableExpression)) : Optional.empty();
    }

    public boolean matchesTable(ResourceLocation tableId) {
        Objects.requireNonNull(tableId, "tableId");
        String value = tableId.toString();
        return switch (tableMatch) {
            case EXACT -> tableExpression.equals(value);
            case PREFIX -> value.startsWith(tableExpression);
            case REGEX -> compiledPattern.matcher(value).matches();
        };
    }

    public double chance() {
        return chance;
    }

    public double chancePerLuck() {
        return chancePerLuck;
    }

    public int minCount() {
        return minCount;
    }

    public int maxCount() {
        return maxCount;
    }

    public List<ResourceLocation> dimensions() {
        return dimensions;
    }

    public ConfiguredItemLootEntityTarget entityTarget() {
        return entityTarget;
    }

    public List<ResourceLocation> entityTypes() {
        return entityTypes;
    }

    public List<ResourceLocation> entityTags() {
        return entityTags;
    }

    public double effectiveChance(double luck) {
        if (!Double.isFinite(luck)) luck = 0.0D;
        return Math.max(0.0D, Math.min(1.0D, chance + luck * chancePerLuck));
    }

    public static void validateTableExpression(
            ConfiguredItemLootTableMatch tableMatch,
            String tableExpression
    ) {
        compileTableExpression(tableMatch, tableExpression == null ? "" : tableExpression.strip());
    }

    private static Pattern compileTableExpression(
            ConfiguredItemLootTableMatch tableMatch,
            String tableExpression
    ) {
        if (tableExpression.isBlank() || tableExpression.length() > MAX_TABLE_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException("table expression length must be 1.."
                    + MAX_TABLE_EXPRESSION_LENGTH);
        }
        return switch (tableMatch) {
            case EXACT -> {
                if (!tableExpression.contains(":") || ResourceLocation.tryParse(tableExpression) == null) {
                    throw new IllegalArgumentException("exact table expression must be a namespaced resource ID");
                }
                yield null;
            }
            case PREFIX -> {
                if (!PREFIX_EXPRESSION.matcher(tableExpression).matches()) {
                    throw new IllegalArgumentException(
                            "prefix table expression must use namespaced resource ID characters");
                }
                yield null;
            }
            case REGEX -> {
                try {
                    validateRegexSafety(tableExpression);
                    yield Pattern.compile(tableExpression);
                } catch (PatternSyntaxException exception) {
                    throw new IllegalArgumentException("invalid table regular expression: "
                            + exception.getDescription());
                }
            }
        };
    }

    private static void validateRegexSafety(String expression) {
        ArrayDeque<RegexGroup> groups = new ArrayDeque<>();
        boolean escaped = false;
        boolean characterClass = false;
        for (int index = 0; index < expression.length(); index++) {
            char current = expression.charAt(index);
            if (escaped) {
                if (current >= '1' && current <= '9') {
                    throw new IllegalArgumentException("regex backreferences are not supported");
                }
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '[') {
                characterClass = true;
                continue;
            }
            if (current == ']' && characterClass) {
                characterClass = false;
                continue;
            }
            if (characterClass) continue;
            if (current == '(') {
                if (expression.startsWith("(?:", index)) {
                    groups.push(new RegexGroup());
                    index += 2;
                    continue;
                }
                if (index + 1 < expression.length() && expression.charAt(index + 1) == '?') {
                    throw new IllegalArgumentException(
                            "regex lookarounds and embedded flags are not supported");
                }
                groups.push(new RegexGroup());
                continue;
            }
            if (current == '|') {
                if (!groups.isEmpty()) groups.peek().alternation = true;
                continue;
            }
            if (current == '*' || current == '+' || current == '?' || current == '{') {
                if (!groups.isEmpty()) groups.peek().quantifier = true;
                continue;
            }
            if (current == ')' && !groups.isEmpty()) {
                RegexGroup completed = groups.pop();
                boolean groupQuantified = index + 1 < expression.length()
                        && "*+?{".indexOf(expression.charAt(index + 1)) >= 0;
                if (groupQuantified && (completed.quantifier || completed.alternation)) {
                    throw new IllegalArgumentException("nested or alternated quantified groups are not supported");
                }
                if (!groups.isEmpty()) {
                    groups.peek().quantifier |= completed.quantifier || groupQuantified;
                    groups.peek().alternation |= completed.alternation;
                }
            }
        }
    }

    private static final class RegexGroup {
        private boolean quantifier;
        private boolean alternation;
    }
}
