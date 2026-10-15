package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.domain.PlaySoundAction;
import com.kidxoxo.talenteditor.domain.ShowMessageAction;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PresentationCueDecoderTest {
    private final CompilationEnvironment environment = new CompilationEnvironment(Set.of("minecraft"),
            Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());

    @Test void soundAndChatUseIndependentTimelineOffsets() {
        ValidationReport.Builder report = ValidationReport.builder();
        var sound = (PlaySoundAction) new PlaySoundActionDecoder().decode(context(report, ActionPlacement.ON_ACQUIRE),
                JsonParser.parseString("""
                    {"type":"play_sound","sound":"minecraft:entity.player.levelup","atTick":40}
                    """).getAsJsonObject()).orElseThrow();
        var message = (ShowMessageAction) new ShowMessageActionDecoder().decode(
                context(report, ActionPlacement.ON_SYNTHESIZE), JsonParser.parseString("""
                    {"type":"show_message","mode":"chat","text":{"literal":"The world awakens"},
                     "atTick":180}
                    """).getAsJsonObject()).orElseThrow();
        assertFalse(report.build().hasErrors());
        assertEquals(40, sound.atTick());
        assertEquals(180, message.atTick());
    }

    @Test void rejectsTimelineOffsetsOutsideLifecycle() {
        ValidationReport.Builder report = ValidationReport.builder();
        new ShowMessageActionDecoder().decode(context(report, ActionPlacement.TRIGGER),
                JsonParser.parseString("""
                    {"type":"show_message","mode":"chat","text":{"literal":"Now"},"atTick":20}
                    """).getAsJsonObject());
        assertTrue(report.build().issues().stream()
                .anyMatch(issue -> issue.code().equals("PRESENTATION_TIMELINE_PLACEMENT")));
    }

    @Test void chatHoldAndLineBreaksSurviveCompilation() {
        ValidationReport.Builder report = ValidationReport.builder();
        var json = new com.google.gson.JsonObject();
        json.addProperty("type", "show_message");
        json.addProperty("text", "First\nSecond");
        json.addProperty("stayTicks", 1960);
        json.addProperty("atTick", 520);
        var action = (ShowMessageAction) new ShowMessageActionDecoder().decode(
                context(report, ActionPlacement.ON_ACQUIRE), json).orElseThrow();
        assertFalse(report.build().hasErrors());
        assertEquals(1960, action.stayTicks());
        assertEquals("First\nSecond", action.text().fallbackText());
        assertEquals(520, action.atTick());
    }

    private ActionDecodeContext context(ValidationReport.Builder report, ActionPlacement placement) {
        return new ActionDecodeContext(new ResourceLocation("example:talent"), "test", "onAcquire[0]",
                placement, environment, List.of(), report);
    }
}
