package com.kidxoxo.talenteditor.resource;

import com.kidxoxo.talenteditor.domain.ActionFailurePolicy;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.domain.CompiledTrigger;
import com.kidxoxo.talenteditor.domain.ConfiguredItemDefinition;
import com.kidxoxo.talenteditor.domain.ConfiguredItemUseDefinition;
import com.kidxoxo.talenteditor.domain.LocalizedText;
import com.kidxoxo.talenteditor.domain.OfferSettings;
import com.kidxoxo.talenteditor.domain.PlaySoundAction;
import com.kidxoxo.talenteditor.domain.SynthesisDefinition;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientResourceRequirementCatalogTest {
    @Test
    void includesNormalAndHoverImagesFromBothUiDocuments() {
        var book = com.kidxoxo.talenteditor.compile.UiDocumentCodec.read("""
                {"elements":[{"id":"slot","type":"slot","texture":"test:textures/book.png","hoverTexture":"test:textures/hover.png"}]}
                """, false);
        var draw = new com.google.gson.JsonObject();var elements = new com.google.gson.JsonArray();draw.add("elements", elements);
        for (int i=0;i<5;i++) elements.add(com.google.gson.JsonParser.parseString(
                "{\"id\":\"o"+i+"\",\"type\":\"offer\",\"index\":"+i+",\"texture\":\"test:textures/draw.png\",\"hoverTexture\":\"test:textures/hover.png\"}"));
        var layout = new TalentBookLayoutDefinition(id("test:ui"),2,true,10,Optional.empty(),60,2,3,120,56,48,true,
                List.of(),List.of(),Optional.of(book),Optional.of(com.kidxoxo.talenteditor.compile.UiDocumentCodec.read(draw.toString(),true)));
        var snapshot = RuntimeSnapshot.candidate("0".repeat(64),Map.of(),Map.of(),Map.of(),Map.of(),Map.of(layout.id(),layout),Map.of());
        var result = ClientResourceRequirementCatalog.from(snapshot);
        assertEquals(3,result.textureCount());
        assertEquals(List.of(requirement(ClientResourceRequirement.Kind.TEXTURE,"test:textures/book.png"),
                requirement(ClientResourceRequirement.Kind.TEXTURE,"test:textures/draw.png"),
                requirement(ClientResourceRequirement.Kind.TEXTURE,"test:textures/hover.png")),result.requirements());
    }

    @Test
    void derivesDistinctAssetsFromActiveDefinitionsAndLayout() {
        TalentDefinition active = talent(true, true);
        TalentDefinition disabled = new TalentDefinition(
                id("example:disabled"), active.schema(), false, active.name(), active.description(),
                active.showGeneratedEffects(), active.rarity(), active.categories(),
                Optional.of(id("example:textures/disabled.png")), active.offer(), active.capacityCost(),
                active.tags(), active.requiredMods(), true, List.of(), active.acquireCondition(),
                active.synthesis(), active.activeEffects(), active.triggers(), active.onAcquire(),
                active.onSynthesize(), active.onRemove()
        );
        TalentBookLayoutDefinition layout = new TalentBookLayoutDefinition(
                id("example:layout"), 2, true, 10,
                Optional.of(id("example:textures/gui/book.png")),
                42, 2, 4, 58, 30, 16, true, List.of(),
                List.of(new TalentBookLayoutDefinition.PageDefinition(
                        "second", Optional.of(id("example:textures/gui/page2.png")), List.of()
                ))
        );
        ConfiguredItemDefinition item = new ConfiguredItemDefinition(
                id("example:token"), 1, true, text("Token"), List.of(), 0, false, List.of(),
                new ConfiguredItemUseDefinition(true, 0, 0, List.of(sound("example:item.use"))),
                List.of(), List.of(), true, List.of()
        );
        RuntimeSnapshot snapshot = RuntimeSnapshot.candidate(
                "0".repeat(64), Map.of(), Map.of(), Map.of(active.id(), active, disabled.id(), disabled),
                Map.of(), Map.of(layout.id(), layout), Map.of(item.id(), item)
        );

        ClientResourceRequirementCatalog.Result result = ClientResourceRequirementCatalog.from(snapshot);

        assertFalse(result.truncated());
        assertEquals(3, result.textureCount());
        assertEquals(4, result.soundCount());
        assertEquals(List.of(
                requirement(ClientResourceRequirement.Kind.TEXTURE, "example:textures/gui/book.png"),
                requirement(ClientResourceRequirement.Kind.TEXTURE, "example:textures/gui/page2.png"),
                requirement(ClientResourceRequirement.Kind.TEXTURE, "example:textures/talent_icons/hero.png"),
                requirement(ClientResourceRequirement.Kind.SOUND, "example:item.use"),
                requirement(ClientResourceRequirement.Kind.SOUND, "example:talent.acquire"),
                requirement(ClientResourceRequirement.Kind.SOUND, "example:talent.fail"),
                requirement(ClientResourceRequirement.Kind.SOUND, "example:talent.trigger")
        ), result.requirements());
        assertTrue(result.requirements().stream().noneMatch(value ->
                value.id().equals(id("example:textures/disabled.png"))));
    }

    private static TalentDefinition talent(boolean enabled, boolean available) {
        CompiledTrigger trigger = new CompiledTrigger(
                "sound", id("talent_editor:player_second"), 0, 1.0D,
                Optional.empty(), Optional.empty(), Optional.empty(), 0.0D, 1.0D, 0, 0,
                new CompiledCondition.Always(true), List.of(sound("example:talent.trigger")),
                ActionFailurePolicy.CONTINUE, List.of(sound("example:talent.fail"))
        );
        return new TalentDefinition(
                id("example:hero"), 2, enabled, text("Hero"), List.of(), true,
                id("talent_editor:common"), List.of(),
                Optional.of(id("example:textures/talent_icons/hero.png")), OfferSettings.disabled(),
                1, List.of(), List.of(), available, List.of(), new CompiledCondition.Always(true),
                SynthesisDefinition.disabled(), List.of(), List.of(trigger),
                List.of(sound("example:talent.acquire"), sound("example:talent.acquire")),
                List.of(), List.of()
        );
    }

    private static PlaySoundAction sound(String id) {
        return new PlaySoundAction(id(id), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static LocalizedText text(String text) {
        return new LocalizedText(text, "", null, false, false);
    }

    private static ClientResourceRequirement requirement(ClientResourceRequirement.Kind kind, String id) {
        return new ClientResourceRequirement(kind, id(id));
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
