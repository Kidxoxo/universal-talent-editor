package com.kidxoxo.talenteditor.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.AttributeOperation;
import com.kidxoxo.talenteditor.domain.ConfiguredItemDefinition;
import com.kidxoxo.talenteditor.domain.ConfiguredItemAttributeModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public final class ConfiguredItem extends Item {
    public static final String DEFINITION_TAG = "TalentEditorDefinition";
    public static final String GLINT_TAG = "TalentEditorGlint";
    public static final String ATTRIBUTE_MODIFIERS_TAG = "TalentEditorAttributeModifiers";
    public static final String REVISION_TAG = "TalentEditorRevision";

    public ConfiguredItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(ConfiguredItemDefinition definition, int count) {
        Item item = definition.id().equals(ModItems.TALENT_BOOK_ID)
                ? ModItems.TALENT_BOOK.get()
                : definition.id().equals(ModItems.TALENT_POINT_ID)
                ? ModItems.TALENT_POINT.get() : ModItems.CONFIGURED_ITEM.get();
        ItemStack stack = new ItemStack(item, Math.max(1, Math.min(item.getMaxStackSize(), count)));
        applyDefinition(stack, definition, TalentEditorMod.SNAPSHOTS.active().revision());
        return stack;
    }

    public static boolean isSystemDefinitionId(ResourceLocation id) {
        return ModItems.TALENT_BOOK_ID.equals(id) || ModItems.TALENT_POINT_ID.equals(id);
    }

    static void applyDefinition(ItemStack stack, ConfiguredItemDefinition definition, long revision) {
        CompoundTag root = stack.getOrCreateTag();
        root.putString(DEFINITION_TAG, definition.id().toString());
        root.putBoolean(GLINT_TAG, definition.glint());
        root.putLong(REVISION_TAG, revision);
        if (definition.customModelData() > 0) {
            root.putInt("CustomModelData", definition.customModelData());
        } else root.remove("CustomModelData");
        stack.setHoverName(definition.name().component());
        CompoundTag display = stack.getOrCreateTagElement("display");
        if (!definition.description().isEmpty()) {
            ListTag lore = new ListTag();
            definition.description().forEach(line ->
                    lore.add(StringTag.valueOf(Component.Serializer.toJson(line.component()))));
            display.put("Lore", lore);
        } else display.remove("Lore");
        writeAttributeModifiers(root, definition);
    }

    public static Optional<ResourceLocation> definitionId(ItemStack stack) {
        if (stack.is(ModItems.TALENT_BOOK.get())) return Optional.of(ModItems.TALENT_BOOK_ID);
        if (stack.is(ModItems.TALENT_POINT.get())) return Optional.of(ModItems.TALENT_POINT_ID);
        if (!stack.is(ModItems.CONFIGURED_ITEM.get()) || !stack.hasTag()) return Optional.empty();
        return Optional.ofNullable(ResourceLocation.tryParse(stack.getTag().getString(DEFINITION_TAG)));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return configuredFoil(stack) || super.isFoil(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return configuredAttributeModifiers(super.getAttributeModifiers(slot, stack), slot, stack);
    }

    static boolean configuredFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(GLINT_TAG);
    }

    static Multimap<Attribute, AttributeModifier> configuredAttributeModifiers(
            Multimap<Attribute, AttributeModifier> base,
            EquipmentSlot slot,
            ItemStack stack
    ) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> result = ImmutableMultimap.builder();
        result.putAll(base);
        CompoundTag root = stack.getTag();
        if (root == null) return result.build();
        ListTag modifiers = root.getList(ATTRIBUTE_MODIFIERS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < modifiers.size(); index++) {
            CompoundTag modifier = modifiers.getCompound(index);
            if (!slot.getName().equals(modifier.getString("Slot"))) continue;
            ResourceLocation attributeId = ResourceLocation.tryParse(modifier.getString("Attribute"));
            Attribute attribute = attributeId == null ? null : ForgeRegistries.ATTRIBUTES.getValue(attributeId);
            if (attribute == null || !modifier.hasUUID("Id")) continue;
            AttributeModifier.Operation operation = operation(modifier.getString("Operation"));
            if (operation == null) continue;
            result.put(attribute, new AttributeModifier(
                    modifier.getUUID("Id"), modifier.getString("Name"), modifier.getDouble("Amount"), operation
            ));
        }
        return result.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return definitionId(stack).isPresent()
                    ? InteractionResultHolder.sidedSuccess(stack, true)
                    : InteractionResultHolder.pass(stack);
        }
        Optional<ResourceLocation> id = definitionId(stack);
        if (id.isEmpty() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        ConfiguredItemDefinition definition = TalentEditorMod.SNAPSHOTS.active().configuredItems().get(id.orElseThrow());
        return definition != null && ConfiguredItemUseRuntime.use(serverPlayer, stack, definition)
                ? InteractionResultHolder.sidedSuccess(stack, false)
                : InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide()) return;
        refreshDefinition(stack);
    }

    static void refreshDefinition(ItemStack stack) {
        CompoundTag root = stack.getTag();
        var snapshot = TalentEditorMod.SNAPSHOTS.active();
        if (root != null && root.getLong(REVISION_TAG) == snapshot.revision()) return;
        Optional<ConfiguredItemDefinition> definition = definitionId(stack).map(snapshot.configuredItems()::get);
        if (definition.isPresent()) {
            applyDefinition(stack, definition.orElseThrow(), snapshot.revision());
        } else if (root != null) {
            root.put(ATTRIBUTE_MODIFIERS_TAG, new ListTag());
            root.putLong(REVISION_TAG, snapshot.revision());
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (flag.isAdvanced()) {
            definitionId(stack).ifPresent(id -> tooltip.add(Component.literal("Definition: " + id)
                    .withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    private static void writeAttributeModifiers(CompoundTag root, ConfiguredItemDefinition definition) {
        ListTag list = new ListTag();
        if (definition.enabled() && definition.available()) {
            for (ConfiguredItemAttributeModifier configured : definition.attributeModifiers()) {
                CompoundTag modifier = new CompoundTag();
                modifier.putString("Attribute", configured.attribute().toString());
                modifier.putDouble("Amount", configured.amount());
                modifier.putString("Operation", configured.operation().name());
                modifier.putString("Slot", configured.slot().equipmentSlot().getName());
                modifier.putString("Name", TalentEditorMod.MOD_ID + ":" + definition.id()
                        + "/" + configured.modifierKey());
                modifier.putUUID("Id", ConfiguredItemModifierIds.create(
                        definition.id(), configured.modifierKey()
                ));
                list.add(modifier);
            }
        }
        root.put(ATTRIBUTE_MODIFIERS_TAG, list);
    }

    @Nullable
    private static AttributeModifier.Operation operation(String name) {
        try {
            return switch (AttributeOperation.parse(name)) {
                case ADDITION -> AttributeModifier.Operation.ADDITION;
                case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
                case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            };
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
