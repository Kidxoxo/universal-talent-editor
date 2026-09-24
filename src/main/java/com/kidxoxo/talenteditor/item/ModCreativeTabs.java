package com.kidxoxo.talenteditor.item;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, new ResourceLocation(TalentEditorMod.MOD_ID, "items")
    );
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, TalentEditorMod.MOD_ID
    );
    public static final RegistryObject<CreativeModeTab> ITEMS = TABS.register("items", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.talent_editor"))
                    .icon(() -> new ItemStack(ModItems.TAB_ICON.get()))
                    .displayItems((parameters, output) -> {
                        // The connected server's item catalog is added when the tab contents are built.
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }
}
