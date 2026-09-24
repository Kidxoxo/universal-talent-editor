package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class CounterMetadataScreen extends Screen {
    private static final List<String> VISIBILITIES = List.of("full", "condition_only", "hidden");
    private final CounterEditScreen parent;
    private String categoriesValue;
    private String displayCategoryValue;
    private String visibility;
    private EditBox categoriesField;
    private EditBox displayCategoryField;
    private Button visibilityButton;
    private String status = "";

    public CounterMetadataScreen(CounterEditScreen parent) {
        super(Component.translatable("screen.talent_editor.counter.metadata.title"));
        this.parent = parent;
        CounterDraft.MetadataSettings settings = parent.metadataSettings();
        this.categoriesValue = CounterEditScreen.joinIds(settings.categories());
        this.displayCategoryValue = settings.displayCategory() == null ? "" : settings.displayCategory().toString();
        this.visibility = VISIBILITIES.contains(settings.visibility()) ? settings.visibility() : "full";
    }

    @Override
    protected void init() {
        int formWidth = Math.min(600, width - 32);
        int x = (width - formWidth) / 2;
        int chooseWidth = Math.min(84, Math.max(62, formWidth / 6));
        int fieldWidth = formWidth - chooseWidth - 6;
        categoriesField = new EditBox(font, x, 54, fieldWidth, 20,
                Component.translatable("screen.talent_editor.counter.metadata.categories"));
        categoriesField.setMaxLength(4096);
        categoriesField.setValue(categoriesValue);
        categoriesField.setResponder(value -> {
            categoriesValue = value;
            status = "";
        });
        addRenderableWidget(categoriesField);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openCategoryPicker())
                .bounds(x + fieldWidth + 6, 54, chooseWidth, 20).build());
        displayCategoryField = new EditBox(font, x, 96, fieldWidth, 20,
                Component.translatable("screen.talent_editor.counter.metadata.display_category"));
        displayCategoryField.setMaxLength(256);
        displayCategoryField.setValue(displayCategoryValue);
        displayCategoryField.setResponder(value -> {
            displayCategoryValue = value;
            status = "";
        });
        addRenderableWidget(displayCategoryField);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openDisplayCategoryPicker())
                .bounds(x + fieldWidth + 6, 96, chooseWidth, 20).build());
        visibilityButton = addRenderableWidget(Button.builder(visibilityLabel(), button -> cycleVisibility())
                .bounds(x, 138, formWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(x + 86, height - 28, formWidth - 86, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(600, width - 32);
        int x = (width - formWidth) / 2;
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.talent_editor.counter.metadata.categories"),
                x, 43, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.counter.metadata.display_category"),
                x, 85, 0xA0A0A0, false);
        ReferenceDiagnostics.Summary references = ReferenceDiagnostics.summarize(
                parent.bootstrap(), ResourceReferencePickerScreen.Source.COUNTER_CATEGORY,
                categoriesValue, false
        );
        graphics.drawString(font, font.plainSubstrByWidth(
                ReferenceDiagnostics.label(references).getString(), formWidth
        ), x, 167, ReferenceDiagnostics.color(references), false);
        graphics.drawWordWrap(font, Component.translatable(
                "screen.talent_editor.counter.metadata.help"), x, 185, formWidth, 0x808080);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                    x, height - 45, 0xFF7777, false);
        }
    }

    private void cycleVisibility() {
        int index = VISIBILITIES.indexOf(visibility);
        visibility = VISIBILITIES.get((index + 1) % VISIBILITIES.size());
        visibilityButton.setMessage(visibilityLabel());
    }

    private Component visibilityLabel() {
        return Component.translatable("screen.talent_editor.counter.metadata.visibility",
                Component.translatable("screen.talent_editor.counter.visibility." + visibility));
    }

    private void openCategoryPicker() {
        if (minecraft != null) {
            minecraft.setScreen(new ResourceReferencePickerScreen(
                    this, ResourceReferencePickerScreen.Source.COUNTER_CATEGORY,
                    parent.bootstrap(), this::chooseCategory
            ));
        }
    }

    private void chooseCategory(ResourceLocation id) {
        categoriesValue = TalentMetadataScreen.appendUniqueId(categoriesValue, id);
        categoriesField.setValue(categoriesValue);
    }

    private void openDisplayCategoryPicker() {
        if (minecraft != null) {
            minecraft.setScreen(new ResourceReferencePickerScreen(
                    this, ResourceReferencePickerScreen.Source.CATEGORY,
                    parent.bootstrap(), this::chooseDisplayCategory
            ));
        }
    }

    private void chooseDisplayCategory(ResourceLocation id) {
        displayCategoryValue = id.toString();
        displayCategoryField.setValue(displayCategoryValue);
    }

    private void save() {
        List<ResourceLocation> categories = CounterEditScreen.parseIds(categoriesField.getValue());
        if (categories == null) {
            status = Component.translatable("screen.talent_editor.counter.metadata.invalid_categories").getString();
            return;
        }
        ResourceLocation displayCategory = displayCategoryField.getValue().isBlank()
                ? null : ResourceLocation.tryParse(displayCategoryField.getValue().strip());
        if (!displayCategoryField.getValue().isBlank() && displayCategory == null) {
            status = Component.translatable(
                    "screen.talent_editor.counter.metadata.invalid_display_category").getString();
            return;
        }
        parent.updateMetadataSettings(new CounterDraft.MetadataSettings(categories, visibility, displayCategory));
        returnParent();
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
