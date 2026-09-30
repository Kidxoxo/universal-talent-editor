package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.domain.SpawnParticlesAction;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpawnParticlesActionDecoderTest {
    private final SpawnParticlesActionDecoder decoder = new SpawnParticlesActionDecoder();
    private final ResourceLocation endRod = new ResourceLocation("minecraft:end_rod");

    @Test void acceptsRegisteredSimpleParticleWithBoundedAnimation() {
        ValidationReport.Builder report = ValidationReport.builder();
        var action = (SpawnParticlesAction) decoder.decode(context(report, Set.of(endRod)),
                JsonParser.parseString("""
                    {"type":"spawn_particles","particle":"minecraft:end_rod","motion":"inward",
                     "count":12,"radius":1.5,"speed":0.08,"durationTicks":12,"intervalTicks":3}
                    """).getAsJsonObject()).orElseThrow();
        assertFalse(report.build().hasErrors());
        assertEquals("inward", action.motion());
        assertEquals(12, action.count());
        assertTrue(decoder.supportedPlacements().contains(ActionPlacement.ON_SYNTHESIZE));
    }

    @Test void rejectsResourceOnlyOrMissingParticleAndUnboundedDensity() {
        ValidationReport.Builder report = ValidationReport.builder();
        decoder.decode(context(report, Set.of()), JsonParser.parseString("""
                {"type":"spawn_particles","particle":"missing:resource_only",
                 "count":32,"durationTicks":60,"intervalTicks":1}
                """).getAsJsonObject());
        var codes = report.build().issues().stream().map(issue -> issue.code()).toList();
        assertTrue(codes.contains("UNREGISTERED_PARTICLE"));
        assertTrue(codes.contains("PARTICLE_BUDGET"));
    }

    @Test void acceptsPreviewCameraDistanceWithoutChangingRuntimeAction() {
        ValidationReport.Builder report = ValidationReport.builder();
        var action = (SpawnParticlesAction) decoder.decode(context(report, Set.of(endRod)),
                JsonParser.parseString("""
                    {"type":"spawn_particles","particle":"minecraft:end_rod",
                     "viewDistance":24,"previewCameraDistance":12}
                    """).getAsJsonObject()).orElseThrow();
        assertFalse(report.build().hasErrors());
        assertEquals(24, action.viewDistance());
    }

    @Test void rejectsPreviewCameraDistanceOutsideEditorRange() {
        ValidationReport.Builder report = ValidationReport.builder();
        decoder.decode(context(report, Set.of(endRod)), JsonParser.parseString("""
                {"type":"spawn_particles","particle":"minecraft:end_rod",
                 "previewCameraDistance":32}
                """).getAsJsonObject());
        assertTrue(report.build().hasErrors());
    }

    @Test void compilesTimedSpiralWithOpacityRamp() {
        ValidationReport.Builder report = ValidationReport.builder();
        var action = (SpawnParticlesAction) decoder.decode(context(report, Set.of(endRod)),
                JsonParser.parseString("""
                    {"type":"spawn_particles","particle":"minecraft:end_rod",
                     "motion":"spiral","atTick":120,"alphaFrom":0,"alphaTo":1,
                     "durationTicks":40,"intervalTicks":4}
                    """).getAsJsonObject()).orElseThrow();
        assertFalse(report.build().hasErrors());
        assertEquals(120, action.atTick());
        assertEquals("spiral", action.motion());
        assertEquals(0, action.alphaFrom());
        assertEquals(1, action.alphaTo());
    }

    @Test void preventsDelayedTriggerParticleAction() {
        ValidationReport.Builder report = ValidationReport.builder();
        ActionDecodeContext trigger = new ActionDecodeContext(new ResourceLocation("example:talent"),
                "test", "triggers[0].actions[0]", ActionPlacement.TRIGGER,
                new CompilationEnvironment(Set.of("minecraft"), Set.of(), Set.of(), Set.of(),
                        Set.of(), Set.of(), Set.of(), Set.of(endRod)), java.util.List.of(), report);
        decoder.decode(trigger, JsonParser.parseString("""
                {"type":"spawn_particles","particle":"minecraft:end_rod","atTick":20}
                """).getAsJsonObject());
        assertTrue(report.build().issues().stream()
                .anyMatch(issue -> issue.code().equals("PRESENTATION_TIMELINE_PLACEMENT")));
    }

    @Test void acceptsColorGradientAndHueShiftForRegisteredParticle() {
        ValidationReport.Builder report = ValidationReport.builder();
        var action = (SpawnParticlesAction) decoder.decode(context(report, Set.of(endRod)),
                JsonParser.parseString("""
                    {"type":"spawn_particles","particle":"minecraft:end_rod",
                     "colorFrom":"#FF0044","colorTo":"#AA22FF","hueShiftDegrees":45,
                     "sizeFrom":0.5,"sizeTo":2.5}
                    """).getAsJsonObject()).orElseThrow();
        assertFalse(report.build().hasErrors());
        assertEquals(0xFF0044, action.colorFrom());
        assertEquals(0xAA22FF, action.colorTo());
        assertEquals(45, action.hueShiftDegrees());
        assertEquals(0.5, action.sizeFrom());
        assertEquals(2.5, action.sizeTo());
    }

    @Test void rejectsMalformedParticleColor() {
        ValidationReport.Builder report = ValidationReport.builder();
        decoder.decode(context(report, Set.of(endRod)), JsonParser.parseString("""
                {"type":"spawn_particles","particle":"minecraft:end_rod","colorFrom":"red"}
                """).getAsJsonObject());
        assertTrue(report.build().issues().stream().anyMatch(issue -> issue.code().equals("INVALID_PARTICLE_COLOR")));
    }

    @Test void rejectsParticleSizeOutsideSafeRange() {
        ValidationReport.Builder report = ValidationReport.builder();
        decoder.decode(context(report, Set.of(endRod)), JsonParser.parseString("""
                {"type":"spawn_particles","particle":"minecraft:end_rod",
                 "sizeFrom":0,"sizeTo":5}
                """).getAsJsonObject());
        assertTrue(report.build().hasErrors());
    }

    private ActionDecodeContext context(ValidationReport.Builder report, Set<ResourceLocation> particles) {
        return new ActionDecodeContext(new ResourceLocation("example:talent"), "test", "onAcquire[0]",
                ActionPlacement.ON_ACQUIRE, new CompilationEnvironment(Set.of("minecraft"), Set.of(),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), particles), java.util.List.of(), report);
    }
}
