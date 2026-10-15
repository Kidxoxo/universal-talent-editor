package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public final class TalentMetadataScreen extends EditorScreen {
    private final TalentEditScreen parent;
    private final Screen returnScreen;
    private final TalentDraft.MetadataDraft original;
    private final List<EditorBootstrap.RaritySummary> rarities;
    private boolean enabled;
    private boolean showGeneratedEffects;
    private int rarityIndex;
    private ResourceLocation fallbackRarity;
    private EditBox categoriesField;
    private EditBox iconField;
    private EditBox capacityField;
    private Button enabledButton;
    private Button rarityButton;
    private Button generatedEffectsButton;
    private String categoriesValue;
    private String iconValue;
    private String capacityValue;
    private String status = "";

    public TalentMetadataScreen(TalentEditScreen parent) {
        this(parent, parent);
    }

    TalentMetadataScreen(TalentEditScreen parent, Screen returnScreen) {
        super(Component.translatable("screen.talent_editor.metadata.title"));
        this.parent = parent;
        this.returnScreen = returnScreen;
        this.original = parent.metadata();
        this.rarities = parent.rarities();
        this.enabled = original.enabled();
        this.showGeneratedEffects = original.showGeneratedEffects();
        this.fallbackRarity = original.rarity();
        this.rarityIndex = findRarity(original.rarity());
        this.categoriesValue = joinIds(original.categories());
        this.iconValue = original.icon().map(ResourceLocation::toString).orElse("");
        this.capacityValue = Integer.toString(original.capacityCost());
    }

    @Override
    protected void init() {
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int toggleWidth = (formWidth - gap * 2) / 3;
        enabledButton = addRenderableWidget(EditorWidgets.button(enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
        }).bounds(x, 34, toggleWidth, 20).build());
        rarityButton = addRenderableWidget(EditorWidgets.button(rarityLabel(), button -> openRarityPicker())
                .bounds(x + toggleWidth + gap, 34, toggleWidth, 20).build());
        rarityButton.active = !rarities.isEmpty();
        generatedEffectsButton = addRenderableWidget(EditorWidgets.button(generatedEffectsLabel(), button -> {
            showGeneratedEffects = !showGeneratedEffects;
            button.setMessage(generatedEffectsLabel());
        }).bounds(x + (toggleWidth + gap) * 2,
                34, formWidth - (toggleWidth + gap) * 2, 20).build());

        int chooseWidth = Math.min(84, Math.max(62, formWidth / 6));
        categoriesField = field(x, 70, formWidth - chooseWidth - gap, categoriesValue, 4_096,
                value -> categoriesValue = value);
        categoriesField.setHint(Component.translatable("screen.talent_editor.metadata.categories_hint"));
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openCategoryPicker())
                .bounds(x + formWidth - chooseWidth, 70, chooseWidth, 20).build());

        int capacityWidth = Math.min(130, Math.max(80, formWidth / 4));
        int iconAreaWidth = formWidth - capacityWidth - gap;
        int iconChooseWidth = Math.min(72, Math.max(52, iconAreaWidth / 4));
        iconField = field(x, 105, iconAreaWidth - iconChooseWidth - gap,
                iconValue, 512, value -> iconValue = value);
        iconField.setHint(Component.translatable("screen.talent_editor.metadata.icon_hint"));
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openIconPicker())
                .bounds(x + iconAreaWidth - iconChooseWidth, 105, iconChooseWidth, 20).build());
        capacityField = field(x + formWidth - capacityWidth, 105, capacityWidth,
                capacityValue, 6, value -> capacityValue = value);

        int buttonWidth = Math.max(90, Math.min(160, (formWidth - gap) / 2));
        int total = buttonWidth * 2 + gap;
        int buttonX = x + (formWidth - total) / 2;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnToParent())
                .bounds(buttonX, height - 28, buttonWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.metadata.apply"), EditorTheme.Tone.PRIMARY,
                        button -> apply())
                .bounds(buttonX + buttonWidth + gap, height - 28, buttonWidth, 20).build());
    }

    private EditBox field(int x, int y, int width, String value, int maxLength,
                          java.util.function.Consumer<String> responder) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(next -> {
            responder.accept(next);
            status = "";
        });
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 8, 27, formWidth + 16, 169);
        super.render(graphics, mouseX, mouseY, partialTick);
        int capacityWidth = Math.min(130, Math.max(80, formWidth / 4));
        EditorTheme.title(graphics,font,title,width);
        label(graphics, "screen.talent_editor.metadata.categories", x, 59);
        label(graphics, "screen.talent_editor.metadata.icon", x, 94);
        label(graphics, "screen.talent_editor.metadata.capacity",
                x + formWidth - capacityWidth, 94);
        graphics.drawString(font, font.plainSubstrByWidth(
                Component.translatable("screen.talent_editor.metadata.help").getString(), formWidth
        ), x, 131, EditorTheme.MUTED, false);
        renderIconDiagnostic(graphics, x, 143, formWidth);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                    x, height - 47, EditorTheme.ERROR, false);
        }
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, EditorTheme.MUTED, false);
    }

    private void renderIconDiagnostic(GuiGraphics graphics, int x, int y, int formWidth) {
        if (minecraft == null || height < 220) return;
        ClientResourceDiagnostics.Result result = ClientResourceDiagnostics.inspect(
                iconValue, id -> minecraft.getResourceManager().getResource(id).isPresent()
        );
        if (result.state() == ClientResourceDiagnostics.State.EMPTY) return;
        Component message;
        int color;
        if (result.state() == ClientResourceDiagnostics.State.AVAILABLE) {
            message = Component.translatable("screen.talent_editor.resource.texture_available", result.id());
            color = 0xFF77DD88;
        } else if (result.state() == ClientResourceDiagnostics.State.MISSING) {
            message = Component.translatable("screen.talent_editor.resource.texture_missing", result.id());
            color = 0xFFFFAA55;
        } else {
            message = Component.translatable("screen.talent_editor.resource.texture_invalid");
            color = 0xFFFF7777;
        }
        graphics.drawString(font, font.plainSubstrByWidth(message.getString(), formWidth), x, y, color, false);
    }

    private Component enabledLabel() {
        return Component.translatable(enabled
                ? "screen.talent_editor.metadata.enabled_yes"
                : "screen.talent_editor.metadata.enabled_no");
    }

    private Component generatedEffectsLabel() {
        return Component.translatable(showGeneratedEffects
                ? "screen.talent_editor.metadata.generated_yes"
                : "screen.talent_editor.metadata.generated_no");
    }

    private Component rarityLabel() {
        if (rarityIndex >= 0 && rarityIndex < rarities.size()) {
            EditorBootstrap.RaritySummary rarity = rarities.get(rarityIndex);
            return Component.translatable("screen.talent_editor.metadata.rarity_value", rarity.name());
        }
        return Component.translatable("screen.talent_editor.metadata.rarity_value", fallbackRarity.toString());
    }

    private void apply() {
        List<ResourceLocation> categories = parseIds(categoriesField.getValue(),
                "screen.talent_editor.metadata.invalid_category");
        if (categories == null) return;
        Optional<ResourceLocation> icon = Optional.empty();
        String rawIcon = iconField.getValue().strip();
        if (!rawIcon.isBlank()) {
            ResourceLocation parsedIcon = ResourceLocation.tryParse(rawIcon);
            if (parsedIcon == null) {
                status = Component.translatable("screen.talent_editor.metadata.invalid_icon", rawIcon).getString();
                return;
            }
            icon = Optional.of(parsedIcon);
        }
        int capacity;
        try {
            capacity = Integer.parseInt(capacityField.getValue().strip());
            if (capacity < 0 || capacity > 10_000) throw new NumberFormatException();
        } catch (NumberFormatException ignored) {
            status = Component.translatable("screen.talent_editor.metadata.invalid_capacity").getString();
            return;
        }

        parent.updateMetadata(
                enabled, currentRarity(), showGeneratedEffects,
                categories, icon, capacity
        );
        returnToParent();
    }

    private void openCategoryPicker() {
        if (minecraft != null) {
            minecraft.setScreen(new ResourceReferencePickerScreen(
                    this, ResourceReferencePickerScreen.Source.CATEGORY, parent.bootstrap(), this::chooseCategory
            ));
        }
    }

    private void openRarityPicker() {
        if (minecraft != null && !rarities.isEmpty()) {
            minecraft.setScreen(new ResourceReferencePickerScreen(
                    this, ResourceReferencePickerScreen.Source.RARITY, parent.bootstrap(), this::chooseRarity
            ));
        }
    }

    private void openIconPicker() {
        if (minecraft != null) {
            minecraft.setScreen(new ResourceReferencePickerScreen(
                    this, ResourceReferencePickerScreen.Source.TEXTURE, parent.bootstrap(), this::chooseIcon
            ));
        }
    }

    private void chooseRarity(ResourceLocation id) {
        fallbackRarity = id;
        rarityIndex = findRarity(id);
        status = "";
    }

    private void chooseCategory(ResourceLocation id) {
        categoriesValue = appendUniqueId(categoriesValue, id);
    }

    private void chooseIcon(ResourceLocation id) {
        iconValue = id.toString();
        status = "";
    }

    private List<ResourceLocation> parseIds(String value, String errorKey) {
        if (value.isBlank()) return List.of();
        ArrayList<ResourceLocation> result = new ArrayList<>();
        HashSet<ResourceLocation> seen = new HashSet<>();
        for (String raw : value.split(",")) {
            String item = raw.strip();
            ResourceLocation id = ResourceLocation.tryParse(item);
            if (id == null || !seen.add(id)) {
                status = Component.translatable(errorKey, item).getString();
                return null;
            }
            result.add(id);
        }
        return List.copyOf(result);
    }

    private int findRarity(ResourceLocation rarity) {
        for (int index = 0; index < rarities.size(); index++) {
            if (rarities.get(index).id().equals(rarity)) return index;
        }
        return -1;
    }

    private ResourceLocation currentRarity() {
        return rarityIndex >= 0 && rarityIndex < rarities.size()
                ? rarities.get(rarityIndex).id() : fallbackRarity;
    }

    private void returnToParent() {
        if (minecraft != null) minecraft.setScreen(returnScreen);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private static String joinIds(List<ResourceLocation> ids) {
        return ids.stream().map(ResourceLocation::toString)
                .reduce((left, right) -> left + ", " + right).orElse("");
    }

    static String appendUniqueId(String value, ResourceLocation id) {
        for (String raw : value.split(",")) {
            if (raw.strip().equals(id.toString())) return value;
        }
        return value.isBlank() ? id.toString() : value.strip() + ", " + id;
    }
}
