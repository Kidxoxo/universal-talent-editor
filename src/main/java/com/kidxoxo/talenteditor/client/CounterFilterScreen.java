package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class CounterFilterScreen extends Screen {
    private final CounterEditScreen parent;
    private EditBox entityTypes;
    private EditBox entityTags;
    private EditBox items;
    private EditBox itemTags;
    private EditBox damageTags;
    private String entityTypesValue;
    private String entityTagsValue;
    private String itemsValue;
    private String itemTagsValue;
    private String damageTagsValue;
    private String status = "";

    public CounterFilterScreen(CounterEditScreen parent) {
        super(Component.translatable("screen.talent_editor.counter.filters.title"));
        this.parent = parent;
        CounterDraft.Filters filters = parent.filters();
        this.entityTypesValue = CounterEditScreen.joinIds(filters.entityTypes());
        this.entityTagsValue = CounterEditScreen.joinIds(filters.entityTags());
        this.itemsValue = CounterEditScreen.joinIds(filters.items());
        this.itemTagsValue = CounterEditScreen.joinIds(filters.itemTags());
        this.damageTagsValue = CounterEditScreen.joinIds(filters.damageTags());
    }

    @Override
    protected void init() {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int chooseWidth = Math.min(84, Math.max(62, formWidth / 7));
        int searchableWidth = formWidth - chooseWidth - 6;
        entityTypes = field(x, 36, searchableWidth, entityTypesValue, value -> entityTypesValue = value);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.ENTITY_TYPE, this::chooseEntityType))
                .bounds(x + searchableWidth + 6, 36, chooseWidth, 20).build());
        entityTags = field(x, 69, searchableWidth, entityTagsValue, value -> entityTagsValue = value);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.ENTITY_TYPE_TAG,
                                this::chooseEntityTag))
                .bounds(x + searchableWidth + 6, 69, chooseWidth, 20).build());
        items = field(x, 102, searchableWidth, itemsValue, value -> itemsValue = value);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.ITEM, this::chooseItem))
                .bounds(x + searchableWidth + 6, 102, chooseWidth, 20).build());
        itemTags = field(x, 135, searchableWidth, itemTagsValue, value -> itemTagsValue = value);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.ITEM_TAG, this::chooseItemTag))
                .bounds(x + searchableWidth + 6, 135, chooseWidth, 20).build());
        damageTags = field(x, 168, searchableWidth, damageTagsValue, value -> damageTagsValue = value);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.DAMAGE_TYPE_TAG,
                                this::chooseDamageTag))
                .bounds(x + searchableWidth + 6, 168, chooseWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(x + 86, height - 28, formWidth - 86, 20).build());
    }

    private EditBox field(int x, int y, int width, String value,
                          java.util.function.Consumer<String> responder) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(4096);
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
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        label(graphics, "screen.talent_editor.counter.entity_types", x, 25);
        label(graphics, "screen.talent_editor.counter.entity_tags", x, 58);
        label(graphics, "screen.talent_editor.counter.items", x, 91);
        label(graphics, "screen.talent_editor.counter.item_tags", x, 124);
        label(graphics, "screen.talent_editor.counter.damage_tags", x, 157);
        ReferenceDiagnostics.Summary references = ReferenceDiagnostics.summarize(
                parent.bootstrap(), ResourceReferencePickerScreen.Source.ENTITY_TYPE, entityTypesValue, false
        ).plus(ReferenceDiagnostics.summarize(
                parent.bootstrap(), ResourceReferencePickerScreen.Source.ENTITY_TYPE_TAG, entityTagsValue, false
        )).plus(ReferenceDiagnostics.summarize(
                parent.bootstrap(), ResourceReferencePickerScreen.Source.ITEM, itemsValue, false
        )).plus(ReferenceDiagnostics.summarize(
                parent.bootstrap(), ResourceReferencePickerScreen.Source.ITEM_TAG, itemTagsValue, false
        )).plus(ReferenceDiagnostics.summarize(
                parent.bootstrap(), ResourceReferencePickerScreen.Source.DAMAGE_TYPE_TAG, damageTagsValue, false
        ));
        String message = status.isBlank() ? ReferenceDiagnostics.label(references).getString() : status;
        graphics.drawString(font, font.plainSubstrByWidth(message, formWidth), x, 194,
                status.isBlank() ? ReferenceDiagnostics.color(references) : 0xFF7777, false);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0, false);
    }

    private void save() {
        List<ResourceLocation> types = CounterEditScreen.parseIds(entityTypes.getValue());
        List<ResourceLocation> entityTagIds = CounterEditScreen.parseIds(entityTags.getValue());
        List<ResourceLocation> itemIds = CounterEditScreen.parseIds(items.getValue());
        List<ResourceLocation> itemTagIds = CounterEditScreen.parseIds(itemTags.getValue());
        List<ResourceLocation> damageTagIds = CounterEditScreen.parseIds(damageTags.getValue());
        if (types == null || entityTagIds == null || itemIds == null || itemTagIds == null || damageTagIds == null) {
            status = Component.translatable("screen.talent_editor.counter.filters.invalid").getString();
            return;
        }
        parent.updateFilters(new CounterDraft.Filters(types, entityTagIds, itemIds, itemTagIds, damageTagIds));
        returnParent();
    }

    private void openPicker(
            ResourceReferencePickerScreen.Source source,
            java.util.function.Consumer<ResourceLocation> selection
    ) {
        if (minecraft != null) {
            minecraft.setScreen(new ResourceReferencePickerScreen(this, source, parent.bootstrap(), selection));
        }
    }

    private void chooseEntityType(ResourceLocation id) {
        entityTypesValue = TalentMetadataScreen.appendUniqueId(entityTypesValue, id);
    }

    private void chooseItem(ResourceLocation id) {
        itemsValue = TalentMetadataScreen.appendUniqueId(itemsValue, id);
    }

    private void chooseEntityTag(ResourceLocation id) {
        entityTagsValue = TalentMetadataScreen.appendUniqueId(entityTagsValue, id);
    }

    private void chooseItemTag(ResourceLocation id) {
        itemTagsValue = TalentMetadataScreen.appendUniqueId(itemTagsValue, id);
    }

    private void chooseDamageTag(ResourceLocation id) {
        damageTagsValue = TalentMetadataScreen.appendUniqueId(damageTagsValue, id);
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
