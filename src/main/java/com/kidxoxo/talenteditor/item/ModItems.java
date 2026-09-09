package com.kidxoxo.talenteditor.item;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final ResourceLocation TALENT_BOOK_ID = new ResourceLocation(TalentEditorMod.MOD_ID, "talent_book");
    public static final ResourceLocation TALENT_POINT_ID = new ResourceLocation(TalentEditorMod.MOD_ID, "talent_point");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TalentEditorMod.MOD_ID);
    public static final RegistryObject<Item> TALENT_BOOK = ITEMS.register(
            "talent_book", () -> new TalentBookItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> CONFIGURED_ITEM = ITEMS.register(
            "configured_item", () -> new ConfiguredItem(new Item.Properties().stacksTo(64))
    );
    public static final RegistryObject<Item> TALENT_POINT = ITEMS.register(
            "talent_point", () -> new TalentPointItem(new Item.Properties().stacksTo(64))
    );

    private ModItems() {
    }
}
