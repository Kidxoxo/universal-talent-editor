package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.catalog.AttributeDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class ResourceReferencePickerScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int MAX_ENTRIES = 20_000;
    private final Screen parent;
    private final Source source;
    private final Consumer<ResourceLocation> selection;
    private final List<Entry> entries;
    private EditBox search;
    private int scroll;

    public ResourceReferencePickerScreen(
            Screen parent,
            Source source,
            EditorBootstrap bootstrap,
            Consumer<ResourceLocation> selection
    ) {
        this(parent, source, bootstrap, List.of(), selection);
    }

    public ResourceReferencePickerScreen(
            Screen parent,
            Source source,
            EditorBootstrap bootstrap,
            List<ResourceLocation> additionalIds,
            Consumer<ResourceLocation> selection
    ) {
        super(Component.translatable("screen.talent_editor.reference_picker.title",
                Component.translatable("screen.talent_editor.reference_picker.source." + source.serializedName())));
        this.parent = parent;
        this.source = source;
        this.selection = selection;
        this.entries = buildEntries(source, bootstrap, additionalIds);
    }

    @Override
    protected void init() {
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        search = new EditBox(font, x, 42, formWidth, 20,
                Component.translatable("screen.talent_editor.reference_picker.search"));
        search.setHint(Component.translatable("screen.talent_editor.reference_picker.search"));
        search.setResponder(ignored -> scroll = 0);
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(x, height - 28, 76, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.reference_picker.origin." + source.originName(), entries.size()
        ), x, 66, 0xFF909090, false);
        int top = 84;
        List<Entry> visible = filtered();
        int rows = Math.max(1, (height - top - 39) / ROW_HEIGHT);
        scroll = clamp(scroll, visible.size(), rows);
        int end = Math.min(visible.size(), scroll + rows);
        for (int index = scroll; index < end; index++) {
            Entry entry = visible.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + formWidth && mouseY >= y - 3 && mouseY < y + 15;
            if (hovered) graphics.fill(x, y - 3, x + formWidth, y + 15, 0x504A90E2);
            String state = "[" + entry.status().label().getString() + "] ";
            int stateWidth = font.width(state);
            graphics.drawString(font, state, x + 4, y, entry.status().color(), false);
            graphics.drawString(font, font.plainSubstrByWidth(
                    entry.id() + " — " + entry.label(), formWidth - stateWidth - 8),
                    x + 4 + stateWidth, y, 0xFFE8E8E8, false);
        }
        if (visible.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable(
                    "screen.talent_editor.reference_picker.empty"), width / 2, height / 2, 0x808080);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int formWidth = Math.min(620, width - 32);
            int x = (width - formWidth) / 2;
            int top = 84;
            if (mouseX >= x && mouseX < x + formWidth && mouseY >= top && mouseY < height - 39) {
                int index = scroll + (int) ((mouseY - top) / ROW_HEIGHT);
                List<Entry> visible = filtered();
                if (index >= 0 && index < visible.size()) {
                    selection.accept(visible.get(index).id());
                    returnParent();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rows = Math.max(1, (height - 84 - 39) / ROW_HEIGHT);
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, filtered().size(), rows);
        return true;
    }

    @Override
    public void onClose() {
        returnParent();
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private List<Entry> filtered() {
        String query = search == null ? "" : search.getValue();
        if (query.isBlank()) return entries;
        return entries.stream().filter(entry -> matchesSearch(entry.id(), entry.label(), query)).toList();
    }

    static boolean matchesSearch(ResourceLocation id, String localizedLabel, String query) {
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return true;
        return (id + " " + (localizedLabel == null ? "" : localizedLabel))
                .toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static List<Entry> buildEntries(
            Source source, EditorBootstrap bootstrap, List<ResourceLocation> additionalIds
    ) {
        ArrayList<Entry> result = new ArrayList<>();
        switch (source) {
            case TALENT -> bootstrap.talents().stream().limit(MAX_ENTRIES).forEach(talent -> result.add(new Entry(
                    talent.id(), talent.name(), ReferenceDiagnostics.status(talent.enabled(), talent.available())
            )));
            case COUNTER -> bootstrap.counters().stream().limit(MAX_ENTRIES).forEach(counter -> result.add(new Entry(
                    counter.id(), counter.name(), ReferenceDiagnostics.status(counter.enabled(), counter.available())
            )));
            case NUMERIC_STATE -> {
                LinkedHashSet<ResourceLocation> states = new LinkedHashSet<>(bootstrap.numericStates());
                states.addAll(additionalIds);
                String label = Component.translatable(
                        "screen.talent_editor.reference_picker.numeric_state_defined"
                ).getString();
                states.stream().sorted(Comparator.comparing(ResourceLocation::toString)).limit(MAX_ENTRIES)
                        .forEach(id -> result.add(new Entry(
                                id, label, ReferenceDiagnostics.Status.AVAILABLE
                        )));
            }
            case COUNTER_CATEGORY -> {
                Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
                bootstrap.counters().forEach(counter -> counter.categories().forEach(category ->
                        counts.merge(category, 1, Integer::sum)));
                counts.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                        .limit(MAX_ENTRIES).forEach(entry -> result.add(new Entry(
                                entry.getKey(), Component.translatable(
                                        "screen.talent_editor.reference_picker.counter_category_usage", entry.getValue()
                                ).getString(), ReferenceDiagnostics.Status.AVAILABLE
                        )));
            }
            case CATEGORY -> {
                Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
                bootstrap.talents().forEach(talent -> talent.categories().forEach(category ->
                        counts.merge(category, 1, Integer::sum)));
                counts.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                        .limit(MAX_ENTRIES).forEach(entry -> result.add(new Entry(
                                entry.getKey(), Component.translatable(
                                        "screen.talent_editor.reference_picker.category_usage", entry.getValue()
                                ).getString(), ReferenceDiagnostics.Status.AVAILABLE
                        )));
            }
            case ITEM -> {
                LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
                bootstrap.configuredItems().stream()
                        .sorted(Comparator.comparing(value -> value.id().toString()))
                        .limit(MAX_ENTRIES)
                        .forEach(item -> {
                            ids.add(item.id());
                            result.add(new Entry(
                                    item.id(), item.name(),
                                    ReferenceDiagnostics.status(item.enabled(), item.available())
                            ));
                        });
                ForgeRegistries.ITEMS.getKeys().stream()
                        .sorted(Comparator.comparing(ResourceLocation::toString))
                        .filter(ids::add)
                        .limit(Math.max(0, MAX_ENTRIES - result.size()))
                        .forEach(id -> {
                            Item item = ForgeRegistries.ITEMS.getValue(id);
                            result.add(new Entry(id, item == null ? "" : item.getDescription().getString(),
                                    item == null ? ReferenceDiagnostics.Status.NOT_PRESENT
                                            : ReferenceDiagnostics.Status.AVAILABLE));
                        });
            }
            case BLOCK -> ForgeRegistries.BLOCKS.getKeys().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString)).limit(MAX_ENTRIES)
                    .forEach(id -> {
                        Block block = ForgeRegistries.BLOCKS.getValue(id);
                        result.add(new Entry(id, block == null ? "" : block.getName().getString(),
                                block == null ? ReferenceDiagnostics.Status.NOT_PRESENT
                                        : ReferenceDiagnostics.Status.AVAILABLE));
                    });
            case SOUND_EVENT -> addClientResources(result, source);
            case TEXTURE -> addClientResources(result, source);
            case POOL -> bootstrap.pools().stream().limit(MAX_ENTRIES).forEach(pool -> result.add(new Entry(
                    pool.id(), pool.name(), pool.enabled()
                            ? ReferenceDiagnostics.Status.AVAILABLE : ReferenceDiagnostics.Status.DISABLED
            )));
            case RARITY -> bootstrap.rarities().stream().limit(MAX_ENTRIES).forEach(rarity -> result.add(new Entry(
                    rarity.id(), rarity.name(), rarity.enabled()
                            ? ReferenceDiagnostics.Status.AVAILABLE : ReferenceDiagnostics.Status.DISABLED
            )));
            case MOB_EFFECT -> ForgeRegistries.MOB_EFFECTS.getKeys().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString)).limit(MAX_ENTRIES)
                    .forEach(id -> {
                        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
                        result.add(new Entry(id,
                                effect == null ? "" : Component.translatable(effect.getDescriptionId()).getString(),
                                effect == null ? ReferenceDiagnostics.Status.NOT_PRESENT
                                        : ReferenceDiagnostics.Status.AVAILABLE));
                    });
            case ATTRIBUTE -> bootstrap.attributes().stream().limit(MAX_ENTRIES).forEach(attribute -> result.add(
                    new Entry(attribute.id(), attributeName(attribute), attribute.attachedToTarget()
                            ? ReferenceDiagnostics.Status.AVAILABLE : ReferenceDiagnostics.Status.UNAVAILABLE)
            ));
            case ENTITY_TYPE -> ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString)).limit(MAX_ENTRIES)
                    .forEach(id -> {
                        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
                        result.add(new Entry(id, entityType == null ? "" : entityType.getDescription().getString(),
                                entityType == null ? ReferenceDiagnostics.Status.NOT_PRESENT
                                        : ReferenceDiagnostics.Status.AVAILABLE));
                    });
            case ENTITY_TYPE_TAG -> addTags(result, bootstrap.entityTypeTags());
            case ITEM_TAG -> addTags(result, bootstrap.itemTags());
            case DAMAGE_TYPE_TAG -> addTags(result, bootstrap.damageTypeTags());
            case BLOCK_TAG -> addTags(result, clientBlockTags(), "client_tag");
        }
        return List.copyOf(result);
    }

    private static void addTags(List<Entry> result, List<ResourceLocation> tags) {
        addTags(result, tags, "server_tag");
    }

    private static void addTags(List<Entry> result, List<ResourceLocation> tags, String labelType) {
        String label = Component.translatable("screen.talent_editor.reference_picker." + labelType).getString();
        tags.stream().limit(MAX_ENTRIES).forEach(id -> result.add(new Entry(
                id, label, ReferenceDiagnostics.Status.AVAILABLE
        )));
    }

    private static void addClientResources(List<Entry> result, Source source) {
        Minecraft minecraft = Minecraft.getInstance();
        LinkedHashSet<ResourceLocation> loaded = new LinkedHashSet<>();
        if (minecraft != null) {
            if (source == Source.TEXTURE) {
                loaded.addAll(minecraft.getResourceManager().listResources(
                        "textures", id -> id.getPath().endsWith(".png")
                ).keySet());
            } else {
                loaded.addAll(minecraft.getSoundManager().getAvailableSounds());
                loaded.addAll(ForgeRegistries.SOUND_EVENTS.getKeys());
            }
        }
        LinkedHashSet<ResourceLocation> managed = new LinkedHashSet<>();
        if (minecraft != null) {
            ManagedClientResourcePack pack = new ManagedClientResourcePack(minecraft.gameDirectory.toPath());
            managed.addAll(pack.managedResources(source == Source.TEXTURE
                    ? ManagedClientResourcePack.ImportKind.TEXTURE
                    : ManagedClientResourcePack.ImportKind.SOUND));
        }
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>(loaded);
        ids.addAll(managed);
        String loadedLabel = Component.translatable(
                "screen.talent_editor.reference_picker.client_resource_loaded").getString();
        String pendingLabel = Component.translatable(
                "screen.talent_editor.reference_picker.client_resource_managed_pending").getString();
        ids.stream().sorted(Comparator.comparing(ResourceLocation::toString)).limit(MAX_ENTRIES)
                .forEach(id -> result.add(new Entry(
                        id, loaded.contains(id) ? loadedLabel : pendingLabel,
                        loaded.contains(id) ? ReferenceDiagnostics.Status.AVAILABLE
                                : ReferenceDiagnostics.Status.UNAVAILABLE
                )));
    }

    static ReferenceDiagnostics.Status clientResourceStatus(Source source, ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return ReferenceDiagnostics.Status.NOT_PRESENT;
        boolean loaded = source == Source.TEXTURE
                ? minecraft.getResourceManager().getResource(id).isPresent()
                : minecraft.getSoundManager().getSoundEvent(id) != null
                || ForgeRegistries.SOUND_EVENTS.containsKey(id);
        if (loaded) return ReferenceDiagnostics.Status.AVAILABLE;
        ManagedClientResourcePack pack = new ManagedClientResourcePack(minecraft.gameDirectory.toPath());
        return pack.managedResources(source == Source.TEXTURE
                        ? ManagedClientResourcePack.ImportKind.TEXTURE
                        : ManagedClientResourcePack.ImportKind.SOUND).contains(id)
                ? ReferenceDiagnostics.Status.UNAVAILABLE : ReferenceDiagnostics.Status.NOT_PRESENT;
    }

    static List<ResourceLocation> clientBlockTags() {
        return ForgeRegistries.BLOCKS.getKeys().stream()
                .map(ForgeRegistries.BLOCKS::getValue)
                .filter(java.util.Objects::nonNull)
                .flatMap(block -> block.builtInRegistryHolder().tags())
                .map(tag -> tag.location())
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .limit(MAX_ENTRIES)
                .toList();
    }

    private static String attributeName(AttributeDescriptor attribute) {
        String translated = Component.translatable(attribute.descriptionId()).getString();
        return translated.equals(attribute.descriptionId()) ? attribute.descriptionId() : translated;
    }

    private static int clamp(int value, int size, int rows) {
        return Math.max(0, Math.min(value, Math.max(0, size - rows)));
    }

    public enum Source {
        TALENT("talent", "server_definition"),
        COUNTER("counter", "server_definition"),
        NUMERIC_STATE("numeric_state", "defined_state"),
        COUNTER_CATEGORY("counter_category", "derived_counter_category"),
        CATEGORY("category", "derived_category"),
        ITEM("item", "registry_or_definition"),
        BLOCK("block", "client_registry"),
        SOUND_EVENT("sound_event", "client_resources"),
        TEXTURE("texture", "client_resources"),
        POOL("pool", "server_definition"),
        RARITY("rarity", "server_definition"),
        MOB_EFFECT("mob_effect", "client_registry"),
        ATTRIBUTE("attribute", "server_definition"),
        ENTITY_TYPE("entity_type", "client_registry"),
        ENTITY_TYPE_TAG("entity_type_tag", "server_tag"),
        ITEM_TAG("item_tag", "server_tag"),
        DAMAGE_TYPE_TAG("damage_type_tag", "server_tag"),
        BLOCK_TAG("block_tag", "client_registry");

        private final String serializedName;
        private final String originName;

        Source(String serializedName, String originName) {
            this.serializedName = serializedName;
            this.originName = originName;
        }

        public String serializedName() {
            return serializedName;
        }

        public String originName() {
            return originName;
        }
    }

    private record Entry(ResourceLocation id, String label, ReferenceDiagnostics.Status status) {
    }
}
