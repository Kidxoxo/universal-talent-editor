package com.kidxoxo.talenteditor.forge;

import com.kidxoxo.talenteditor.compile.CompilationEnvironment;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;

public final class ForgeCompilationEnvironment {
    private ForgeCompilationEnvironment() {
    }

    public static CompilationEnvironment capture() {
        LinkedHashSet<String> mods = new LinkedHashSet<>();
        ModList.get().getMods().forEach(info -> mods.add(info.getModId()));
        return new CompilationEnvironment(
                mods,
                ForgeRegistries.ATTRIBUTES.getKeys(),
                ForgeRegistries.ITEMS.getKeys(),
                ForgeRegistries.ENTITY_TYPES.getKeys(),
                ForgeRegistries.MOB_EFFECTS.getKeys(),
                ForgeRegistries.BLOCKS.getKeys(),
                ForgeRegistries.SOUND_EVENTS.getKeys()
        );
    }
}
