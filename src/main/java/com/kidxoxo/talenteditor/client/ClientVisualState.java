package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.PlayerTalentState;
import com.kidxoxo.talenteditor.network.PlayerVisualStatePacket;
import com.kidxoxo.talenteditor.network.PresentationTransformPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientVisualState {
    private static final Map<UUID, PlayerTalentState.RenderView> STATES = new HashMap<>();
    private static final Map<UUID, PresentationPose> PRESENTATIONS = new HashMap<>();
    private static boolean alphaOverrides;
    private ClientVisualState() { }
    public static void accept(PlayerVisualStatePacket packet) {
        if (packet.render().equals(PlayerTalentState.RenderView.DEFAULT)) STATES.remove(packet.player());
        else STATES.put(packet.player(), packet.render());
        refreshAlphaOverrides();
    }
    public static PlayerTalentState.RenderView get(UUID player) {
        PlayerTalentState.RenderView base = STATES.getOrDefault(player, PlayerTalentState.RenderView.DEFAULT);
        PresentationPose pose = PRESENTATIONS.get(player);
        return pose == null ? base : new PlayerTalentState.RenderView(
                base.alpha() * pose.alpha(), base.scale() * pose.scale(), base.hideNameplate());
    }

    public static PresentationPose presentation(UUID player) { return PRESENTATIONS.get(player); }

    public static void acceptPresentation(PresentationTransformPacket packet) {
        if (packet.clear()) clearPresentation(packet.player());
        else setPresentation(packet.player(), packet.alpha(), packet.scale(),
                packet.offsetX(), packet.offsetY(), packet.offsetZ());
    }

    public static void setPresentation(UUID player, double alpha, double scale,
                                       double x, double y, double z) {
        if (!Double.isFinite(alpha) || alpha < 0 || alpha > 1 || !Double.isFinite(scale) || scale < 0.1
                || scale > 4 || !Double.isFinite(x) || Math.abs(x) > 3 || !Double.isFinite(y)
                || Math.abs(y) > 3 || !Double.isFinite(z) || Math.abs(z) > 3) return;
        PRESENTATIONS.put(player, new PresentationPose(alpha, scale, x, y, z));
        refreshAlphaOverrides();
    }

    public static void clearPresentation(UUID player) {
        PRESENTATIONS.remove(player);
        refreshAlphaOverrides();
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
        PRESENTATIONS.clear();
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
        if (!any) for (PresentationPose pose : PRESENTATIONS.values()) {
            if (pose.alpha() < 0.999D) { any = true; break; }
        }
        alphaOverrides = any;
    }

    public record PresentationPose(double alpha, double scale, double x, double y, double z) {}
}
