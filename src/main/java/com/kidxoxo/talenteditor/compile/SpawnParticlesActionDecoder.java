package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.SpawnParticlesAction;
import com.kidxoxo.talenteditor.domain.ParticleTint;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class SpawnParticlesActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "particle", "anchor", "motion", "count",
            "radius", "speed", "durationTicks", "intervalTicks", "viewDistance", "cooldownTicks",
            "previewCameraDistance", "atTick", "alphaFrom", "alphaTo",
            "colorFrom", "colorTo", "hueShiftDegrees", "sizeFrom", "sizeTo");

    @Override public ResourceLocation id() { return SpawnParticlesAction.TYPE; }

    @Override public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE,
                ActionPlacement.ON_SYNTHESIZE, ActionPlacement.ON_REMOVE);
    }

    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation particle = JsonAccess.requiredId(json, "particle", "minecraft:end_rod", true,
                context.source(), context.path(), context.report());
        if (!context.environment().isModLoaded(particle.getNamespace())) {
            context.report().error("MISSING_PARTICLE_MOD", context.source(),
                    JsonAccess.child(context.path(), "particle"),
                    "The particle's mod is not loaded: " + particle.getNamespace());
        }
        if (!context.environment().hasParticle(particle)) {
            context.report().error("UNREGISTERED_PARTICLE", context.source(),
                    JsonAccess.child(context.path(), "particle"),
                    "Particle is not a registered simple particle type on the server: " + particle);
        }
        String anchor = JsonAccess.string(json, "anchor", "body", false,
                context.source(), context.path(), context.report());
        String motion = JsonAccess.string(json, "motion", "burst", false,
                context.source(), context.path(), context.report());
        if (!Set.of("feet", "body", "head").contains(anchor)) context.report().error(
                "INVALID_PARTICLE_ANCHOR", context.source(), JsonAccess.child(context.path(), "anchor"),
                "Expected feet, body, or head");
        if (!Set.of("burst", "inward", "outward", "spiral", "ring", "fountain").contains(motion)) context.report().error(
                "INVALID_PARTICLE_MOTION", context.source(), JsonAccess.child(context.path(), "motion"),
                "Expected burst, inward, outward, spiral, ring, or fountain");
        int count = JsonAccess.integer(json, "count", 8, 1, 32, context.source(), context.path(), context.report());
        double radius = JsonAccess.decimal(json, "radius", 0.7, 0, 4,
                context.source(), context.path(), context.report());
        double speed = JsonAccess.decimal(json, "speed", 0.03, 0, 0.3,
                context.source(), context.path(), context.report());
        int duration = JsonAccess.integer(json, "durationTicks", 1, 1, 60,
                context.source(), context.path(), context.report());
        int interval = JsonAccess.integer(json, "intervalTicks", 4, 1, 20,
                context.source(), context.path(), context.report());
        double viewDistance = JsonAccess.decimal(json, "viewDistance", 24, 4, 64,
                context.source(), context.path(), context.report());
        // Preview-only metadata is kept in the action JSON so the editor remembers it per action.
        JsonAccess.decimal(json, "previewCameraDistance", 4, 2, 16,
                context.source(), context.path(), context.report());
        int cooldown = JsonAccess.integer(json, "cooldownTicks", 10, 0, 1200,
                context.source(), context.path(), context.report());
        int atTick = JsonAccess.integer(json, "atTick", 0, 0, 2400,
                context.source(), context.path(), context.report());
        double alphaFrom = JsonAccess.decimal(json, "alphaFrom", 1, 0, 1,
                context.source(), context.path(), context.report());
        double alphaTo = JsonAccess.decimal(json, "alphaTo", 1, 0, 1,
                context.source(), context.path(), context.report());
        String rawColorFrom = JsonAccess.string(json, "colorFrom", "", false,
                context.source(), context.path(), context.report());
        String rawColorTo = JsonAccess.string(json, "colorTo", "", false,
                context.source(), context.path(), context.report());
        int colorFrom = parseColor(context, rawColorFrom, "colorFrom");
        int colorTo = parseColor(context, rawColorTo, "colorTo");
        double hueShift = JsonAccess.decimal(json, "hueShiftDegrees", 0, -180, 180,
                context.source(), context.path(), context.report());
        double sizeFrom = JsonAccess.decimal(json, "sizeFrom", 1, 0.1, 4,
                context.source(), context.path(), context.report());
        double sizeTo = JsonAccess.decimal(json, "sizeTo", 1, 0.1, 4,
                context.source(), context.path(), context.report());
        if (atTick > 0 && context.placement() != ActionPlacement.ON_ACQUIRE
                && context.placement() != ActionPlacement.ON_SYNTHESIZE) context.report().error(
                "PRESENTATION_TIMELINE_PLACEMENT", context.source(), JsonAccess.child(context.path(), "atTick"),
                "atTick is only supported for acquisition and synthesis actions");
        if ((long) count * (1 + (duration - 1) / interval) > 256) context.report().error(
                "PARTICLE_BUDGET", context.source(), context.path(), "Animation exceeds 256 particles");
        return Optional.of(new SpawnParticlesAction(particle, anchor, motion, count, radius, speed,
                duration, interval, viewDistance, cooldown, atTick, alphaFrom, alphaTo,
                colorFrom, colorTo, (float) hueShift, sizeFrom, sizeTo));
    }

    private static int parseColor(ActionDecodeContext context, String raw, String field) {
        try { return ParticleTint.parse(raw); }
        catch (IllegalArgumentException exception) {
            context.report().error("INVALID_PARTICLE_COLOR", context.source(),
                    JsonAccess.child(context.path(), field), "Expected #RRGGBB");
            return -1;
        }
    }
}
