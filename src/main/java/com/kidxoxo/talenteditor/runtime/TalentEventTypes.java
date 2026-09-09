package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

public final class TalentEventTypes {
    public static final ResourceLocation DAMAGE_DEALT_PRE = id("damage_dealt_pre");
    public static final ResourceLocation DAMAGE_TAKEN_PRE = id("damage_taken_pre");
    public static final ResourceLocation DAMAGE_DEALT = id("damage_dealt");
    public static final ResourceLocation DAMAGE_TAKEN = id("damage_taken");
    public static final ResourceLocation INTERACT_ENTITY = id("interact_entity");
    public static final ResourceLocation KILL = id("kill");
    public static final ResourceLocation DEATH = id("death");
    public static final ResourceLocation ITEM_USE_FINISH = id("item_use_finish");
    public static final ResourceLocation INTERACT_BLOCK = id("interact_block");
    public static final ResourceLocation PLAYER_TICK = id("player_tick");
    public static final ResourceLocation PLAYER_SECOND = id("player_second");
    public static final ResourceLocation PLAYER_MOVE_DISTANCE = id("player_move_distance");

    private TalentEventTypes() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(TalentEditorMod.MOD_ID, path);
    }
}
