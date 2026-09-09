package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.BootstrapPacket;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.ValidationResultPacket;
import com.kidxoxo.talenteditor.network.TalentDefinitionPacket;
import com.kidxoxo.talenteditor.network.PlayerTalentStatePacket;
import com.kidxoxo.talenteditor.network.PlayerTalentState;
import com.kidxoxo.talenteditor.network.DrawSessionPacket;
import com.kidxoxo.talenteditor.network.DrawResultPacket;
import com.kidxoxo.talenteditor.network.PoolDefinitionPacket;
import com.kidxoxo.talenteditor.network.RarityDefinitionPacket;
import com.kidxoxo.talenteditor.network.SynthesisResultPacket;
import com.kidxoxo.talenteditor.network.CounterDefinitionPacket;
import com.kidxoxo.talenteditor.network.CounterProgressPacket;
import com.kidxoxo.talenteditor.network.LayoutDefinitionPacket;
import com.kidxoxo.talenteditor.network.ConfiguredItemDefinitionPacket;
import com.kidxoxo.talenteditor.network.DefinitionDeletionResultPacket;
import com.kidxoxo.talenteditor.network.DefinitionUndoResultPacket;
import com.kidxoxo.talenteditor.network.TransactionHistoryPacket;
import com.kidxoxo.talenteditor.network.ClientResourceRequirementsPacket;
import com.kidxoxo.talenteditor.network.ClientResourceStatusPacket;
import com.kidxoxo.talenteditor.network.EditorSettingsPacket;
import com.kidxoxo.talenteditor.network.EditorManagementResultPacket;
import com.kidxoxo.talenteditor.draw.DrawResult;
import com.kidxoxo.talenteditor.editor.DefinitionDeletionStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void acceptBootstrap(BootstrapPacket packet) {
        ClientEditorState.setBootstrap(packet.bootstrap());
        if (packet.openScreen()) {
            ClientEditorState.clearValidation();
            Minecraft.getInstance().setScreen(new EditorHomeScreen(packet.bootstrap()));
        }
    }

    public static void acceptEditorSettings(EditorSettingsPacket packet) {
        ClientEditorState.setEditorSettings(packet);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof EditorSettingsScreen screen) screen.acceptSettings(packet);
        if (minecraft.screen instanceof TalentBookScreen screen) screen.refreshSettings();
    }

    public static void acceptManagementResult(EditorManagementResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof EditorSettingsScreen screen) {
            screen.acceptManagementResult(packet);
        } else if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(
                    packet.successful()
                            ? "screen.talent_editor.settings.result.success"
                            : "screen.talent_editor.settings.result.failure",
                    packet.detail()
            ), false);
        }
    }

    public static void acceptResourceRequirements(ClientResourceRequirementsPacket packet) {
        ClientEditorState.setResourceRequirements(packet);
        ClientEditorState.setResourceStatus(null);
        assessAndReport(packet);
    }

    public static void recheckResourceRequirements() {
        ClientResourceRequirementsPacket packet = ClientEditorState.resourceRequirements();
        if (packet != null) assessAndReport(packet);
    }

    private static void assessAndReport(ClientResourceRequirementsPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        java.util.Set<net.minecraft.resources.ResourceLocation> availableSounds = packet.requirements().stream()
                .filter(requirement -> requirement.kind()
                        == com.kidxoxo.talenteditor.resource.ClientResourceRequirement.Kind.SOUND)
                .map(com.kidxoxo.talenteditor.resource.ClientResourceRequirement::id)
                .filter(id -> minecraft.getSoundManager().getSoundEvent(id) != null)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        var resources = minecraft.getResourceManager();
        java.util.concurrent.CompletableFuture.supplyAsync(
                () -> ClientResourceAssessment.capture(resources, availableSounds, packet),
                Util.backgroundExecutor()
        ).whenComplete((status, error) -> minecraft.execute(() -> {
            ClientResourceRequirementsPacket current = ClientEditorState.resourceRequirements();
            if (current == null || current.revision() != packet.revision()
                    || !current.contentHash().equals(packet.contentHash())) return;
            ClientResourceStatusPacket completed = error == null ? status : ClientResourceAssessment.assess(
                    packet, requirement -> new ClientResourceAssessment.Observation(false, "")
            );
            ClientEditorState.setResourceStatus(completed);
            EditorNetwork.sendClientResourceStatus(completed);
        }));
    }

    public static void acceptValidation(ValidationResultPacket packet) {
        ClientEditorState.setValidation(packet);
    }

    public static void acceptDeletionResult(DefinitionDeletionResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.status() == DefinitionDeletionStatus.DELETED) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.talent_editor.delete.success", packet.id().toString()
                        ), false
                );
            }
            EditorBootstrap bootstrap = ClientEditorState.bootstrap();
            if (bootstrap == null || bootstrap.revision() != packet.activeRevision()) {
                EditorNetwork.requestOpen();
                return;
            }
            minecraft.setScreen(switch (packet.kind()) {
                case RARITY -> new RarityListScreen(bootstrap);
                case POOL -> new PoolListScreen(bootstrap);
                case COUNTER -> new CounterListScreen(bootstrap);
                case LAYOUT -> new LayoutListScreen(bootstrap);
                case ITEM -> new ConfiguredItemListScreen(bootstrap);
                case TALENT, TALENT_CATEGORY, COUNTER_CATEGORY -> new EditorHomeScreen(bootstrap);
            });
            return;
        }
        if (packet.status() == DefinitionDeletionStatus.REVISION_CONFLICT) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "screen.talent_editor.delete.result.revision_conflict"
                ), false);
            }
            EditorNetwork.requestOpen();
            return;
        }
        if (minecraft.screen instanceof DeleteDefinitionScreen screen) {
            screen.acceptResult(packet);
        } else if (minecraft.player != null) {
            minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "screen.talent_editor.delete.result."
                            + packet.status().name().toLowerCase(java.util.Locale.ROOT)
            ), false);
        }
    }

    public static void acceptTransactionHistory(TransactionHistoryPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        EditorBootstrap bootstrap = ClientEditorState.bootstrap();
        if (bootstrap == null || bootstrap.revision() != packet.revision()) {
            EditorNetwork.requestOpen();
            return;
        }
        minecraft.setScreen(new TransactionHistoryScreen(bootstrap, packet.transactions()));
    }

    public static void acceptUndoResult(DefinitionUndoResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.status() == com.kidxoxo.talenteditor.editor.DefinitionUndoStatus.UNDONE) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.talent_editor.undo.success"
                ), false);
            }
            EditorNetwork.requestTransactionHistory();
            return;
        }
        if (packet.status() == com.kidxoxo.talenteditor.editor.DefinitionUndoStatus.REVISION_CONFLICT) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "screen.talent_editor.undo.result.revision_conflict"
                ), false);
            }
            EditorNetwork.requestOpen();
            return;
        }
        if (minecraft.screen instanceof UndoTransactionScreen screen) {
            screen.acceptResult(packet);
        } else if (minecraft.player != null) {
            minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "screen.talent_editor.undo.result."
                            + packet.status().name().toLowerCase(java.util.Locale.ROOT)
            ), false);
        }
    }

    public static void acceptTalentDefinition(TalentDefinitionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        EditorBootstrap bootstrap = ClientEditorState.bootstrap();
        if (bootstrap == null || bootstrap.revision() != packet.revision()) {
            EditorNetwork.requestOpen();
            return;
        }
        ClientEditorState.clearValidation();
        try {
            minecraft.setScreen(TalentEditScreen.editExisting(bootstrap, packet.id(), packet.json()));
        } catch (RuntimeException ex) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.talent_editor.definition_decode_failed", packet.id().toString()
                        ), false
                );
            }
            minecraft.setScreen(new EditorHomeScreen(bootstrap));
        }
    }

    public static void acceptPoolDefinition(PoolDefinitionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        EditorBootstrap bootstrap = ClientEditorState.bootstrap();
        if (bootstrap == null || bootstrap.revision() != packet.revision()) {
            EditorNetwork.requestOpen();
            return;
        }
        ClientEditorState.clearValidation();
        try {
            minecraft.setScreen(PoolEditScreen.editExisting(bootstrap, packet.id(), packet.json()));
        } catch (RuntimeException ex) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.talent_editor.pool_definition_decode_failed", packet.id().toString()
                        ), false
                );
            }
            minecraft.setScreen(new PoolListScreen(bootstrap));
        }
    }

    public static void acceptRarityDefinition(RarityDefinitionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        EditorBootstrap bootstrap = ClientEditorState.bootstrap();
        if (bootstrap == null || bootstrap.revision() != packet.revision()) {
            EditorNetwork.requestOpen();
            return;
        }
        ClientEditorState.clearValidation();
        try {
            minecraft.setScreen(RarityEditScreen.editExisting(bootstrap, packet.id(), packet.json()));
        } catch (RuntimeException ex) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.talent_editor.rarity_definition_decode_failed", packet.id().toString()
                        ), false
                );
            }
            minecraft.setScreen(new RarityListScreen(bootstrap));
        }
    }

    public static void acceptCounterDefinition(CounterDefinitionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        EditorBootstrap bootstrap = ClientEditorState.bootstrap();
        if (bootstrap == null || bootstrap.revision() != packet.revision()) {
            EditorNetwork.requestOpen();
            return;
        }
        ClientEditorState.clearValidation();
        try {
            minecraft.setScreen(CounterEditScreen.editExisting(bootstrap, packet.id(), packet.json()));
        } catch (RuntimeException ex) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.talent_editor.counter_definition_decode_failed", packet.id().toString()
                        ), false
                );
            }
            minecraft.setScreen(new CounterListScreen(bootstrap));
        }
    }

    public static void acceptLayoutDefinition(LayoutDefinitionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        EditorBootstrap bootstrap = ClientEditorState.bootstrap();
        if (bootstrap == null || bootstrap.revision() != packet.revision()) {
            EditorNetwork.requestOpen();
            return;
        }
        ClientEditorState.clearValidation();
        try {
            minecraft.setScreen(LayoutEditScreen.editExisting(bootstrap, packet.id(), packet.json()));
        } catch (RuntimeException ex) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.talent_editor.layout_definition_decode_failed", packet.id().toString()
                ), false);
            }
            minecraft.setScreen(new LayoutListScreen(bootstrap));
        }
    }

    public static void acceptConfiguredItemDefinition(ConfiguredItemDefinitionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        EditorBootstrap bootstrap = ClientEditorState.bootstrap();
        if (bootstrap == null || bootstrap.revision() != packet.revision()) {
            EditorNetwork.requestOpen();
            return;
        }
        ClientEditorState.clearValidation();
        try {
            minecraft.setScreen(ConfiguredItemEditScreen.editExisting(
                    bootstrap, packet.id(), packet.json()
            ));
        } catch (RuntimeException ex) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(
                        "message.talent_editor.configured_item_definition_decode_failed",
                        packet.id().toString()
                ), false);
            }
            minecraft.setScreen(new ConfiguredItemListScreen(bootstrap));
        }
    }

    public static void acceptPlayerTalentState(PlayerTalentStatePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientEditorState.setPlayerTalentState(packet.state());
        ClientEditorState.setBookLayout(packet.layout());
        if (packet.openScreen()) {
            minecraft.setScreen(new TalentBookScreen(packet.state(), packet.layout()));
        } else if (minecraft.screen instanceof TalentBookScreen screen) {
            screen.updateState(packet.state(), packet.layout());
        }
    }

    public static void acceptCounterProgress(CounterProgressPacket packet) {
        PlayerTalentState state = ClientEditorState.applyCounterProgress(packet);
        Minecraft minecraft = Minecraft.getInstance();
        if (state != null && minecraft.screen instanceof TalentBookScreen screen) screen.updateState(state);
    }

    public static void acceptDrawSession(DrawSessionPacket packet) {
        ClientEditorState.setDrawSession(packet.view());
        Minecraft.getInstance().setScreen(new TalentDrawScreen(packet.view()));
    }

    public static void acceptDrawResult(DrawResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.status() == DrawResult.Status.ACQUIRED
                || packet.status() == DrawResult.Status.DUPLICATE_REFUNDED
                || packet.status() == DrawResult.Status.DUPLICATE_COUNTER_GRANTED) {
            if (minecraft.player != null && packet.acquired().isPresent()) {
                minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        packet.status() == DrawResult.Status.ACQUIRED
                                ? "screen.talent_editor.draw.acquired"
                                : "screen.talent_editor.draw.result."
                                + packet.status().name().toLowerCase(java.util.Locale.ROOT),
                        packet.acquired().orElseThrow().toString()
                ), false);
            }
            if (ClientEditorState.playerTalentState() != null) {
                minecraft.setScreen(new TalentBookScreen(
                        ClientEditorState.playerTalentState(), ClientEditorState.bookLayout()
                ));
            }
            return;
        }
        if (minecraft.screen instanceof TalentDrawScreen screen) {
            screen.setResult(packet.status());
        } else if (minecraft.player != null) {
            minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "screen.talent_editor.draw.result." + packet.status().name().toLowerCase(java.util.Locale.ROOT)
            ), false);
        }
    }

    public static void acceptSynthesisResult(SynthesisResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        String key = "screen.talent_editor.synthesis.result."
                + packet.status().name().toLowerCase(java.util.Locale.ROOT);
        net.minecraft.network.chat.Component message = packet.detail().isPresent()
                ? net.minecraft.network.chat.Component.translatable(
                        key, packet.target().toString(), packet.detail().orElseThrow().toString()
                )
                : net.minecraft.network.chat.Component.translatable(key, packet.target().toString());
        if (minecraft.player != null) minecraft.player.displayClientMessage(message, false);
    }
}
