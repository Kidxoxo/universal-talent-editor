package com.kidxoxo.talenteditor.item;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class ConfiguredItemModifierIds {
    private ConfiguredItemModifierIds() {
    }

    static UUID create(ResourceLocation item, String modifierKey) {
        String seed = TalentEditorMod.MOD_ID + "|configured_item|" + item + "|" + modifierKey;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
