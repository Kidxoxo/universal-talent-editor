package com.kidxoxo.talenteditor.compat;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.ConfiguredItemSlot;
import com.kidxoxo.talenteditor.item.ConfiguredItemModifierIds;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Reflection-only Curios bridge: the editor has no hard Curios dependency. */
public final class CuriosIntegration {
    private static volatile Method inventoryMethod;
    private static volatile boolean lookupAttempted;
    private CuriosIntegration() { }

    public static boolean installed() { return ModList.get().isLoaded("curios"); }

    /** Register a stack-aware Curios adapter for the shared configured-item carrier. */
    public static void registerConfiguredItem() {
        if (!installed()) return;
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Class<?> contract = Class.forName("top.theillusivec4.curios.api.type.capability.ICurioItem");
            Object handler = Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[] {contract},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "TalentEditorConfiguredCurio";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> null;
                            };
                        }
                        if ((method.getName().equals("canEquip")
                                || method.getName().equals("canEquipFromUse")) && args != null) {
                            String slot = args.length == 0 ? null : slotIdentifier(args[0]);
                            for (Object arg : args) {
                                if (arg instanceof ItemStack stack && !curiosEquippable(stack, slot)) return false;
                            }
                        }
                        if ((method.getName().equals("getSlotsTooltip")
                                || method.getName().equals("getTagsTooltip")) && args != null) {
                            for (Object arg : args) {
                                if (arg instanceof ItemStack stack
                                        && com.kidxoxo.talenteditor.item.ConfiguredItem.definitionId(stack)
                                        .isPresent()) return new ArrayList<>();
                            }
                        }
                        Object value = method.isDefault() ? InvocationHandler.invokeDefault(
                                proxy, method, args == null ? new Object[0] : args) : null;
                        if (method.getName().equals("getAttributeModifiers") && args != null
                                && args.length == 3 && args[2] instanceof ItemStack stack
                                && value instanceof Multimap<?, ?> base) {
                            return withCuriosAttributes(base, stack, slotIdentifier(args[0]));
                        }
                        return value;
                    });
            api.getMethod("registerCurio", net.minecraft.world.item.Item.class, contract)
                    .invoke(null, com.kidxoxo.talenteditor.item.ModItems.CONFIGURED_ITEM.get(), handler);
            TalentEditorMod.LOGGER.info("Registered configured-item Curios adapter");
        } catch (ReflectiveOperationException | LinkageError exception) {
            TalentEditorMod.LOGGER.warn("Curios configured-item registration unavailable", exception);
        }
    }

    private static String slotIdentifier(Object context) {
        if (context instanceof String slot) return slot;
        if (context == null) return null;
        try {
            Object value = context.getClass().getMethod("identifier").invoke(context);
            return value instanceof String slot ? slot : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static boolean curiosEquippable(ItemStack stack, String slot) {
        return com.kidxoxo.talenteditor.item.ConfiguredItem.definitionId(stack)
                .map(id -> TalentEditorMod.SNAPSHOTS.active().configuredItems().get(id))
                .map(definition -> definition.allowsCuriosSlot(slot))
                .orElse(true);
    }

    /** Slot types actually assigned to this player, including server-synced third-party types. */
    public static List<String> availablePlayerSlots(Player player) {
        if (!installed() || player == null) return List.of();
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object value = api.getMethod("getPlayerSlots", Player.class).invoke(null, player);
            if (value instanceof Map<?, ?> slots) {
                return slots.keySet().stream().filter(String.class::isInstance)
                        .map(String.class::cast).sorted().toList();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            TalentEditorMod.LOGGER.debug("Curios player slots unavailable", exception);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Multimap<Attribute, AttributeModifier> withCuriosAttributes(
            Multimap<?, ?> base, ItemStack stack, String slot
    ) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> result = ImmutableMultimap.builder();
        result.putAll((Multimap<Attribute, AttributeModifier>) base);
        if (!curiosEquippable(stack, slot)) return result.build();
        com.kidxoxo.talenteditor.item.ConfiguredItem.definitionId(stack).ifPresent(id -> {
            var definition = TalentEditorMod.SNAPSHOTS.active().configuredItems().get(id);
            if (definition == null || !definition.enabled() || !definition.available()) return;
            definition.attributeModifiers().stream()
                    .filter(modifier -> modifier.slots().contains(ConfiguredItemSlot.CURIOS))
                    .forEach(modifier -> {
                        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(modifier.attribute());
                        if (attribute == null) return;
                        AttributeModifier.Operation operation = switch (modifier.operation()) {
                            case ADDITION -> AttributeModifier.Operation.ADDITION;
                            case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
                            case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                        };
                        result.put(attribute, new AttributeModifier(
                                ConfiguredItemModifierIds.create(id,
                                        modifier.modifierIdentity(ConfiguredItemSlot.CURIOS)),
                                TalentEditorMod.MOD_ID + ":" + id + "/" + modifier.modifierKey(),
                                modifier.amount(), operation));
                    });
        });
        return result.build();
    }

    public static boolean hasEquipped(ServerPlayer player, ResourceLocation itemId) {
        if (!installed()) return false;
        if (TalentEditorMod.SNAPSHOTS.active().configuredItems().containsKey(itemId)) {
            return equippedLogicalIds(player).contains(itemId);
        }
        var item = ForgeRegistries.ITEMS.getValue(itemId);
        try {
            Method method = inventoryMethod();
            if (method == null) return false;
            Object lazy = method.invoke(null, player);
            Object resolved = lazy.getClass().getMethod("resolve").invoke(lazy);
            if (!(resolved instanceof Optional<?> optional) || optional.isEmpty()) return false;
            Object handler = optional.orElseThrow();
            Method find = handler.getClass().getMethod("findFirstCurio", Predicate.class);
            Predicate<ItemStack> predicate = stack -> com.kidxoxo.talenteditor.item.ConfiguredItem.definitionId(stack)
                    .map(itemId::equals).orElseGet(() -> item != null && stack.is(item));
            Object result = find.invoke(handler, predicate);
            return result instanceof Optional<?> found && found.isPresent();
        } catch (ReflectiveOperationException | LinkageError exception) {
            TalentEditorMod.LOGGER.debug("Curios equipment query unavailable", exception);
            return false;
        }
    }

    public static Set<ResourceLocation> equippedLogicalIds(ServerPlayer player) {
        if (!installed()) return Set.of();
        try {
            Method method = inventoryMethod();
            if (method == null) return Set.of();
            Object lazy = method.invoke(null, player);
            Object resolved = lazy.getClass().getMethod("resolve").invoke(lazy);
            if (!(resolved instanceof Optional<?> optional) || optional.isEmpty()) return Set.of();
            Object handler = optional.orElseThrow();
            LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
            Object slots = handler.getClass().getMethod("getCurios").invoke(handler);
            if (!(slots instanceof Map<?, ?> handlers)) return Set.of();
            for (var entry : handlers.entrySet()) {
                if (!(entry.getKey() instanceof String slot)) continue;
                Object stacks = entry.getValue().getClass().getMethod("getStacks").invoke(entry.getValue());
                Method getSlots = stacks.getClass().getMethod("getSlots");
                Method getStack = stacks.getClass().getMethod("getStackInSlot", int.class);
                int count = (int) getSlots.invoke(stacks);
                for (int index = 0; index < count; index++) {
                    Object value = getStack.invoke(stacks, index);
                    if (value instanceof ItemStack stack && curiosEquippable(stack, slot)) {
                        com.kidxoxo.talenteditor.item.ConfiguredItem.definitionId(stack).ifPresent(result::add);
                    }
                }
            }
            return Set.copyOf(result);
        } catch (ReflectiveOperationException | LinkageError exception) {
            TalentEditorMod.LOGGER.debug("Curios equipped-item enumeration unavailable", exception);
            return Set.of();
        }
    }

    private static Method inventoryMethod() {
        if (lookupAttempted) return inventoryMethod;
        synchronized (CuriosIntegration.class) {
            if (lookupAttempted) return inventoryMethod;
            lookupAttempted = true;
            try {
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                inventoryMethod = api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            } catch (ReflectiveOperationException | LinkageError exception) {
                TalentEditorMod.LOGGER.warn("Curios is loaded but its inventory API could not be linked");
            }
            return inventoryMethod;
        }
    }
}
