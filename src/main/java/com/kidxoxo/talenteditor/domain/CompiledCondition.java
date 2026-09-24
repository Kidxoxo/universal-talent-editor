package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public sealed interface CompiledCondition permits CompiledCondition.Always, CompiledCondition.All,
        CompiledCondition.Any, CompiledCondition.Not, CompiledCondition.HasTalent,
        CompiledCondition.CounterAtLeast, CompiledCondition.CategoryCountAtLeast,
        CompiledCondition.CounterCategoryTotalAtLeast, CompiledCondition.HasItem,
        CompiledCondition.AttributeCompare,
        CompiledCondition.EventEntityType, CompiledCondition.EventEntityTag,
        CompiledCondition.EventEntityEffect, CompiledCondition.DamageTypeTag,
        CompiledCondition.EventTalent, CompiledCondition.EventTalentCategory,
        CompiledCondition.EventTalentRarity, CompiledCondition.EventItem,
        CompiledCondition.EventItemTag, CompiledCondition.EventItemEdible, CompiledCondition.EventBlock,
        CompiledCondition.EventBlockTag, CompiledCondition.CuriosEquipped,
        CompiledCondition.ExpressionCompare {
    ResourceLocation type();

    record Always(boolean value) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "always");

        @Override
        public ResourceLocation type() {
            return TYPE;
        }
    }

    record All(List<CompiledCondition> children) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "all");

        public All {
            children = List.copyOf(children);
        }

        @Override
        public ResourceLocation type() {
            return TYPE;
        }
    }

    record Any(List<CompiledCondition> children) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "any");

        public Any {
            children = List.copyOf(children);
        }

        @Override
        public ResourceLocation type() {
            return TYPE;
        }
    }

    record Not(CompiledCondition child) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "not");

        public Not {
            child = Objects.requireNonNull(child, "child");
        }

        @Override
        public ResourceLocation type() {
            return TYPE;
        }
    }

    record HasTalent(ResourceLocation talent) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "has_talent");

        public HasTalent {
            talent = Objects.requireNonNull(talent, "talent");
        }

        @Override
        public ResourceLocation type() {
            return TYPE;
        }
    }

    record CounterAtLeast(ResourceLocation counter, long value) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "counter_at_least");

        public CounterAtLeast {
            counter = Objects.requireNonNull(counter, "counter");
        }

        @Override
        public ResourceLocation type() {
            return TYPE;
        }
    }

    record CategoryCountAtLeast(ResourceLocation category, int count) implements CompiledCondition {
        public static final ResourceLocation TYPE =
                new ResourceLocation(TalentEditorMod.MOD_ID, "category_count_at_least");

        public CategoryCountAtLeast {
            category = Objects.requireNonNull(category, "category");
        }

        @Override
        public ResourceLocation type() {
            return TYPE;
        }
    }

    record CounterCategoryTotalAtLeast(ResourceLocation category, long value) implements CompiledCondition {
        public static final ResourceLocation TYPE =
                new ResourceLocation(TalentEditorMod.MOD_ID, "counter_category_total_at_least");

        public CounterCategoryTotalAtLeast {
            category = Objects.requireNonNull(category, "category");
        }

        @Override
        public ResourceLocation type() {
            return TYPE;
        }
    }

    record HasItem(ResourceLocation item, int count) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "has_item");

        public HasItem {
            item = Objects.requireNonNull(item, "item");
        }

        @Override
        public ResourceLocation type() {
            return TYPE;
        }
    }

    record EventEntityType(ResourceLocation entityType) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "event_entity_type");

        public EventEntityType {
            entityType = Objects.requireNonNull(entityType, "entityType");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventEntityTag(ResourceLocation tag) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "event_entity_tag");

        public EventEntityTag {
            tag = Objects.requireNonNull(tag, "tag");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventEntityEffect(ResourceLocation effect) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "event_entity_effect");

        public EventEntityEffect {
            effect = Objects.requireNonNull(effect, "effect");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record DamageTypeTag(ResourceLocation tag) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "damage_type_tag");

        public DamageTypeTag {
            tag = Objects.requireNonNull(tag, "tag");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventTalent(ResourceLocation talent) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "event_talent");

        public EventTalent {
            talent = Objects.requireNonNull(talent, "talent");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventTalentCategory(ResourceLocation category) implements CompiledCondition {
        public static final ResourceLocation TYPE =
                new ResourceLocation(TalentEditorMod.MOD_ID, "event_talent_category");

        public EventTalentCategory {
            category = Objects.requireNonNull(category, "category");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventTalentRarity(ResourceLocation rarity) implements CompiledCondition {
        public static final ResourceLocation TYPE =
                new ResourceLocation(TalentEditorMod.MOD_ID, "event_talent_rarity");

        public EventTalentRarity {
            rarity = Objects.requireNonNull(rarity, "rarity");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventItem(ResourceLocation item) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "event_item");

        public EventItem {
            item = Objects.requireNonNull(item, "item");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventItemTag(ResourceLocation tag) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "event_item_tag");

        public EventItemTag {
            tag = Objects.requireNonNull(tag, "tag");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventItemEdible() implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "event_item_edible");

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventBlock(ResourceLocation block) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "event_block");

        public EventBlock {
            block = Objects.requireNonNull(block, "block");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record EventBlockTag(ResourceLocation tag) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "event_block_tag");

        public EventBlockTag {
            tag = Objects.requireNonNull(tag, "tag");
        }

        @Override
        public ResourceLocation type() { return TYPE; }
    }

    record CuriosEquipped(ResourceLocation item) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "curios_equipped");
        public CuriosEquipped { item = Objects.requireNonNull(item, "item"); }
        @Override public ResourceLocation type() { return TYPE; }
    }

    /** Compares the player's current effective attribute value, including equipment and modifiers. */
    record AttributeCompare(ResourceLocation attribute, ComparisonOperator operator, double value)
            implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "attribute_compare");
        public AttributeCompare {
            attribute = Objects.requireNonNull(attribute, "attribute");
            operator = Objects.requireNonNull(operator, "operator");
            if (!Double.isFinite(value)) throw new IllegalArgumentException("value must be finite");
        }
        @Override public ResourceLocation type() { return TYPE; }
    }

    record ExpressionCompare(
            NumericExpression left,
            ComparisonOperator operator,
            NumericExpression right
    ) implements CompiledCondition {
        public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "expression_compare");

        public ExpressionCompare {
            left = Objects.requireNonNull(left, "left");
            operator = Objects.requireNonNull(operator, "operator");
            right = Objects.requireNonNull(right, "right");
        }

        @Override public ResourceLocation type() { return TYPE; }
    }
}
