package com.kidxoxo.talenteditor;

import com.kidxoxo.talenteditor.command.TalentEditorCommands;
import com.kidxoxo.talenteditor.compile.TalentCompiler;
import com.kidxoxo.talenteditor.editor.DefinitionService;
import com.kidxoxo.talenteditor.editor.EditorPackStorage;
import com.kidxoxo.talenteditor.draw.TalentDrawService;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.reload.DefinitionReloadListener;
import com.kidxoxo.talenteditor.runtime.SnapshotRepository;
import com.kidxoxo.talenteditor.synthesis.TalentSynthesisService;
import com.kidxoxo.talenteditor.counter.CounterEventService;
import com.kidxoxo.talenteditor.runtime.TalentTriggerService;
import com.kidxoxo.talenteditor.runtime.TalentPresentationService;
import com.kidxoxo.talenteditor.runtime.ItemSubmissionService;
import com.kidxoxo.talenteditor.resource.ClientResourceStatusTracker;
import com.kidxoxo.talenteditor.settings.EditorSettingsService;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.kidxoxo.talenteditor.player.PlayerTalentData;
import com.kidxoxo.talenteditor.item.ModItems;
import com.kidxoxo.talenteditor.item.ModCreativeTabs;
import com.kidxoxo.talenteditor.loot.ModLootModifiers;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

@Mod(TalentEditorMod.MOD_ID)
public final class TalentEditorMod {
    public static final String MOD_ID = "talent_editor";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final TalentCompiler COMPILER = TalentCompiler.withBuiltins();
    public static final SnapshotRepository SNAPSHOTS = new SnapshotRepository();
    public static final EditorPackStorage EDITOR_PACK = new EditorPackStorage();
    public static final DefinitionService DEFINITIONS = new DefinitionService(COMPILER, SNAPSHOTS, EDITOR_PACK);
    public static final TalentDrawService DRAWS = new TalentDrawService();
    public static final TalentSynthesisService SYNTHESES = new TalentSynthesisService();
    public static final CounterEventService COUNTERS = new CounterEventService();
    public static final TalentTriggerService TRIGGERS = new TalentTriggerService();
    public static final TalentPresentationService PRESENTATIONS = new TalentPresentationService();
    public static final ItemSubmissionService SUBMISSIONS = new ItemSubmissionService();
    public static final ClientResourceStatusTracker CLIENT_RESOURCES = new ClientResourceStatusTracker();
    public static final EditorSettingsService SETTINGS = new EditorSettingsService(EDITOR_PACK);

    public TalentEditorMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModLootModifiers.REGISTER.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::addCreativeTabItems);
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            EditorNetwork.register();
            com.kidxoxo.talenteditor.compat.CuriosIntegration.registerConfiguredItem();
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerTalentData.class);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ModCreativeTabs.KEY)) {
            com.kidxoxo.talenteditor.client.ClientCreativeItems.addTo(event);
        }
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DefinitionReloadListener(DEFINITIONS));
    }

    private void registerCommands(RegisterCommandsEvent event) {
        TalentEditorCommands.register(event.getDispatcher());
    }
}
