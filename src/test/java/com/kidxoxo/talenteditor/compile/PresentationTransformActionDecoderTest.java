package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.domain.PresentationTransformAction;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PresentationTransformActionDecoderTest {
    private final PresentationTransformActionDecoder decoder = new PresentationTransformActionDecoder();

    @Test void compilesBoundedFloatAndModelFade() {
        ValidationReport.Builder report = ValidationReport.builder();
        var action = (PresentationTransformAction) decoder.decode(context(report),
                JsonParser.parseString("""
                    {"type":"presentation_transform","atTick":30,"durationTicks":80,
                     "alphaFrom":0,"alphaTo":1,"scaleFrom":0.6,"scaleTo":1.4,"offsetY":2}
                    """).getAsJsonObject()).orElseThrow();
        assertFalse(report.build().hasErrors());
        assertEquals(30, action.atTick());
        assertEquals("return", action.positionMode());
        assertEquals(0, action.positionFactor(0), 1e-9);
        assertEquals(1, action.positionFactor(0.5), 1e-9);
        assertEquals(0, action.positionFactor(1), 1e-9);
        assertFalse(decoder.supportedPlacements().contains(ActionPlacement.TRIGGER));
    }

    @Test void rejectsExcessiveDisplacementAndDuration() {
        ValidationReport.Builder report = ValidationReport.builder();
        decoder.decode(context(report), JsonParser.parseString("""
                {"type":"presentation_transform","durationTicks":400,"offsetY":8}
                """).getAsJsonObject());
        assertTrue(report.build().hasErrors());
    }

    @Test void holdPositionReachesOffsetAndStaysThere() {
        ValidationReport.Builder report = ValidationReport.builder();
        var action = (PresentationTransformAction) decoder.decode(context(report),
                JsonParser.parseString("""
                    {"type":"presentation_transform","durationTicks":80,
                     "offsetY":3,"positionMode":"hold"}
                    """).getAsJsonObject()).orElseThrow();
        assertFalse(report.build().hasErrors());
        assertEquals(0, action.positionFactor(0), 1e-9);
        assertTrue(action.positionFactor(0.125) > 0);
        assertEquals(1, action.positionFactor(0.25), 1e-9);
        assertEquals(1, action.positionFactor(0.75), 1e-9);
        assertEquals(1, action.positionFactor(1), 1e-9);
    }

    @Test void rejectsUnknownPositionMode() {
        ValidationReport.Builder report = ValidationReport.builder();
        decoder.decode(context(report), JsonParser.parseString("""
                {"type":"presentation_transform","positionMode":"teleport"}
                """).getAsJsonObject());
        assertTrue(report.build().hasErrors());
    }

    private ActionDecodeContext context(ValidationReport.Builder report) {
        return new ActionDecodeContext(new ResourceLocation("example:talent"), "test", "onAcquire[0]",
                ActionPlacement.ON_ACQUIRE, new CompilationEnvironment(Set.of("minecraft"), Set.of(),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of()), List.of(), report);
    }
}
