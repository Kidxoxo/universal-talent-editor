package com.kidxoxo.talenteditor.loot;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTER =
            DeferredRegister.create(
                    ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    TalentEditorMod.MOD_ID
            );

    public static final RegistryObject<Codec<ConfiguredItemLootModifier>> CONFIGURED_ITEMS =
            REGISTER.register("configured_items", ConfiguredItemLootModifier.CODEC);

    private ModLootModifiers() {
    }
}
