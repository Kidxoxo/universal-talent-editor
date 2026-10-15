package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TalentLifecycleScreen extends EditorScreen {
    private final TalentEditScreen parent;
    private final Screen returnScreen;
    private EditBox duration;
    private EditBox draws;
    private boolean retainEffects;
    private boolean retainCategory;
    private boolean retainTriggers;
    private Button effectsButton;
    private Button categoryButton;
    private Button triggersButton;
    private String status = "";

    TalentLifecycleScreen(TalentEditScreen parent) {
        this(parent, parent);
    }

    TalentLifecycleScreen(TalentEditScreen parent, Screen returnScreen) {
        super(Component.translatable("screen.talent_editor.lifecycle.title"));
        this.parent = parent;
        this.returnScreen = returnScreen;
        TalentDraft.LifecycleDraft draft = parent.lifecycle();
        retainEffects = draft.retainEffects();
        retainCategory = draft.retainCategoryContribution();
        retainTriggers = draft.retainTriggers();
    }

    @Override
    protected void init() {
        TalentDraft.LifecycleDraft draft = parent.lifecycle();
        int formWidth = Math.min(520, width - 32);
        int x = (width - formWidth) / 2;
        duration = field(x, 58, formWidth, Integer.toString(draft.durationTicks()));
        draws = field(x, 101, formWidth, Integer.toString(draft.removeAfterDraws()));
        int half = (formWidth - 6) / 2;
        effectsButton = addRenderableWidget(EditorWidgets.button(effectsLabel(), button -> {
            retainEffects = !retainEffects;
            effectsButton.setMessage(effectsLabel());
        }).bounds(x, 144, half, 20).build());
        categoryButton = addRenderableWidget(EditorWidgets.button(categoryLabel(), button -> {
            retainCategory = !retainCategory;
            categoryButton.setMessage(categoryLabel());
        }).bounds(x + half + 6, 144, formWidth - half - 6, 20).build());
        triggersButton = addRenderableWidget(EditorWidgets.button(triggersLabel(), button -> {
            retainTriggers = !retainTriggers;
            triggersButton.setMessage(triggersLabel());
        }).bounds(x, 174, formWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, 80, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), button -> apply())
                .bounds(x + 86, height - 28, formWidth - 86, 20).build());
    }

    private EditBox field(int x, int y, int width, String value) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(10);
        field.setValue(value);
        addRenderableWidget(field);
        return field;
    }

    private Component effectsLabel() {
        return Component.translatable("screen.talent_editor.lifecycle.retain_effects", yesNo(retainEffects));
    }

    private Component categoryLabel() {
        return Component.translatable("screen.talent_editor.lifecycle.retain_category", yesNo(retainCategory));
    }

    private Component triggersLabel() {
        return Component.translatable("screen.talent_editor.lifecycle.retain_triggers", yesNo(retainTriggers));
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private void apply() {
        try {
            int durationTicks = Integer.parseInt(duration.getValue().strip());
            int removeAfterDraws = Integer.parseInt(draws.getValue().strip());
            if (durationTicks < 0 || durationTicks > 172_800_000
                    || removeAfterDraws < 0 || removeAfterDraws > 1_000_000) {
                throw new NumberFormatException();
            }
            parent.applyLifecycle(new TalentDraft.LifecycleDraft(
                    durationTicks, removeAfterDraws, retainEffects, retainCategory, retainTriggers
            ));
            returnParent();
        } catch (NumberFormatException exception) {
            status = Component.translatable("screen.talent_editor.lifecycle.invalid").getString();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(520, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 29, formWidth + 14, 181);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        graphics.drawString(font, Component.translatable("screen.talent_editor.lifecycle.duration"),
                x, 45, EditorTheme.MUTED, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.lifecycle.draws"),
                x, 88, EditorTheme.MUTED, false);
        graphics.drawWordWrap(font, Component.translatable("screen.talent_editor.lifecycle.help"),
                x, 216, formWidth, EditorTheme.MUTED);
        if (!status.isBlank()) graphics.drawString(font, status, x, height - 47, EditorTheme.ERROR, false);
    }

    private void returnParent() { if (minecraft != null) minecraft.setScreen(returnScreen); }
    @Override public void onClose() { returnParent(); }
}
