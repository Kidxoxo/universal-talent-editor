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
    public static final ResourceLocation LEVEL_UP = id("level_up");
    public static final ResourceLocation DRAW_SELECTED = id("draw_selected");
    public static final ResourceLocation DRAW_REFRESHED = id("draw_refreshed");
    public static final ResourceLocation COUNTER_CHANGED = id("counter_changed");
    public static final ResourceLocation FATAL_DAMAGE = id("fatal_damage");
    public static final ResourceLocation NEARBY_ENTITY_DEATH = id("nearby_entity_death");
    public static final ResourceLocation ITEM_SUBMITTED = id("item_submitted");

    private TalentEventTypes() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(TalentEditorMod.MOD_ID, path);
    }
}
