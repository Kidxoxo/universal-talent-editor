package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class TalentOfferScreen extends Screen {
    private final TalentEditScreen parent;
    private boolean enabled;
    private Button enabledButton;
    private EditBox weightField;
    private EditBox poolsField;
    private String weightValue;
    private String poolsValue;
    private String status = "";

    public TalentOfferScreen(TalentEditScreen parent) {
        super(Component.translatable("screen.talent_editor.offer.title"));
        this.parent = parent;
        TalentDraft.OfferDraft offer = parent.offer();
        this.enabled = offer.enabled();
        this.weightValue = Double.toString(offer.weight());
        this.poolsValue = offer.pools().isEmpty()
                ? "talent_editor:default"
                : String.join(", ", offer.pools().stream().map(ResourceLocation::toString).toList());
    }

    @Override
    protected void init() {
        int formWidth = Math.min(460, width - 32);
        int x = (width - formWidth) / 2;
        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enabled = !enabled;
            enabledButton.setMessage(enabledLabel());
        }).bounds(x, 55, formWidth, 20).build());

        weightField = new EditBox(font, x, 98, formWidth, 20,
                Component.translatable("screen.talent_editor.offer.weight"));
        weightField.setMaxLength(48);
        weightField.setValue(weightValue);
        weightField.setResponder(value -> {
            weightValue = value;
            status = "";
        });
        addRenderableWidget(weightField);

        int chooseWidth = Math.min(84, Math.max(62, formWidth / 5));
        poolsField = new EditBox(font, x, 141, formWidth - chooseWidth - 6, 20,
                Component.translatable("screen.talent_editor.offer.pools"));
        poolsField.setMaxLength(2_048);
        poolsField.setValue(poolsValue);
        poolsField.setResponder(value -> {
            poolsValue = value;
            status = "";
        });
        addRenderableWidget(poolsField);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPoolPicker())
                .bounds(x + formWidth - chooseWidth, 141, chooseWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.offer.apply"),
                        button -> apply())
                .bounds(x, height - 28, (formWidth - 6) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x + (formWidth + 6) / 2, height - 28, (formWidth - 6) / 2, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(460, width - 32);
        int x = (width - formWidth) / 2;
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.talent_editor.offer.enabled"),
                x, 44, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.offer.weight"),
                x, 87, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.offer.pools"),
                x, 130, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.offer.pool_hint"),
                x, 169, 0x808080, false);
        ReferenceDiagnostics.Summary references = ReferenceDiagnostics.summarize(
                parent.bootstrap(), ResourceReferencePickerScreen.Source.POOL, poolsValue, false
        );
        graphics.drawString(font, font.plainSubstrByWidth(
                ReferenceDiagnostics.label(references).getString(), formWidth
        ), x, 181, ReferenceDiagnostics.color(references), false);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                x, 193, 0xFF7777, false);
    }

    private void apply() {
        double weight;
        try {
            weight = Double.parseDouble(weightField.getValue().strip());
            if (!Double.isFinite(weight) || weight < 0.0D || weight > 1.0E9D) throw new NumberFormatException();
        } catch (NumberFormatException ignored) {
            status = Component.translatable("screen.talent_editor.offer.invalid_weight").getString();
            return;
        }
        LinkedHashSet<ResourceLocation> pools = new LinkedHashSet<>();
        for (String raw : poolsField.getValue().split(",")) {
            if (raw.isBlank()) continue;
            ResourceLocation id = ResourceLocation.tryParse(raw.strip());
            if (id == null) {
                status = Component.translatable("screen.talent_editor.offer.invalid_pool", raw.strip()).getString();
                return;
            }
            pools.add(id);
        }
        parent.updateOffer(enabled, weight, new ArrayList<>(pools));
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private Component enabledLabel() {
        return Component.translatable(enabled
                ? "screen.talent_editor.offer.enabled_yes"
                : "screen.talent_editor.offer.enabled_no");
    }

    private void openPoolPicker() {
        if (minecraft != null) {
            minecraft.setScreen(new ResourceReferencePickerScreen(
                    this, ResourceReferencePickerScreen.Source.POOL, parent.bootstrap(), this::choosePool
            ));
        }
    }

    private void choosePool(ResourceLocation id) {
        poolsValue = TalentMetadataScreen.appendUniqueId(poolsValue, id);
    }
}
