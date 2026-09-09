package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.ValidationResultPacket;
import com.kidxoxo.talenteditor.network.PlayerTalentState;
import com.kidxoxo.talenteditor.network.DrawSessionView;
import com.kidxoxo.talenteditor.network.CounterProgressPacket;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import com.kidxoxo.talenteditor.network.ClientResourceRequirementsPacket;
import com.kidxoxo.talenteditor.network.ClientResourceStatusPacket;
import com.kidxoxo.talenteditor.network.EditorSettingsPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientEditorState {
    private static EditorBootstrap bootstrap;
    private static ValidationResultPacket validation;
    private static PlayerTalentState playerTalentState;
    private static TalentBookLayoutDefinition bookLayout = TalentBookLayoutDefinition.DEFAULT;
    private static DrawSessionView drawSession;
    private static ClientResourceRequirementsPacket resourceRequirements;
    private static ClientResourceStatusPacket resourceStatus;
    private static EditorSettingsPacket editorSettings = new EditorSettingsPacket(
            true,
            com.kidxoxo.talenteditor.settings.EditorRuntimeSettings.SynthesisSoundAudience.PERSONAL,
            com.kidxoxo.talenteditor.settings.EditorRuntimeSettings.SynthesisMessageAudience.PERSONAL,
            com.kidxoxo.talenteditor.settings.EditorRuntimeSettings.SoundOverlapPolicy.IMMEDIATE,
            false
    );

    private ClientEditorState() {
    }

    public static EditorBootstrap bootstrap() {
        return bootstrap;
    }

    static void setBootstrap(EditorBootstrap value) {
        bootstrap = value;
    }

    public static ValidationResultPacket validation() {
        return validation;
    }

    public static void clearValidation() {
        validation = null;
    }

    static void setValidation(ValidationResultPacket value) {
        validation = value;
    }

    public static PlayerTalentState playerTalentState() {
        return playerTalentState;
    }

    static void setPlayerTalentState(PlayerTalentState value) {
        playerTalentState = value;
    }

    public static TalentBookLayoutDefinition bookLayout() {
        return bookLayout;
    }

    static void setBookLayout(TalentBookLayoutDefinition value) {
        bookLayout = value;
    }

    static PlayerTalentState applyCounterProgress(CounterProgressPacket packet) {
        if (playerTalentState == null) return null;
        Map<net.minecraft.resources.ResourceLocation, Long> values = new HashMap<>();
        packet.entries().forEach(entry -> values.put(entry.id(), entry.value()));
        List<PlayerTalentState.CounterProgressView> counters = playerTalentState.counters().stream()
                .map(counter -> values.containsKey(counter.id())
                        ? new PlayerTalentState.CounterProgressView(
                                counter.id(), counter.name(), counter.event(), values.get(counter.id()),
                                counter.maximum(), counter.defined(), counter.active()
                        ) : counter)
                .toList();
        List<PlayerTalentState.SynthesisView> syntheses = playerTalentState.syntheses().stream()
                .map(synthesis -> {
                    List<PlayerTalentState.SynthesisCostView> costs = synthesis.costs().stream()
                            .map(cost -> cost.type() == PlayerTalentState.SynthesisCostType.COUNTER
                                    && values.containsKey(cost.id())
                                    ? new PlayerTalentState.SynthesisCostView(
                                            cost.type(), cost.id(), cost.required(), values.get(cost.id())
                                    ) : cost)
                            .toList();
                    return new PlayerTalentState.SynthesisView(
                            synthesis.target(), synthesis.conditionMet(), synthesis.ready(), costs
                    );
                })
                .toList();
        playerTalentState = new PlayerTalentState(
                playerTalentState.definitionRevision(), playerTalentState.drawTokens(), playerTalentState.truncated(),
                playerTalentState.talents(), playerTalentState.synthesesTruncated(), syntheses,
                playerTalentState.countersTruncated(), counters
        );
        return playerTalentState;
    }

    public static DrawSessionView drawSession() {
        return drawSession;
    }

    static void setDrawSession(DrawSessionView value) {
        drawSession = value;
    }

    public static ClientResourceRequirementsPacket resourceRequirements() {
        return resourceRequirements;
    }

    static void setResourceRequirements(ClientResourceRequirementsPacket value) {
        resourceRequirements = value;
    }

    public static ClientResourceStatusPacket resourceStatus() {
        return resourceStatus;
    }

    static void setResourceStatus(ClientResourceStatusPacket value) {
        resourceStatus = value;
    }

    public static EditorSettingsPacket editorSettings() {
        return editorSettings;
    }

    static void setEditorSettings(EditorSettingsPacket value) {
        editorSettings = value;
    }
}
