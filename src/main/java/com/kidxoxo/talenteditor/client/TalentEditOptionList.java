package com.kidxoxo.talenteditor.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Key-binding-style grouped list used by the unified talent editor. */
final class TalentEditOptionList extends ContainerObjectSelectionList<TalentEditOptionList.Row> {
    private static final int EDIT_WIDTH = 72;
    private String selectedKey = "";

    TalentEditOptionList(Minecraft minecraft, int width, int height, int top, int bottom) {
        super(minecraft, width, height, top, bottom, 30);
        setRenderBackground(false);
        setRenderTopAndBottom(false);
    }

    void addHeader(Component label) {
        addEntry(new HeaderRow(label));
    }

    void addOption(
            String key,
            Component label,
            Component summary,
            boolean configured,
            boolean advanced,
            Runnable action
    ) {
        OptionRow row = new OptionRow(key, label, summary, configured, advanced, action);
        addEntry(row);
        if (key.equals(selectedKey)) setSelected(row);
    }

    void restoreSelectedKey(String key) {
        selectedKey = key == null ? "" : key;
        children().stream().filter(OptionRow.class::isInstance).map(OptionRow.class::cast)
                .filter(row -> row.key.equals(selectedKey)).findFirst().ifPresent(this::setSelected);
    }

    String selectedKey() {
        Row selected = getSelected();
        return selected instanceof OptionRow option ? option.key : selectedKey;
    }

    @Override
    public int getRowWidth() {
        return Math.min(720, Math.max(160, width - 36));
    }

    @Override
    protected int getScrollbarPosition() {
        return width / 2 + getRowWidth() / 2 + 5;
    }

    abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
    }

    private final class HeaderRow extends Row {
        private final Component label;

        private HeaderRow(Component label) {
            this.label = label;
        }

        @Override
        public void render(
                GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovered, float partialTick
        ) {
            int textY = top + (height - minecraft.font.lineHeight) / 2;
            graphics.drawString(minecraft.font, label, left + 4, textY, EditorTheme.ACCENT, false);
            int lineX = left + 12 + minecraft.font.width(label);
            if (lineX < left + width) {
                graphics.fill(lineX, top + height / 2, left + width, top + height / 2 + 1, 0x504A6B8E);
            }
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }
    }

    private final class OptionRow extends Row {
        private final String key;
        private final Component label;
        private final Component summary;
        private final boolean configured;
        private final boolean advanced;
        private final Runnable action;
        private final Button edit;

        private OptionRow(
                String key,
                Component label,
                Component summary,
                boolean configured,
                boolean advanced,
                Runnable action
        ) {
            this.key = key;
            this.label = label;
            this.summary = summary;
            this.configured = configured;
            this.advanced = advanced;
            this.action = action;
            this.edit = EditorWidgets.button(Component.translatable("screen.talent_editor.overview.edit"),
                    button -> open()).bounds(0, 0, EDIT_WIDTH, 20).build();
        }

        @Override
        public void render(
                GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovered, float partialTick
        ) {
            boolean selected = getSelected() == this;
            EditorTheme.row(graphics, left, top + 1, width, height - 2, selected, hovered);
            edit.setX(left + width - EDIT_WIDTH - 3);
            edit.setY(top + 5);
            edit.render(graphics, mouseX, mouseY, partialTick);

            int contentWidth = Math.max(20, width - EDIT_WIDTH - 16);
            graphics.drawString(minecraft.font,
                    minecraft.font.plainSubstrByWidth(label.getString(), contentWidth),
                    left + 6, top + 4, EditorTheme.TEXT, false);
            String status = advanced
                    ? Component.translatable("screen.talent_editor.overview.advanced_config").getString()
                    : configured
                    ? Component.translatable("screen.talent_editor.overview.configured").getString()
                    : "";
            String summaryText = summary.getString();
            if (!status.isBlank()) summaryText = summaryText + "  " + status;
            graphics.drawString(minecraft.font,
                    minecraft.font.plainSubstrByWidth(summaryText, contentWidth),
                    left + 6, top + 16,
                    advanced ? EditorTheme.WARNING : configured ? EditorTheme.SUCCESS : EditorTheme.MUTED, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (edit.mouseClicked(mouseX, mouseY, button)) return true;
            if (button == 0) {
                open();
                return true;
            }
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER
                    || keyCode == InputConstants.KEY_SPACE) {
                open();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private void open() {
            selectedKey = key;
            setSelected(this);
            action.run();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(edit);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(edit);
        }
    }
}
