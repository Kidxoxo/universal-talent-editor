package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class ClientResourceImportScreen extends Screen {
    /** Survives closing and reopening this screen until the generated pack is successfully reloaded. */
    private static final Set<String> PENDING_RESOURCES = new LinkedHashSet<>();
    private final Screen parent;
    private final ResourceLocation configuredItemId;
    private final int configuredItemModelData;
    private ManagedClientResourcePack pack;
    private ManagedClientResourcePack.ImportKind kind = ManagedClientResourcePack.ImportKind.TEXTURE;
    private ManagedClientResourcePack.AuditResult audit;
    private String sourceValue = "";
    private String resourceValue = "example:textures/talent_icons/1.png";
    private boolean stream;
    private boolean reloading;
    private int reloadingCount;
    private EditBox sourceField;
    private EditBox resourceField;
    private Button kindButton;
    private Button streamButton;
    private Button importButton;
    private Button reloadButton;
    private Button auditButton;
    private Component status = Component.empty();
    private int statusColor = EditorTheme.MUTED;

    public ClientResourceImportScreen(Screen parent) {
        this(parent, null, 0);
    }

    public ClientResourceImportScreen(Screen parent, ResourceLocation configuredItemId, int customModelData) {
        super(Component.translatable("screen.talent_editor.resource_import.title"));
        this.parent = parent;
        this.configuredItemId = configuredItemId;
        this.configuredItemModelData = customModelData;
        if (configuredItemId != null) {
            this.kind = ManagedClientResourcePack.ImportKind.TEXTURE;
            this.resourceValue = configuredItemId.getNamespace() + ":textures/item/"
                    + configuredItemId.getPath() + ".png";
        }
    }

    @Override
    protected void init() {
        if (pack == null) pack = new ManagedClientResourcePack(minecraft.gameDirectory.toPath());
        if (audit == null) audit = pack.audit();
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int half = (formWidth - gap) / 2;
        kindButton = addRenderableWidget(Button.builder(kindLabel(), button -> toggleKind())
                .bounds(x, 34, half, 20).build());
        streamButton = addRenderableWidget(Button.builder(streamLabel(), button -> {
            stream = !stream;
            button.setMessage(streamLabel());
        }).bounds(x + half + gap, 34, formWidth - half - gap, 20).build());

        sourceField = field(x, 72, formWidth, sourceValue, 2_048, value -> sourceValue = value);
        sourceField.setHint(Component.translatable("screen.talent_editor.resource_import.source_hint"));
        resourceField = field(x, 110, formWidth, resourceValue, 256, value -> resourceValue = value);
        resourceField.setHint(resourceHint());

        int third = (formWidth - gap * 2) / 3;
        importButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.resource_import.import_only"),
                        button -> importOnly())
                .bounds(x, 140, third, 20).build());
        reloadButton = addRenderableWidget(Button.builder(reloadLabel(), button -> enableAndReloadPack())
                .bounds(x + third + gap, 140, third, 20).build());
        auditButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.resource_import.audit"),
                        button -> refreshAudit())
                .bounds(x + (third + gap) * 2, 140,
                        formWidth - (third + gap) * 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnParent())
                .bounds(width / 2 - 55, height - 28, 110, 20).build());
        updateWidgets();
    }

    private EditBox field(
            int x, int y, int width, String value, int maximum,
            java.util.function.Consumer<String> responder
    ) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maximum);
        field.setValue(value);
        field.setResponder(responder);
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 24, formWidth + 14, Math.max(1, height - 60));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        label(graphics, x, 61, "source");
        label(graphics, x, 99, kind == ManagedClientResourcePack.ImportKind.TEXTURE
                ? "texture_id" : "sound_id");

        int y = 171;
        Component auditText;
        int auditColor;
        if (audit == null || !audit.valid()) {
            auditText = Component.translatable("screen.talent_editor.resource_import.audit_invalid",
                    audit == null ? "" : audit.detail());
            auditColor = 0xFFFF7777;
        } else {
            auditText = Component.translatable("screen.talent_editor.resource_import.audit_result",
                    audit.total(), audit.missing(), audit.changed());
            auditColor = audit.missing() == 0 && audit.changed() == 0 ? 0xFF77DD88 : 0xFFFFAA55;
        }
        graphics.drawString(font, font.plainSubstrByWidth(auditText.getString(), formWidth),
                x, y, auditColor, false);
        var serverStatus = ClientEditorState.resourceStatus();
        Component serverText = serverStatus == null
                ? Component.translatable("screen.talent_editor.resource_import.server_unknown")
                : Component.translatable("screen.talent_editor.resource_import.server_result",
                serverStatus.availableCount(), serverStatus.statuses().size(), serverStatus.missingCount(),
                ClientEditorState.resourceRequirements() != null
                        && ClientEditorState.resourceRequirements().truncated()
                        ? Component.translatable("screen.talent_editor.resource_import.server_truncated")
                        : Component.empty());
        int serverColor = serverStatus != null && serverStatus.missingCount() == 0
                ? 0xFF77DD88 : 0xFFFFAA55;
        graphics.drawString(font, font.plainSubstrByWidth(serverText.getString(), formWidth),
                x, y + 13, serverColor, false);
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                        "screen.talent_editor.resource_import.root", pack.root()).getString(), formWidth),
                x, y + 26, EditorTheme.MUTED, false);
        if (height >= 300) {
            graphics.drawWordWrap(font, Component.translatable("screen.talent_editor.resource_import.help"),
                    x, y + 44, formWidth, EditorTheme.MUTED);
            if (!status.getString().isBlank()) {
                graphics.drawWordWrap(font, status, x, y + 74, formWidth, statusColor);
            }
        } else if (!status.getString().isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status.getString(), formWidth),
                    x, y + 39, statusColor, false);
        }
    }

    private void label(GuiGraphics graphics, int x, int y, String suffix) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.resource_import." + suffix),
                x, y, 0xFFA0A0A0, false);
    }

    private void toggleKind() {
        kind = kind == ManagedClientResourcePack.ImportKind.TEXTURE
                ? ManagedClientResourcePack.ImportKind.SOUND
                : ManagedClientResourcePack.ImportKind.TEXTURE;
        if (kind == ManagedClientResourcePack.ImportKind.TEXTURE
                && resourceValue.equals("example:talent.acquire")) {
            resourceValue = "example:textures/talent_icons/1.png";
        } else if (kind == ManagedClientResourcePack.ImportKind.SOUND
                && resourceValue.equals("example:textures/talent_icons/1.png")) {
            resourceValue = "example:talent.acquire";
        }
        resourceField.setValue(resourceValue);
        resourceField.setHint(resourceHint());
        status = Component.empty();
        updateWidgets();
    }

    private void importOnly() {
        if (reloading) return;
        String rawId = resourceField.getValue().strip();
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null || !rawId.contains(":")) {
            setStatus("invalid_target", 0xFFFF7777, rawId);
            return;
        }
        Path source;
        try {
            String rawSource = unquote(sourceField.getValue());
            source = Path.of(rawSource);
        } catch (IllegalArgumentException exception) {
            setStatus("source_missing", 0xFFFF7777, sourceField.getValue());
            return;
        }
        ManagedClientResourcePack.ImportResult result = pack.importFile(source, id, kind, stream);
        audit = pack.audit();
        if (!result.successful()) {
            setStatus(result.code().name().toLowerCase(), 0xFFFF7777, result.detail());
            return;
        }
        String pendingKey = kind.name() + ":" + result.resource();
        int pendingCount = PENDING_RESOURCES.size() + (PENDING_RESOURCES.contains(pendingKey) ? 0 : 1);
        PENDING_RESOURCES.add(pendingKey);
        if (configuredItemId != null) {
            ManagedClientResourcePack.IconBindingResult binding = pack.bindConfiguredItemIcon(
                    configuredItemId, configuredItemModelData, result.resource()
            );
            if (!binding.successful()) {
                setStatus("item_icon_binding", 0xFFFF7777, binding.detail());
                return;
            }
        }
        status = Component.translatable("screen.talent_editor.resource_import.imported",
                result.resource(), result.bytes(), shortHash(result.sha256()),
                pendingCount);
        statusColor = 0xFF77DD88;
        updateWidgets();
    }

    private void enableAndReloadPack() {
        PackRepository repository = minecraft.getResourcePackRepository();
        repository.reload();
        Optional<String> packId = repository.getAvailableIds().stream()
                .filter(id -> id.equals(ManagedClientResourcePack.PREFERRED_PACK_ID))
                .findFirst()
                .or(() -> repository.getAvailableIds().stream()
                        .filter(id -> id.endsWith("/" + ManagedClientResourcePack.DIRECTORY_NAME)).findFirst());
        if (packId.isEmpty() || !repository.getSelectedIds().contains(packId.orElseThrow())
                && !repository.addPack(packId.orElseThrow())) {
            setStatus("pack_unavailable", 0xFFFF7777, ManagedClientResourcePack.DIRECTORY_NAME);
            return;
        }
        // Options#updateResourcePacks starts its own unobservable reload when the selection changes.
        // Mirror its persisted selection update so this screen owns exactly one reload future.
        minecraft.options.resourcePacks.clear();
        minecraft.options.incompatibleResourcePacks.clear();
        for (Pack selected : repository.getSelectedPacks()) {
            if (selected.isFixedPosition()) continue;
            minecraft.options.resourcePacks.add(selected.getId());
            if (!selected.getCompatibility().isCompatible()) {
                minecraft.options.incompatibleResourcePacks.add(selected.getId());
            }
        }
        minecraft.options.save();
        reloading = true;
        reloadingCount = PENDING_RESOURCES.size();
        updateWidgets();
        status = Component.translatable("screen.talent_editor.resource_import.reloading");
        statusColor = EditorTheme.ACCENT;
        minecraft.reloadResourcePacks().whenComplete((ignored, error) -> minecraft.execute(() -> {
            reloading = false;
            if (error == null) {
                PENDING_RESOURCES.clear();
                audit = pack.audit();
                ClientPacketHandlers.recheckResourceRequirements();
                status = Component.translatable("screen.talent_editor.resource_import.reload_complete",
                        reloadingCount);
                statusColor = 0xFF77DD88;
            } else {
                setStatus("reload_failed", 0xFFFF7777,
                        error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
            }
            updateWidgets();
        }));
    }

    private void refreshAudit() {
        audit = pack.audit();
        status = Component.translatable("screen.talent_editor.resource_import.audit_refreshed");
        statusColor = audit.valid() && audit.missing() == 0 && audit.changed() == 0
                ? 0xFF77DD88 : 0xFFFFAA55;
    }

    private void setStatus(String suffix, int color, Object detail) {
        status = Component.translatable("screen.talent_editor.resource_import.error." + suffix, detail);
        statusColor = color;
    }

    private void updateWidgets() {
        if (kindButton == null) return;
        kindButton.setMessage(kindLabel());
        streamButton.setMessage(streamLabel());
        streamButton.active = kind == ManagedClientResourcePack.ImportKind.SOUND && !reloading;
        importButton.active = !reloading;
        reloadButton.setMessage(reloadLabel());
        reloadButton.active = !reloading && audit != null && audit.valid() && audit.total() > 0;
        auditButton.active = !reloading;
        sourceField.setEditable(!reloading);
        resourceField.setEditable(!reloading);
        kindButton.active = !reloading;
        if (configuredItemId != null) kindButton.active = false;
    }

    private Component kindLabel() {
        return Component.translatable("screen.talent_editor.resource_import.kind."
                + kind.name().toLowerCase());
    }

    private Component streamLabel() {
        return Component.translatable("screen.talent_editor.resource_import.stream",
                Component.translatable("options." + (stream ? "on" : "off")));
    }

    private Component reloadLabel() {
        return Component.translatable("screen.talent_editor.resource_import.reload_pending",
                PENDING_RESOURCES.size());
    }

    private Component resourceHint() {
        return Component.translatable("screen.talent_editor.resource_import."
                + (kind == ManagedClientResourcePack.ImportKind.TEXTURE ? "texture_hint" : "sound_hint"));
    }

    private static String unquote(String value) {
        String result = value == null ? "" : value.strip();
        if (result.length() >= 2 && result.startsWith("\"") && result.endsWith("\"")) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }

    private static String shortHash(String hash) {
        return hash.length() <= 12 ? hash : hash.substring(0, 12);
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
