package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.PlayerTalentState;
import com.kidxoxo.talenteditor.network.PlayerVisualStatePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientVisualState {
    private static final Map<UUID, PlayerTalentState.RenderView> STATES = new HashMap<>();
    private static boolean alphaOverrides;
    private ClientVisualState() { }
    public static void accept(PlayerVisualStatePacket packet) {
        if (packet.render().equals(PlayerTalentState.RenderView.DEFAULT)) STATES.remove(packet.player());
        else STATES.put(packet.player(), packet.render());
        refreshAlphaOverrides();
    }
    public static PlayerTalentState.RenderView get(UUID player) {
        return STATES.getOrDefault(player, PlayerTalentState.RenderView.DEFAULT);
    }

    /**
     * {@return whether at least one tracked player currently needs a translucent model}
     *
     * <p>Used to keep the render mixin from allocating a buffer wrapper for every living entity in
     * the world when no talent is changing anything.</p>
     */
    public static boolean hasAlphaOverrides() {
        return alphaOverrides;
    }

    public static void clear() {
        STATES.clear();
        alphaOverrides = false;
    }

    private static void refreshAlphaOverrides() {
        boolean any = false;
        for (PlayerTalentState.RenderView view : STATES.values()) {
            if (view.alpha() < 0.999D) {
                any = true;
                break;
            }
        }
        alphaOverrides = any;
    }
}
