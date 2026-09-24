package com.kidxoxo.talenteditor.compat;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Reflection-only Curios bridge: the editor has no hard Curios dependency. */
public final class CuriosIntegration {
    private static volatile Method inventoryMethod;
    private static volatile boolean lookupAttempted;
    private CuriosIntegration() { }

    public static boolean installed() { return ModList.get().isLoaded("curios"); }

    public static boolean hasEquipped(ServerPlayer player, ResourceLocation itemId) {
        if (!installed()) return false;
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
            Object stacks = handler.getClass().getMethod("getEquippedCurios").invoke(handler);
            Method getSlots = stacks.getClass().getMethod("getSlots");
            Method getStack = stacks.getClass().getMethod("getStackInSlot", int.class);
            int slots = (int) getSlots.invoke(stacks);
            LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
            for (int index = 0; index < slots; index++) {
                Object value = getStack.invoke(stacks, index);
                if (value instanceof ItemStack stack) {
                    com.kidxoxo.talenteditor.item.ConfiguredItem.definitionId(stack).ifPresent(result::add);
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
