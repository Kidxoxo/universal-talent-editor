package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.catalog.AttributeCatalog;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.domain.RarityDefinition;
import com.kidxoxo.talenteditor.domain.CounterRuleDefinition;
import com.kidxoxo.talenteditor.domain.ConfiguredItemDefinition;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.ModifyNumericStateAction;
import com.kidxoxo.talenteditor.domain.HudResourceAction;
import com.kidxoxo.talenteditor.editor.EditorSubmissionResult;
import com.kidxoxo.talenteditor.editor.EditorTransactionSummary;
import com.kidxoxo.talenteditor.editor.DefinitionService;
import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.kidxoxo.talenteditor.draw.DrawResult;
import com.kidxoxo.talenteditor.draw.DrawSession;
import com.kidxoxo.talenteditor.draw.TalentDrawService;
import com.kidxoxo.talenteditor.domain.TalentPoolDefinition;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import com.kidxoxo.talenteditor.resource.ClientResourceRequirementCatalog;
import com.kidxoxo.talenteditor.synthesis.SynthesisResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

public final class EditorNetwork {
    private static final String PROTOCOL = "42";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_CATALOG_ENTRIES = 10_000;
    private static final int MAX_POOL_ENTRIES = 2_048;
    private static final int MAX_RARITY_ENTRIES = 2_048;
    private static final int MAX_COUNTER_ENTRIES = 4_096;
    private static final int MAX_LAYOUT_ENTRIES = 512;
    private static final int MAX_CONFIGURED_ITEM_ENTRIES = 10_000;
    private static final int MAX_POOL_RARITY_WEIGHTS = 256;
    private static final int MAX_CATALOG_TEXT = 512;
    private static final int MAX_TAG_ENTRIES = 20_000;
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(TalentEditorMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static int nextMessageId;
    private static boolean registered;

    private EditorNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.messageBuilder(OpenEditorPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenEditorPacket::encode)
                .decoder(OpenEditorPacket::decode)
                .consumerMainThread(OpenEditorPacket::handle)
                .add();
        CHANNEL.messageBuilder(SubmitTalentPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SubmitTalentPacket::encode)
                .decoder(SubmitTalentPacket::decode)
                .consumerMainThread(SubmitTalentPacket::handle)
                .add();
        CHANNEL.messageBuilder(SubmitPoolPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SubmitPoolPacket::encode)
                .decoder(SubmitPoolPacket::decode)
                .consumerMainThread(SubmitPoolPacket::handle)
                .add();
        CHANNEL.messageBuilder(SubmitRarityPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SubmitRarityPacket::encode)
                .decoder(SubmitRarityPacket::decode)
                .consumerMainThread(SubmitRarityPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestTalentPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestTalentPacket::encode)
                .decoder(RequestTalentPacket::decode)
                .consumerMainThread(RequestTalentPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestPoolPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestPoolPacket::encode)
                .decoder(RequestPoolPacket::decode)
                .consumerMainThread(RequestPoolPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestRarityPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestRarityPacket::encode)
                .decoder(RequestRarityPacket::decode)
                .consumerMainThread(RequestRarityPacket::handle)
                .add();
        CHANNEL.messageBuilder(SubmitCounterPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SubmitCounterPacket::encode)
                .decoder(SubmitCounterPacket::decode)
                .consumerMainThread(SubmitCounterPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestCounterPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestCounterPacket::encode)
                .decoder(RequestCounterPacket::decode)
                .consumerMainThread(RequestCounterPacket::handle)
                .add();
        CHANNEL.messageBuilder(SubmitLayoutPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SubmitLayoutPacket::encode)
                .decoder(SubmitLayoutPacket::decode)
                .consumerMainThread(SubmitLayoutPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestLayoutPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestLayoutPacket::encode)
                .decoder(RequestLayoutPacket::decode)
                .consumerMainThread(RequestLayoutPacket::handle)
                .add();
        CHANNEL.messageBuilder(SubmitConfiguredItemPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SubmitConfiguredItemPacket::encode)
                .decoder(SubmitConfiguredItemPacket::decode)
                .consumerMainThread(SubmitConfiguredItemPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestConfiguredItemPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestConfiguredItemPacket::encode)
                .decoder(RequestConfiguredItemPacket::decode)
                .consumerMainThread(RequestConfiguredItemPacket::handle)
                .add();
        CHANNEL.messageBuilder(DeleteDefinitionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteDefinitionPacket::encode)
                .decoder(DeleteDefinitionPacket::decode)
                .consumerMainThread(DeleteDefinitionPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestTransactionHistoryPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestTransactionHistoryPacket::encode)
                .decoder(RequestTransactionHistoryPacket::decode)
                .consumerMainThread(RequestTransactionHistoryPacket::handle)
                .add();
        CHANNEL.messageBuilder(UndoDefinitionTransactionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UndoDefinitionTransactionPacket::encode)
                .decoder(UndoDefinitionTransactionPacket::decode)
                .consumerMainThread(UndoDefinitionTransactionPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenTalentBookPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenTalentBookPacket::encode)
                .decoder(OpenTalentBookPacket::decode)
                .consumerMainThread(OpenTalentBookPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenDrawPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenDrawPacket::encode)
                .decoder(OpenDrawPacket::decode)
                .consumerMainThread(OpenDrawPacket::handle)
                .add();
        CHANNEL.messageBuilder(DrawActionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DrawActionPacket::encode)
                .decoder(DrawActionPacket::decode)
                .consumerMainThread(DrawActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(SynthesizeTalentPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SynthesizeTalentPacket::encode)
                .decoder(SynthesizeTalentPacket::decode)
                .consumerMainThread(SynthesizeTalentPacket::handle)
                .add();
        CHANNEL.messageBuilder(ClientResourceStatusPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClientResourceStatusPacket::encode)
                .decoder(ClientResourceStatusPacket::decode)
                .consumerMainThread(ClientResourceStatusPacket::handle)
                .add();
        CHANNEL.messageBuilder(UpdateEditorSettingsPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateEditorSettingsPacket::encode)
                .decoder(UpdateEditorSettingsPacket::decode)
                .consumerMainThread(UpdateEditorSettingsPacket::handle)
                .add();
        CHANNEL.messageBuilder(EditorManagementActionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EditorManagementActionPacket::encode)
                .decoder(EditorManagementActionPacket::decode)
                .consumerMainThread(EditorManagementActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(SubmitItemSubmissionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SubmitItemSubmissionPacket::encode)
                .decoder(SubmitItemSubmissionPacket::decode)
                .consumerMainThread(SubmitItemSubmissionPacket::handle)
                .add();
        CHANNEL.messageBuilder(BootstrapPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BootstrapPacket::encode)
                .decoder(BootstrapPacket::decode)
                .consumerMainThread(BootstrapPacket::handle)
                .add();
        CHANNEL.messageBuilder(ConfiguredItemCatalogPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfiguredItemCatalogPacket::encode)
                .decoder(ConfiguredItemCatalogPacket::decode)
                .consumerMainThread(ConfiguredItemCatalogPacket::handle)
                .add();
        CHANNEL.messageBuilder(ValidationResultPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ValidationResultPacket::encode)
                .decoder(ValidationResultPacket::decode)
                .consumerMainThread(ValidationResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(DefinitionDeletionResultPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DefinitionDeletionResultPacket::encode)
                .decoder(DefinitionDeletionResultPacket::decode)
                .consumerMainThread(DefinitionDeletionResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(TransactionHistoryPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TransactionHistoryPacket::encode)
                .decoder(TransactionHistoryPacket::decode)
                .consumerMainThread(TransactionHistoryPacket::handle)
                .add();
        CHANNEL.messageBuilder(DefinitionUndoResultPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DefinitionUndoResultPacket::encode)
                .decoder(DefinitionUndoResultPacket::decode)
                .consumerMainThread(DefinitionUndoResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(TalentDefinitionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TalentDefinitionPacket::encode)
                .decoder(TalentDefinitionPacket::decode)
                .consumerMainThread(TalentDefinitionPacket::handle)
                .add();
        CHANNEL.messageBuilder(PoolDefinitionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PoolDefinitionPacket::encode)
                .decoder(PoolDefinitionPacket::decode)
                .consumerMainThread(PoolDefinitionPacket::handle)
                .add();
        CHANNEL.messageBuilder(RarityDefinitionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RarityDefinitionPacket::encode)
                .decoder(RarityDefinitionPacket::decode)
                .consumerMainThread(RarityDefinitionPacket::handle)
                .add();
        CHANNEL.messageBuilder(CounterDefinitionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CounterDefinitionPacket::encode)
                .decoder(CounterDefinitionPacket::decode)
                .consumerMainThread(CounterDefinitionPacket::handle)
                .add();
        CHANNEL.messageBuilder(LayoutDefinitionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LayoutDefinitionPacket::encode)
                .decoder(LayoutDefinitionPacket::decode)
                .consumerMainThread(LayoutDefinitionPacket::handle)
                .add();
        CHANNEL.messageBuilder(ConfiguredItemDefinitionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfiguredItemDefinitionPacket::encode)
                .decoder(ConfiguredItemDefinitionPacket::decode)
                .consumerMainThread(ConfiguredItemDefinitionPacket::handle)
                .add();
        CHANNEL.messageBuilder(PlayerTalentStatePacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerTalentStatePacket::encode)
                .decoder(PlayerTalentStatePacket::decode)
                .consumerMainThread(PlayerTalentStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(CounterProgressPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CounterProgressPacket::encode)
                .decoder(CounterProgressPacket::decode)
                .consumerMainThread(CounterProgressPacket::handle)
                .add();
        CHANNEL.messageBuilder(DrawSessionPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DrawSessionPacket::encode)
                .decoder(DrawSessionPacket::decode)
                .consumerMainThread(DrawSessionPacket::handle)
                .add();
        CHANNEL.messageBuilder(DrawResultPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DrawResultPacket::encode)
                .decoder(DrawResultPacket::decode)
                .consumerMainThread(DrawResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(SynthesisResultPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SynthesisResultPacket::encode)
                .decoder(SynthesisResultPacket::decode)
                .consumerMainThread(SynthesisResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        ClientResourceRequirementsPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(ClientResourceRequirementsPacket::encode)
                .decoder(ClientResourceRequirementsPacket::decode)
                .consumerMainThread(ClientResourceRequirementsPacket::handle)
                .add();
        CHANNEL.messageBuilder(EditorSettingsPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EditorSettingsPacket::encode)
                .decoder(EditorSettingsPacket::decode)
                .consumerMainThread(EditorSettingsPacket::handle)
                .add();
        CHANNEL.messageBuilder(EditorManagementResultPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EditorManagementResultPacket::encode)
                .decoder(EditorManagementResultPacket::decode)
                .consumerMainThread(EditorManagementResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(SynthesisSoundPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SynthesisSoundPacket::encode)
                .decoder(SynthesisSoundPacket::decode)
                .consumerMainThread(SynthesisSoundPacket::handle)
                .add();
        CHANNEL.messageBuilder(ParticleWavePacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ParticleWavePacket::encode)
                .decoder(ParticleWavePacket::decode)
                .consumerMainThread(ParticleWavePacket::handle)
                .add();
        CHANNEL.messageBuilder(PresentationTransformPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PresentationTransformPacket::encode)
                .decoder(PresentationTransformPacket::decode)
                .consumerMainThread(PresentationTransformPacket::handle)
                .add();
        CHANNEL.messageBuilder(ItemSubmissionOpenPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ItemSubmissionOpenPacket::encode)
                .decoder(ItemSubmissionOpenPacket::decode)
                .consumerMainThread(ItemSubmissionOpenPacket::handle)
                .add();
        CHANNEL.messageBuilder(ItemSubmissionResultPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ItemSubmissionResultPacket::encode)
                .decoder(ItemSubmissionResultPacket::decode)
                .consumerMainThread(ItemSubmissionResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(PlayerVisualStatePacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerVisualStatePacket::encode)
                .decoder(PlayerVisualStatePacket::decode)
                .consumerMainThread(PlayerVisualStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(HudResourceStatePacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HudResourceStatePacket::encode)
                .decoder(HudResourceStatePacket::decode)
                .consumerMainThread(HudResourceStatePacket::handle)
                .add();
    }

    public static void requestOpen() {
        CHANNEL.sendToServer(new OpenEditorPacket());
    }

    public static void requestTalentBook() {
        CHANNEL.sendToServer(new OpenTalentBookPacket());
    }

    public static void requestDraw(ResourceLocation poolId) {
        CHANNEL.sendToServer(new OpenDrawPacket(poolId));
    }

    public static void refreshDraw(java.util.UUID sessionId) {
        CHANNEL.sendToServer(DrawActionPacket.refresh(sessionId));
    }

    public static void selectDraw(java.util.UUID sessionId, ResourceLocation talentId) {
        CHANNEL.sendToServer(DrawActionPacket.select(sessionId, talentId));
    }

    public static void requestSynthesis(ResourceLocation target) {
        CHANNEL.sendToServer(new SynthesizeTalentPacket(target));
    }

    public static void submitItemSubmission(java.util.UUID sessionId, boolean confirm) {
        CHANNEL.sendToServer(new SubmitItemSubmissionPacket(sessionId, confirm));
    }

    public static void sendItemSubmissionOpen(ServerPlayer player, ItemSubmissionOpenPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendItemSubmissionResult(ServerPlayer player, ItemSubmissionResultPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void submitTalent(long baseRevision, ResourceLocation id, String json, boolean commit) {
        CHANNEL.sendToServer(new SubmitTalentPacket(baseRevision, id, json, commit));
    }

    public static void submitPool(long baseRevision, ResourceLocation id, String json, boolean commit) {
        CHANNEL.sendToServer(new SubmitPoolPacket(baseRevision, id, json, commit));
    }

    public static void submitRarity(long baseRevision, ResourceLocation id, String json, boolean commit) {
        CHANNEL.sendToServer(new SubmitRarityPacket(baseRevision, id, json, commit));
    }

    public static void submitCounter(long baseRevision, ResourceLocation id, String json, boolean commit) {
        CHANNEL.sendToServer(new SubmitCounterPacket(baseRevision, id, json, commit));
    }

    public static void submitLayout(long baseRevision, ResourceLocation id, String json, boolean commit) {
        CHANNEL.sendToServer(new SubmitLayoutPacket(baseRevision, id, json, commit));
    }

    public static void submitConfiguredItem(long baseRevision, ResourceLocation id, String json, boolean commit) {
        CHANNEL.sendToServer(new SubmitConfiguredItemPacket(baseRevision, id, json, commit));
    }

    public static void deleteDefinition(long baseRevision, DefinitionKind kind, ResourceLocation id) {
        CHANNEL.sendToServer(new DeleteDefinitionPacket(baseRevision, kind, id));
    }

    public static void requestTransactionHistory() {
        CHANNEL.sendToServer(new RequestTransactionHistoryPacket());
    }

    public static void undoTransaction(long baseRevision, java.util.UUID transactionId) {
        CHANNEL.sendToServer(new UndoDefinitionTransactionPacket(baseRevision, transactionId));
    }

    public static void requestTalent(ResourceLocation id) {
        CHANNEL.sendToServer(new RequestTalentPacket(id));
    }

    public static void requestPool(ResourceLocation id) {
        CHANNEL.sendToServer(new RequestPoolPacket(id));
    }

    public static void requestRarity(ResourceLocation id) {
        CHANNEL.sendToServer(new RequestRarityPacket(id));
    }

    public static void requestCounter(ResourceLocation id) {
        CHANNEL.sendToServer(new RequestCounterPacket(id));
    }

    public static void requestLayout(ResourceLocation id) {
        CHANNEL.sendToServer(new RequestLayoutPacket(id));
    }

    public static void requestConfiguredItem(ResourceLocation id) {
        CHANNEL.sendToServer(new RequestConfiguredItemPacket(id));
    }

    public static void sendClientResourceStatus(ClientResourceStatusPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void updateSettings(com.kidxoxo.talenteditor.settings.EditorRuntimeSettings settings) {
        CHANNEL.sendToServer(new UpdateEditorSettingsPacket(
                settings.automaticSynthesis(), settings.synthesisSoundAudience(),
                settings.synthesisMessageAudience(), settings.soundOverlapPolicy()
        ));
    }

    public static void runManagementAction(
            com.kidxoxo.talenteditor.settings.EditorSettingsService.ManagementAction action
    ) {
        CHANNEL.sendToServer(new EditorManagementActionPacket(action));
    }

    public static void importSettings(String archivePath) {
        CHANNEL.sendToServer(new EditorManagementActionPacket(
                com.kidxoxo.talenteditor.settings.EditorSettingsService.ManagementAction.IMPORT,
                archivePath
        ));
    }

    public static void sendSettings(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EditorSettingsPacket(
                TalentEditorMod.SETTINGS.automaticSynthesis(),
                TalentEditorMod.SETTINGS.synthesisSoundAudience(),
                TalentEditorMod.SETTINGS.synthesisMessageAudience(),
                TalentEditorMod.SETTINGS.soundOverlapPolicy(),
                TalentEditorMod.SETTINGS.globalTemplateAvailable()
        ));
    }

    public static void syncSettings(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(EditorNetwork::sendSettings);
    }

    public static void sendManagementResult(
            ServerPlayer player,
            com.kidxoxo.talenteditor.settings.EditorSettingsService.ManagementResult result
    ) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EditorManagementResultPacket(
                result.action(), result.successful(), result.detail()
        ));
    }

    public static void sendSynthesisSound(
            ServerPlayer player,
            com.kidxoxo.talenteditor.domain.PlaySoundAction action,
            com.kidxoxo.talenteditor.settings.EditorRuntimeSettings.SoundOverlapPolicy overlapPolicy
    ) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SynthesisSoundPacket(
                action.sound(), action.source(), action.volume(), action.pitch(), overlapPolicy
        ));
    }

    public static void sendParticleWave(ServerPlayer player, ParticleWavePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendPresentationTransform(ServerPlayer subject, PresentationTransformPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> subject), packet);
    }

    public static void sendBootstrap(ServerPlayer player, boolean openScreen) {
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        RawDefinitionSet raw = TalentEditorMod.DEFINITIONS.activeRaw();
        List<DefinitionReference> references = snapshot.reverseReferences().values().stream()
                .flatMap(List::stream)
                .limit(BootstrapPacket.MAX_REFERENCES)
                .toList();
        long totalReferences = snapshot.reverseReferences().values().stream().mapToLong(List::size).sum();
        List<EditorBootstrap.RaritySummary> rarities = snapshot.rarities().values().stream()
                .sorted(Comparator.comparingInt(RarityDefinition::order)
                        .thenComparing(rarity -> rarity.id().toString()))
                .limit(MAX_RARITY_ENTRIES)
                .map(rarity -> summarize(
                        rarity, source(raw.raritySources(), rarity.id()),
                        snapshot.referencesTo(DefinitionKind.RARITY, rarity.id()).size()
                ))
                .toList();
        List<EditorBootstrap.TalentSummary> talents = snapshot.talents().values().stream()
                .sorted(Comparator.comparing(talent -> talent.id().toString()))
                .limit(MAX_CATALOG_ENTRIES)
                .map(talent -> summarize(
                        talent, raw.talents().get(talent.id()), source(raw.talentSources(), talent.id()),
                        snapshot.referencesTo(DefinitionKind.TALENT, talent.id()).size()
                ))
                .toList();
        List<EditorBootstrap.PoolSummary> pools = snapshot.pools().values().stream()
                .sorted(Comparator.comparing(pool -> pool.id().toString()))
                .limit(MAX_POOL_ENTRIES)
                .map(pool -> summarize(
                        pool, source(raw.poolSources(), pool.id()),
                        snapshot.referencesTo(DefinitionKind.POOL, pool.id()).size()
                ))
                .toList();
        List<EditorBootstrap.CounterSummary> counters = snapshot.counters().values().stream()
                .sorted(Comparator.comparing(counter -> counter.id().toString()))
                .limit(MAX_COUNTER_ENTRIES)
                .map(counter -> summarize(
                        counter, source(raw.counterSources(), counter.id()),
                        snapshot.referencesTo(DefinitionKind.COUNTER, counter.id()).size()
                ))
                .toList();
        List<EditorBootstrap.LayoutSummary> layouts = snapshot.layouts().values().stream()
                .sorted(Comparator.comparingInt(TalentBookLayoutDefinition::priority).reversed()
                        .thenComparing(layout -> layout.id().toString()))
                .limit(MAX_LAYOUT_ENTRIES)
                .map(layout -> summarize(
                        layout, source(raw.layoutSources(), layout.id()),
                        snapshot.bookLayout().equals(layout)
                ))
                .toList();
        List<EditorBootstrap.ConfiguredItemSummary> configuredItems = snapshot.configuredItems().values().stream()
                .sorted(Comparator.comparing(item -> item.id().toString()))
                .limit(MAX_CONFIGURED_ITEM_ENTRIES)
                .map(item -> summarize(
                        item, source(raw.configuredItemSources(), item.id()),
                        snapshot.referencesTo(DefinitionKind.ITEM, item.id()).size()
                ))
                .toList();
        EditorBootstrap bootstrap = new EditorBootstrap(
                snapshot.revision(), snapshot.contentHash(), rarities, talents, pools, counters, layouts,
                references, totalReferences > references.size(),
                tagIds(player.serverLevel().registryAccess().registryOrThrow(Registries.ENTITY_TYPE)),
                tagIds(player.serverLevel().registryAccess().registryOrThrow(Registries.ITEM)),
                tagIds(player.serverLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)),
                AttributeCatalog.capture(player).stream().limit(MAX_CATALOG_ENTRIES).toList(),
                configuredItems,
                numericStates(snapshot, player)
        );
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BootstrapPacket(bootstrap, openScreen));
        sendConfiguredItemCatalog(player);
    }

    public static void sendConfiguredItemCatalog(ServerPlayer player) {
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        List<ConfiguredItemCatalogPacket.Entry> items = snapshot.configuredItems().values().stream()
                .filter(item -> item.enabled() && item.available())
                .sorted(Comparator.comparing(item -> item.id().toString()))
                .limit(MAX_CONFIGURED_ITEM_ENTRIES)
                .map(item -> new ConfiguredItemCatalogPacket.Entry(
                        item.id(), clipCatalogText(item.name().fallbackText()),
                        item.description().stream().limit(32)
                                .map(line -> clipCatalogText(line.fallbackText())).toList(),
                        item.customModelData(), item.glint()
                ))
                .toList();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ConfiguredItemCatalogPacket(snapshot.revision(), items));
    }

    private static List<ResourceLocation> numericStates(RuntimeSnapshot snapshot, ServerPlayer player) {
        LinkedHashSet<ResourceLocation> states = new LinkedHashSet<>();
        snapshot.talents().values().stream()
                .sorted(Comparator.comparing(talent -> talent.id().toString()))
                .forEach(talent -> {
                    collectNumericStates(states, talent.activeEffects());
                    collectNumericStates(states, talent.onAcquire());
                    collectNumericStates(states, talent.onSynthesize());
                    collectNumericStates(states, talent.onRemove());
                    talent.triggers().forEach(trigger -> {
                        collectNumericStates(states, trigger.actions());
                        collectNumericStates(states, trigger.failureActions());
                    });
                });
        states.addAll(PlayerTalents.numericStates(player).keySet());
        return states.stream().sorted(Comparator.comparing(ResourceLocation::toString))
                .limit(MAX_CATALOG_ENTRIES).toList();
    }

    private static void collectNumericStates(
            java.util.Set<ResourceLocation> states,
            List<CompiledAction> actions
    ) {
        actions.stream().map(com.kidxoxo.talenteditor.domain.ConditionalAction::unwrap)
                .filter(ModifyNumericStateAction.class::isInstance)
                .map(ModifyNumericStateAction.class::cast)
                .map(ModifyNumericStateAction::state)
                .forEach(states::add);
        actions.stream().map(com.kidxoxo.talenteditor.domain.ConditionalAction::unwrap)
                .filter(HudResourceAction.class::isInstance)
                .map(HudResourceAction.class::cast)
                .map(HudResourceAction::state)
                .forEach(states::add);
    }

    public static void sendValidation(ServerPlayer player, EditorSubmissionResult result) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), ValidationResultPacket.from(result));
    }

    public static void sendDeletionResult(ServerPlayer player, DefinitionDeletionResultPacket result) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), result);
    }

    public static void sendTransactionHistory(
            ServerPlayer player,
            List<EditorTransactionSummary> transactions
    ) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TransactionHistoryPacket(
                TalentEditorMod.SNAPSHOTS.active().revision(), transactions
        ));
    }

    public static void sendUndoResult(ServerPlayer player, DefinitionUndoResultPacket result) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), result);
    }

    public static void sendTalentDefinition(ServerPlayer player, ResourceLocation id, JsonObject definition) {
        String json = GSON.toJson(definition);
        if (json.length() > DefinitionService.MAX_JSON_CHARACTERS) {
            player.sendSystemMessage(Component.translatable(
                    "message.talent_editor.definition_too_large", id.toString()
            ));
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TalentDefinitionPacket(
                TalentEditorMod.SNAPSHOTS.active().revision(), id, json
        ));
    }

    public static void sendPoolDefinition(ServerPlayer player, ResourceLocation id, JsonObject definition) {
        String json = GSON.toJson(definition);
        if (json.length() > DefinitionService.MAX_JSON_CHARACTERS) {
            player.sendSystemMessage(Component.translatable(
                    "message.talent_editor.pool_definition_too_large", id.toString()
            ));
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PoolDefinitionPacket(
                TalentEditorMod.SNAPSHOTS.active().revision(), id, json
        ));
    }

    public static void sendRarityDefinition(ServerPlayer player, ResourceLocation id, JsonObject definition) {
        String json = GSON.toJson(definition);
        if (json.length() > DefinitionService.MAX_JSON_CHARACTERS) {
            player.sendSystemMessage(Component.translatable(
                    "message.talent_editor.rarity_definition_too_large", id.toString()
            ));
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RarityDefinitionPacket(
                TalentEditorMod.SNAPSHOTS.active().revision(), id, json
        ));
    }

    public static void sendCounterDefinition(ServerPlayer player, ResourceLocation id, JsonObject definition) {
        String json = GSON.toJson(definition);
        if (json.length() > DefinitionService.MAX_JSON_CHARACTERS) {
            player.sendSystemMessage(Component.translatable(
                    "message.talent_editor.counter_definition_too_large", id.toString()
            ));
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CounterDefinitionPacket(
                TalentEditorMod.SNAPSHOTS.active().revision(), id, json
        ));
    }

    public static void sendLayoutDefinition(ServerPlayer player, ResourceLocation id, JsonObject definition) {
        String json = GSON.toJson(definition);
        if (json.length() > DefinitionService.MAX_JSON_CHARACTERS) {
            player.sendSystemMessage(Component.translatable(
                    "message.talent_editor.layout_definition_too_large", id.toString()
            ));
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new LayoutDefinitionPacket(
                TalentEditorMod.SNAPSHOTS.active().revision(), id, json
        ));
    }

    public static void sendConfiguredItemDefinition(
            ServerPlayer player, ResourceLocation id, JsonObject definition
    ) {
        String json = GSON.toJson(definition);
        if (json.length() > DefinitionService.MAX_JSON_CHARACTERS) {
            player.sendSystemMessage(Component.translatable(
                    "message.talent_editor.configured_item_definition_too_large", id.toString()
            ));
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ConfiguredItemDefinitionPacket(
                TalentEditorMod.SNAPSHOTS.active().revision(), id, json
        ));
    }

    public static void sendPlayerState(ServerPlayer player, boolean openScreen) {
        PlayerTalentState state = buildPlayerState(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerTalentStatePacket(
                state, TalentEditorMod.SNAPSHOTS.active().bookLayout(), openScreen
        ));
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new PlayerVisualStatePacket(player.getUUID(), state.render()));
    }

    public static void sendPlayerVisualState(ServerPlayer subject, ServerPlayer recipient) {
        PlayerTalentState.RenderView render = PlayerTalentStateFactory.renderView(
                TalentEditorMod.SNAPSHOTS.active(), subject, PlayerTalents.activeSources(subject)
        );
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient),
                new PlayerVisualStatePacket(subject.getUUID(), render));
    }

    /** Re-evaluates dynamic render expressions and updates the subject plus all tracking clients. */
    public static void sendPlayerVisualState(ServerPlayer subject) {
        PlayerTalentState.RenderView render = PlayerTalentStateFactory.renderView(
                TalentEditorMod.SNAPSHOTS.active(), subject, PlayerTalents.activeSources(subject)
        );
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> subject),
                new PlayerVisualStatePacket(subject.getUUID(), render));
    }

    public static void sendHudResources(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HudResourceStatePacket(
                PlayerTalentStateFactory.hudResources(
                        TalentEditorMod.SNAPSHOTS.active(), player, PlayerTalents.activeSources(player)
                )
        ));
    }

    public static void sendCounterProgress(ServerPlayer player, java.util.Collection<ResourceLocation> ids) {
        List<CounterProgressPacket.Entry> entries = ids.stream()
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .limit(CounterProgressPacket.MAX_ENTRIES)
                .map(id -> new CounterProgressPacket.Entry(id, PlayerTalents.counter(player, id)))
                .toList();
        if (!entries.isEmpty()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CounterProgressPacket(entries));
        }
    }

    public static void syncAllPlayerStates(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(player -> {
            sendPlayerState(player, false);
            sendResourceRequirements(player);
        });
    }

    public static void sendResourceRequirements(ServerPlayer player) {
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        ClientResourceRequirementCatalog.Result catalog = ClientResourceRequirementCatalog.from(snapshot);
        TalentEditorMod.CLIENT_RESOURCES.begin(
                player.getUUID(), player.getGameProfile().getName(), snapshot.revision(), snapshot.contentHash(),
                catalog.requirements(), catalog.truncated()
        );
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientResourceRequirementsPacket(
                snapshot.revision(), snapshot.contentHash(), catalog.truncated(), catalog.requirements()
        ));
    }

    public static void sendDrawResult(ServerPlayer player, DrawResult result) {
        if (result.session().isPresent()) {
            DrawSession session = result.session().orElseThrow();
            RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
            TalentPoolDefinition pool = snapshot.pools().get(session.poolId());
            if (pool == null) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new DrawResultPacket(DrawResult.Status.POOL_NOT_FOUND, java.util.Optional.empty()));
                return;
            }
            List<PlayerTalentState.TalentView> offers = session.offers().stream()
                    .map(id -> PlayerTalentStateFactory.view(snapshot, id))
                    .toList();
            DrawSessionView view = new DrawSessionView(
                    session.id(), session.definitionRevision(), session.poolId(),
                    PlayerTalentStateFactory.clipText(pool.name().fallbackText()), PlayerTalents.drawTokens(player),
                    session.refreshesRemaining(), pool.iconSize(), TalentDrawService.luckValue(player, pool),
                    PlayerTalents.drawPity(player, pool.id()), pool.pityAfter(),
                    pool.pityAfter() > 0 && PlayerTalents.drawPity(player, pool.id()) >= pool.pityAfter() - 1,
                    TalentDrawService.rarityProbabilitiesForPlayer(snapshot, pool, player), offers
            );
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DrawSessionPacket(view));
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DrawResultPacket(result.status(), result.acquired()));
    }

    public static void sendSynthesisResult(ServerPlayer player, SynthesisResult result) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SynthesisResultPacket.from(result));
    }

    static PlayerTalentState buildPlayerState(ServerPlayer player) {
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        return PlayerTalentStateFactory.buildForPlayer(snapshot, player);
    }

    private static EditorBootstrap.TalentSummary summarize(
            TalentDefinition talent, JsonObject raw, String source, int inboundReferences
    ) {
        TalentRelationSummary relations = TalentRelationSummary.from(raw);
        return new EditorBootstrap.TalentSummary(
                talent.id(), clipCatalogText(talent.name().fallbackText()), talent.rarity(),
                talent.enabled(), talent.available(), talent.categories().stream().limit(256).toList(),
                source, inboundReferences, talent.icon(),
                relations.acquireTalents().stream().distinct()
                        .sorted(Comparator.comparing(ResourceLocation::toString)).limit(256).toList(),
                relations.acquireEditable(), relations.acquireMode(),
                Math.min(256, relations.acquireOtherConditions()),
                talent.synthesis().enabled(), talent.synthesis().consumeTalents().stream().distinct()
                        .sorted(Comparator.comparing(ResourceLocation::toString)).limit(256).toList(),
                Math.min(256, talent.synthesis().consumeCounters().size()
                        + talent.synthesis().consumeItems().size())
        );
    }

    private static EditorBootstrap.RaritySummary summarize(
            RarityDefinition rarity, String source, int inboundReferences
    ) {
        return new EditorBootstrap.RaritySummary(
                rarity.id(), clipCatalogText(rarity.name().fallbackText()), rarity.color(), rarity.order(),
                rarity.enabled(), rarity.offerable(), rarity.baseWeight(), source, inboundReferences
        );
    }

    private static EditorBootstrap.PoolSummary summarize(
            TalentPoolDefinition pool, String source, int inboundReferences
    ) {
        LinkedHashMap<ResourceLocation, Double> rarityWeights = new LinkedHashMap<>();
        pool.rarityWeights().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .limit(MAX_POOL_RARITY_WEIGHTS)
                .forEach(entry -> rarityWeights.put(entry.getKey(), entry.getValue()));
        return new EditorBootstrap.PoolSummary(
                pool.id(), clipCatalogText(pool.name().fallbackText()), pool.enabled(), pool.choices(), pool.refreshes(),
                pool.maxOwned(), pool.excludeOwned(), rarityWeights, source, inboundReferences
        );
    }

    private static EditorBootstrap.CounterSummary summarize(
            CounterRuleDefinition counter, String source, int inboundReferences
    ) {
        return new EditorBootstrap.CounterSummary(
                counter.id(), clipCatalogText(counter.name().fallbackText()), counter.event().serializedName(),
                counter.enabled(), counter.available(), counter.categories(), counter.visibility().serializedName(),
                source, inboundReferences
        );
    }

    private static EditorBootstrap.LayoutSummary summarize(
            TalentBookLayoutDefinition layout, String source, boolean active
    ) {
        return new EditorBootstrap.LayoutSummary(
                layout.id(), layout.enabled(), active, layout.priority(), layout.background(),
                layout.panelPercent(), layout.columns(), layout.rows(), layout.slotWidth(), layout.slotHeight(),
                layout.iconSize(), layout.slots().size(), layout.pages().size(),
                layout.showGeneratedEffects(), source, 0
        );
    }

    private static EditorBootstrap.ConfiguredItemSummary summarize(
            ConfiguredItemDefinition item, String source, int inboundReferences
    ) {
        return new EditorBootstrap.ConfiguredItemSummary(
                item.id(), clipCatalogText(item.name().fallbackText()), item.enabled(), item.available(),
                item.customModelData(), item.glint(), source, inboundReferences
        );
    }

    private static String source(java.util.Map<ResourceLocation, String> sources, ResourceLocation id) {
        return clipCatalogText(sources.getOrDefault(id, "unknown"));
    }

    private static String clipCatalogText(String value) {
        if (value.length() <= MAX_CATALOG_TEXT) return value;
        return value.substring(0, MAX_CATALOG_TEXT - 1) + "…";
    }

    private static <T> List<ResourceLocation> tagIds(Registry<T> registry) {
        return registry.getTagNames()
                .map(tag -> tag.location())
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .limit(MAX_TAG_ENTRIES)
                .toList();
    }
}
