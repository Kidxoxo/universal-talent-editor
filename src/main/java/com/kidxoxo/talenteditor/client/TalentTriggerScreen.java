package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TalentTriggerScreen extends Screen {
    private static final List<String> EVENTS = List.of(
            "damage_dealt_pre", "damage_taken_pre", "damage_dealt", "damage_taken", "interact_entity",
            "kill", "death", "item_use_finish", "interact_block", "player_tick", "player_second",
            "player_move_distance"
    );
    private static final List<String> CHANCE_SOURCES = List.of("attribute", "counter", "category");
    private static final List<String> TARGETS = List.of("self", "event_entity", "damage_source", "nearby");
    private static final List<String> DAMAGE_OPERATIONS = List.of("add", "multiply");
    private static final List<String> SEARCH_QUEUE_MODES = List.of("append", "dedupe", "replace");
    private static final List<String> ACTION_FAILURE_POLICIES = List.of("continue", "stop");
    private static final int ROW_HEIGHT = 20;

    private final TalentEditScreen owner;
    private final Screen returnScreen;
    private final TalentDraft working;
    private final EditorBootstrap bootstrap;
    private int selected = -1;
    private int scroll;
    private int eventIndex;
    private int actionIndex;
    private int selectedAction;
    private int damageOperationIndex;
    private int targetIndex;
    private int filterIndex;
    private int searchTargetIndex;
    private int searchQueueModeIndex;
    private int searchMilestoneTargetIndex = TalentDraft.DrawSearchTarget.TAG.ordinal();
    private int pageIndex;
    private int chanceSourceIndex;
    private int actionFailurePolicyIndex;
    private boolean failureBranch;
    private ResourceLocation selectedEffect = new ResourceLocation("minecraft", "speed");
    private ResourceLocation chanceAttribute;
    private ResourceLocation chanceCounter;
    private ResourceLocation chanceCategory;
    private ResourceLocation filterValue;
    private ResourceLocation searchValue = new ResourceLocation("custom", "target_talent");
    private ResourceLocation searchPool;
    private ResourceLocation searchSuccessCounter;
    private ResourceLocation searchMilestoneValue;
    private ArrayList<TalentDraft.TriggerActionDraft> editingActions = new ArrayList<>();
    private ArrayList<TalentDraft.TriggerActionDraft> editingFailureActions = new ArrayList<>();
    private boolean ambient;
    private boolean showParticles = true;
    private boolean showIcon = true;
    private EditBox idField;
    private EditBox priorityField;
    private EditBox chanceField;
    private EditBox chanceAttributeField;
    private EditBox chancePerPointField;
    private EditBox distanceIntervalField;
    private EditBox cooldownField;
    private EditBox activationLimitField;
    private EditBox durationField;
    private EditBox amplifierField;
    private EditBox effectField;
    private EditBox damageAmountField;
    private EditBox damageMinimumField;
    private EditBox damageMaximumField;
    private EditBox entityRadiusField;
    private EditBox filterValueField;
    private EditBox searchGuaranteedField;
    private EditBox searchValueField;
    private EditBox searchPoolField;
    private EditBox searchSuccessCounterField;
    private EditBox searchSuccessIncrementField;
    private EditBox searchMilestoneEveryField;
    private EditBox searchMilestoneValueField;
    private EditBox searchMilestoneGuaranteedField;
    private Button eventButton;
    private Button actionButton;
    private Button actionBranchButton;
    private Button actionFailurePolicyButton;
    private Button onceButton;
    private Button previousActionButton;
    private Button nextActionButton;
    private Button addActionButton;
    private Button removeActionButton;
    private Button moveActionUpButton;
    private Button moveActionDownButton;
    private Button targetButton;
    private Button filterButton;
    private Button chooseEffectButton;
    private Button chooseChanceAttributeButton;
    private Button clearChanceAttributeButton;
    private Button chanceSourceButton;
    private Button damageOperationButton;
    private Button chooseFilterButton;
    private Button ambientButton;
    private Button particlesButton;
    private Button iconButton;
    private Button searchTargetButton;
    private Button chooseSearchValueButton;
    private Button chooseSearchPoolButton;
    private Button clearSearchPoolButton;
    private Button searchQueueModeButton;
    private Button chooseSearchSuccessCounterButton;
    private Button clearSearchSuccessCounterButton;
    private Button searchMilestoneTargetButton;
    private Button chooseSearchMilestoneValueButton;
    private Button basePageButton;
    private Button actionPageButton;
    private Button conditionPageButton;
    private Button searchPageButton;
    private Button addTriggerButton;
    private Button removeButton;
    private String status = "";

    TalentTriggerScreen(TalentEditScreen parent, TalentDraft working, EditorBootstrap bootstrap) {
        this(parent, working, bootstrap, parent, -1);
    }

    TalentTriggerScreen(
            TalentEditScreen owner, TalentDraft working, EditorBootstrap bootstrap, Screen returnScreen
    ) {
        this(owner, working, bootstrap, returnScreen, -1);
    }

    TalentTriggerScreen(
            TalentEditScreen owner, TalentDraft working, EditorBootstrap bootstrap, Screen returnScreen,
            int selectedTriggerRawIndex
    ) {
        super(Component.translatable("screen.talent_editor.trigger.title"));
        this.owner = owner;
        this.returnScreen = returnScreen;
        this.working = working;
        this.bootstrap = bootstrap;
        List<TalentDraft.TriggerRow> rows = working.triggerRows();
        if (!rows.isEmpty()) {
            selected = selectedTriggerRawIndex < 0 ? 0 : java.util.stream.IntStream.range(0, rows.size())
                    .filter(index -> rows.get(index).rawIndex() == selectedTriggerRawIndex)
                    .findFirst().orElse(0);
        }
    }

    @Override
    protected void init() {
        int formWidth = Math.min(760, width - 32);
        int left = (width - formWidth) / 2;
        int listWidth = Math.min(220, Math.max(112, formWidth * 2 / 5));
        int right = left + listWidth + 12;
        int rightWidth = formWidth - listWidth - 12;
        int half = (rightWidth - 6) / 2;
        int fifth = (rightWidth - 24) / 5;

        int pageWidth = (rightWidth - 18) / 4;
        basePageButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.trigger.page.base"), button -> switchPage(0))
                .bounds(right, 32, pageWidth, 20).build());
        actionPageButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.trigger.page.action"), button -> switchPage(1))
                .bounds(right + pageWidth + 6, 32, pageWidth, 20).build());
        conditionPageButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.trigger.page.condition"), button -> switchPage(2))
                .bounds(right + (pageWidth + 6) * 2, 32,
                        pageWidth, 20).build());
        searchPageButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.trigger.page.search"), button -> switchPage(3))
                .bounds(right + (pageWidth + 6) * 3, 32,
                        rightWidth - (pageWidth + 6) * 3, 20).build());

        idField = field(right, 63, rightWidth, 64);
        priorityField = field(right, 93, fifth, 8);
        chanceField = field(right + fifth + 6, 93, fifth, 8);
        cooldownField = field(right + (fifth + 6) * 2, 93, fifth, 10);
        activationLimitField = field(right + (fifth + 6) * 3, 93, fifth, 10);
        eventButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleEvent())
                .bounds(right + (fifth + 6) * 4, 93,
                        rightWidth - (fifth + 6) * 4, 20).build());
        int chanceChooseWidth = Math.min(76, Math.max(46, rightWidth / 4));
        int chanceAttributeWidth = Math.max(40, rightWidth - chanceChooseWidth - 30);
        chanceAttributeField = field(right, 123, chanceAttributeWidth, 256);
        chanceAttributeField.setEditable(false);
        chooseChanceAttributeButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.trigger.choose_chance_attribute"),
                        button -> chooseChanceAttribute())
                .bounds(right + chanceAttributeWidth + 4, 123, chanceChooseWidth, 20).build());
        clearChanceAttributeButton = addRenderableWidget(Button.builder(Component.literal("×"),
                        button -> clearChanceAttribute())
                .bounds(right + chanceAttributeWidth + chanceChooseWidth + 8, 123, 22, 20).build());
        chanceSourceButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleChanceSource())
                .bounds(right, 153, half, 20).build());
        chancePerPointField = field(right + half + 6, 153, rightWidth - half - 6, 32);
        distanceIntervalField = field(right, 183, rightWidth, 32);

        int branchWidth = Math.min(104, Math.max(62, rightWidth / 3));
        actionBranchButton = addRenderableWidget(Button.builder(Component.empty(), button -> switchActionBranch())
                .bounds(right, 63, branchWidth, 20).build());
        actionButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleAction())
                .bounds(right + branchWidth + 6, 63, rightWidth - branchWidth - 6, 20).build());
        int actionControlWidth = Math.max(18, (rightWidth - 20) / 6);
        int actionX = right;
        previousActionButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> previousAction())
                .bounds(actionX, 89, actionControlWidth, 20).build());
        actionX += actionControlWidth + 4;
        nextActionButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> nextAction())
                .bounds(actionX, 89, actionControlWidth, 20).build());
        actionX += actionControlWidth + 4;
        addActionButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> addAction())
                .bounds(actionX, 89, actionControlWidth, 20).build());
        actionX += actionControlWidth + 4;
        removeActionButton = addRenderableWidget(Button.builder(Component.literal("−"), button -> removeAction())
                .bounds(actionX, 89, actionControlWidth, 20).build());
        actionX += actionControlWidth + 4;
        moveActionUpButton = addRenderableWidget(Button.builder(Component.literal("↑"), button -> moveAction(-1))
                .bounds(actionX, 89, actionControlWidth, 20).build());
        actionX += actionControlWidth + 4;
        moveActionDownButton = addRenderableWidget(Button.builder(Component.literal("↓"), button -> moveAction(1))
                .bounds(actionX, 89, right + rightWidth - actionX, 20).build());

        effectField = field(right, 126, rightWidth - 70, 256);
        effectField.setEditable(false);
        chooseEffectButton = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.trigger.choose_effect"),
                        button -> chooseEffect())
                .bounds(right + rightWidth - 64, 126, 64, 20).build());
        damageAmountField = field(right, 156, half, 32);
        damageOperationButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleDamageOperation())
                .bounds(right + half + 6, 156, rightWidth - half - 6, 20).build());
        durationField = field(right, 156, half, 8);
        amplifierField = field(right + half + 6, 156, rightWidth - half - 6, 4);
        damageMinimumField = field(right, 186, half, 32);
        damageMaximumField = field(right + half + 6, 186, rightWidth - half - 6, 32);
        targetButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleTarget())
                .bounds(right, 63, half, 20).build());
        filterButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleFilter())
                .bounds(right + half + 6, 63, rightWidth - half - 6, 20).build());
        filterValueField = field(right, 96, rightWidth - 70, 256);
        filterValueField.setEditable(false);
        chooseFilterButton = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.trigger.choose_filter"),
                        button -> chooseFilter())
                .bounds(right + rightWidth - 64, 96, 64, 20).build());
        int third = (rightWidth - 12) / 3;
        ambientButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleAmbient())
                .bounds(right, 186, third, 20).build());
        particlesButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleParticles())
                .bounds(right + third + 6, 186, third, 20).build());
        iconButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleIcon())
                .bounds(right + (third + 6) * 2, 186, rightWidth - (third + 6) * 2, 20).build());
        actionFailurePolicyButton = addRenderableWidget(Button.builder(Component.empty(),
                        button -> cycleActionFailurePolicy())
                .bounds(right, 126, half, 20).build());
        onceButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleOnce())
                .bounds(right + half + 6, 126, rightWidth - half - 6, 20).build());
        entityRadiusField = field(right, 156, rightWidth, 32);
        searchTargetButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleSearchTarget())
                .bounds(right, 126, half, 20).build());
        searchGuaranteedField = field(right + half + 6, 126, rightWidth - half - 6, 2);
        searchValueField = field(right, 156, rightWidth - 70, 256);
        searchValueField.setEditable(false);
        chooseSearchValueButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.trigger.choose_search_value"), button -> chooseSearchValue())
                .bounds(right + rightWidth - 64, 156, 64, 20).build());
        int searchPoolWidth = Math.max(30, rightWidth - 94);
        searchPoolField = field(right, 186, searchPoolWidth, 256);
        searchPoolField.setEditable(false);
        chooseSearchPoolButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.trigger.choose_pool"), button -> chooseSearchPool())
                .bounds(right + searchPoolWidth + 4, 186, 64, 20).build());
        clearSearchPoolButton = addRenderableWidget(Button.builder(Component.literal("×"),
                        button -> clearSearchPool())
                .bounds(right + searchPoolWidth + 72, 186, rightWidth - searchPoolWidth - 72, 20).build());

        searchQueueModeButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleSearchQueueMode())
                .bounds(right, 63, rightWidth, 20).build());
        int counterWidth = Math.max(30, rightWidth - 94);
        searchSuccessCounterField = field(right, 93, counterWidth, 256);
        searchSuccessCounterField.setEditable(false);
        chooseSearchSuccessCounterButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.trigger.choose_counter"), button -> chooseSearchSuccessCounter())
                .bounds(right + counterWidth + 4, 93, 64, 20).build());
        clearSearchSuccessCounterButton = addRenderableWidget(Button.builder(Component.literal("×"),
                        button -> clearSearchSuccessCounter())
                .bounds(right + counterWidth + 72, 93, rightWidth - counterWidth - 72, 20).build());
        searchSuccessIncrementField = field(right, 123, half, 16);
        searchMilestoneEveryField = field(right + half + 6, 123, rightWidth - half - 6, 16);
        searchMilestoneTargetButton = addRenderableWidget(Button.builder(Component.empty(),
                        button -> cycleSearchMilestoneTarget())
                .bounds(right, 153, half, 20).build());
        searchMilestoneGuaranteedField = field(right + half + 6, 153, rightWidth - half - 6, 2);
        searchMilestoneValueField = field(right, 183, rightWidth - 70, 256);
        chooseSearchMilestoneValueButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.trigger.choose_search_value"), button -> chooseSearchMilestoneValue())
                .bounds(right + rightWidth - 64, 183, 64, 20).build());
        addTriggerButton = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.trigger.add"),
                        button -> addTrigger())
                .bounds(left, height - 56, (listWidth - 6) / 2, 20).build());
        removeButton = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.trigger.remove"),
                        button -> removeTrigger())
                .bounds(left + (listWidth - 6) / 2 + 6, height - 56,
                        listWidth - (listWidth - 6) / 2 - 6, 20).build());

        int bottom = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnWithoutApplying())
                .bounds(left, bottom, (formWidth - 6) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.trigger.apply"),
                        button -> apply())
                .bounds(left + (formWidth - 6) / 2 + 6, bottom,
                        formWidth - (formWidth - 6) / 2 - 6, 20).build());
        loadSelected();
    }

    private EditBox field(int x, int y, int width, int maxLength) {
        EditBox value = new EditBox(font, x, y, width, 20, Component.empty());
        value.setMaxLength(maxLength);
        addRenderableWidget(value);
        return value;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(760, width - 32);
        int left = (width - formWidth) / 2;
        int listWidth = Math.min(220, Math.max(112, formWidth * 2 / 5));
        int right = left + listWidth + 12;
        int rightWidth = formWidth - listWidth - 12;
        EditorTheme.panel(graphics, left - 6, 27, listWidth + 12, Math.max(1, height - 63));
        EditorTheme.panel(graphics, right - 6, 27, rightWidth + 12, Math.max(1, height - 63));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 9, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.talent_editor.trigger.list"), left, 33, 0xA0A0A0, false);
        int pageWidth = (rightWidth - 18) / 4;
        int tabX = right + pageIndex * (pageWidth + 6);
        int tabWidth = pageIndex == 3 ? rightWidth - (pageWidth + 6) * 3 : pageWidth;
        EditorTheme.selectedTab(graphics, tabX, 32, tabWidth);
        TalentDraft.TriggerAction action = actions()[actionIndex];
        if (pageIndex == 0) {
            label(graphics, right, 52, "id");
            label(graphics, right, 82, "priority_event_limit");
            label(graphics, right, 112, "chance_attribute");
            label(graphics, right, 142, "chance_per_point");
            if (EVENTS.get(eventIndex).equals("player_move_distance")) {
                label(graphics, right, 172, "distance_interval");
            }
        } else if (pageIndex == 1) {
            label(graphics, right, 52, "action_type");
            if (action == TalentDraft.TriggerAction.QUEUE_DRAW_CONSTRAINT) {
                label(graphics, right, 115, "search_target_guaranteed");
                label(graphics, right, 145, "search_value");
                label(graphics, right, 175, "search_pool");
            } else if (action == TalentDraft.TriggerAction.MODIFY_ENTITY_ATTRIBUTE) {
                label(graphics, right, 115, "attribute");
                label(graphics, right, 145, "attribute_amount_operation");
                label(graphics, right, 175, "attribute_minimum_maximum");
            } else if (action == TalentDraft.TriggerAction.DAMAGE_ENTITY
                    || action == TalentDraft.TriggerAction.HEAL_ENTITY) {
                label(graphics, right, 145, "entity_amount");
            } else if (action != TalentDraft.TriggerAction.CANCEL_DAMAGE) {
                label(graphics, right, action == TalentDraft.TriggerAction.MODIFY_DAMAGE ? 145 : 115,
                        action == TalentDraft.TriggerAction.MODIFY_DAMAGE
                                ? "damage_amount_operation" : "effect");
            }
            if (action != TalentDraft.TriggerAction.QUEUE_DRAW_CONSTRAINT
                    && action != TalentDraft.TriggerAction.REMOVE_EFFECT
                    && action != TalentDraft.TriggerAction.CANCEL_DAMAGE
                    && action != TalentDraft.TriggerAction.MODIFY_ENTITY_ATTRIBUTE
                    && action != TalentDraft.TriggerAction.DAMAGE_ENTITY
                    && action != TalentDraft.TriggerAction.HEAL_ENTITY) {
                label(graphics, right, action == TalentDraft.TriggerAction.MODIFY_DAMAGE ? 175 : 145,
                        action == TalentDraft.TriggerAction.MODIFY_DAMAGE
                                ? "damage_minimum_maximum" : "duration_amplifier");
            }
            if (action == TalentDraft.TriggerAction.ADD_EFFECT) label(graphics, right, 175, "effect_display");
        } else if (pageIndex == 2) {
            label(graphics, right, 52, action == TalentDraft.TriggerAction.MODIFY_DAMAGE
                    || action == TalentDraft.TriggerAction.CANCEL_DAMAGE
                    ? "damage_filter" : action == TalentDraft.TriggerAction.QUEUE_DRAW_CONSTRAINT
                    ? "event_filter" : "target_filter");
            label(graphics, right, 85, "filter_value");
            label(graphics, right, 115, "failure_policy_once");
            if (supportsExtendedTarget(action)) label(graphics, right, 145, "nearby_radius");
        } else {
            label(graphics, right, 52, "search_queue_mode");
            label(graphics, right, 82, "search_success_counter");
            label(graphics, right, 112, "search_increment_milestone_every");
            label(graphics, right, 142, "search_milestone_target_guaranteed");
            label(graphics, right, 172, "search_milestone_value");
        }

        List<TalentDraft.TriggerRow> rows = working.triggerRows();
        int visibleRows = Math.max(1, (height - 63 - 50) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visibleRows)));
        int end = Math.min(rows.size(), scroll + visibleRows);
        for (int index = scroll; index < end; index++) {
            TalentDraft.TriggerRow row = rows.get(index);
            int y = 51 + (index - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX < left + listWidth && mouseY >= y - 3 && mouseY < y + 15;
            if (index == selected || hovered) graphics.fill(left, y - 3, left + listWidth, y + 15,
                    index == selected ? 0x704A90E2 : 0x404A90E2);
            String marker = row.editable() ? "" : "[JSON] ";
            graphics.drawString(font, font.plainSubstrByWidth(marker + row.id() + " · " + row.event(), listWidth - 8),
                    left + 4, y, row.editable() ? 0xE8E8E8 : 0x909090, false);
        }
        if (rows.isEmpty()) graphics.drawString(font,
                Component.translatable("screen.talent_editor.trigger.empty"), left + 4, 52, 0x808080, false);
        if (!status.isBlank()) graphics.drawCenteredString(font,
                font.plainSubstrByWidth(status, formWidth), width / 2, 20, 0xFF7777);
    }

    private void label(GuiGraphics graphics, int x, int y, String suffix) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.trigger." + suffix),
                x, y, 0xA0A0A0, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int formWidth = Math.min(760, width - 32);
            int left = (width - formWidth) / 2;
            int listWidth = Math.min(220, Math.max(112, formWidth * 2 / 5));
            if (mouseX >= left && mouseX < left + listWidth && mouseY >= 48 && mouseY < height - 63) {
                int next = scroll + (int) ((mouseY - 48) / ROW_HEIGHT);
                if (next >= 0 && next < working.triggerRows().size()) {
                    if (!storeSelected()) return true;
                    selected = next;
                    selectedAction = 0;
                    failureBranch = false;
                    loadSelected();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rows = Math.max(1, (height - 63 - 50) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta) * 3,
                Math.max(0, working.triggerRows().size() - rows)));
        return true;
    }

    private void addTrigger() {
        if (!storeSelected()) return;
        if (working.triggerRows().size() >= 256) {
            status = Component.translatable("screen.talent_editor.trigger.limit").getString();
            return;
        }
        selected = working.addEffectTrigger();
        selectedAction = 0;
        failureBranch = false;
        loadSelected();
        status = "";
    }

    private void removeTrigger() {
        List<TalentDraft.TriggerRow> rows = working.triggerRows();
        if (selected < 0 || selected >= rows.size() || !rows.get(selected).editable()) return;
        working.removeEffectTrigger(rows.get(selected).rawIndex());
        selected = Math.min(selected, working.triggerRows().size() - 1);
        selectedAction = 0;
        failureBranch = false;
        loadSelected();
        status = "";
    }

    private boolean storeSelected() {
        List<TalentDraft.TriggerRow> rows = working.triggerRows();
        if (selected < 0 || selected >= rows.size() || !rows.get(selected).editable()) return true;
        TalentDraft.TriggerDraft draft = captureDraft();
        if (draft == null) return false;
        working.updateEffectTrigger(rows.get(selected).rawIndex(), draft);
        status = "";
        return true;
    }

    private TalentDraft.TriggerDraft captureDraft() {
        String id = idField.getValue().strip();
        int priority;
        double chance;
        double chancePerPoint;
        double distanceInterval;
        int cooldown;
        int activationLimit;
        int duration;
        int amplifier;
        int searchGuaranteed;
        double damageAmount;
        double damageMinimum;
        double damageMaximum;
        double entityRadius;
        try {
            priority = Integer.parseInt(priorityField.getValue().strip());
            chance = Double.parseDouble(chanceField.getValue().strip());
            chancePerPoint = Double.parseDouble(chancePerPointField.getValue().strip());
            distanceInterval = Double.parseDouble(distanceIntervalField.getValue().strip());
            cooldown = Integer.parseInt(cooldownField.getValue().strip());
            activationLimit = Integer.parseInt(activationLimitField.getValue().strip());
            duration = Integer.parseInt(durationField.getValue().strip());
            amplifier = Integer.parseInt(amplifierField.getValue().strip());
            searchGuaranteed = Integer.parseInt(searchGuaranteedField.getValue().strip());
            damageAmount = Double.parseDouble(damageAmountField.getValue().strip());
            damageMinimum = Double.parseDouble(damageMinimumField.getValue().strip());
            damageMaximum = Double.parseDouble(damageMaximumField.getValue().strip());
            entityRadius = Double.parseDouble(entityRadiusField.getValue().strip());
        } catch (NumberFormatException exception) {
            status = Component.translatable("screen.talent_editor.trigger.invalid_number").getString();
            return null;
        }
        if (!id.matches("[a-z0-9_.-]{1,96}") || priority < -1000 || priority > 1000
                || !Double.isFinite(chance) || chance < 0.0D || chance > 1.0D
                || !Double.isFinite(chancePerPoint) || chancePerPoint < -1.0D || chancePerPoint > 1.0D
                || !Double.isFinite(distanceInterval) || distanceInterval < 0.01D
                || distanceInterval > 1_000_000.0D
                || cooldown < 0 || cooldown > 1_728_000
                || activationLimit < 0 || activationLimit > 1_000_000) {
            status = Component.translatable("screen.talent_editor.trigger.invalid_value").getString();
            return null;
        }
        TalentDraft.TriggerFilter filter = filters()[filterIndex];
        if (filter != TalentDraft.TriggerFilter.NONE && filterValue == null) {
            status = Component.translatable("screen.talent_editor.trigger.filter_required").getString();
            return null;
        }
        String event = EVENTS.get(eventIndex);
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        if (!activeActions.isEmpty()) {
            TalentDraft.TriggerActionDraft actionDraft = captureAction(event, duration, amplifier,
                    damageAmount, damageMinimum, damageMaximum, entityRadius, searchGuaranteed);
            if (actionDraft == null) return null;
            activeActions.set(Math.max(0, Math.min(selectedAction, activeActions.size() - 1)), actionDraft);
        }
        if (!isDamageEvent(event) && java.util.stream.Stream
                .concat(editingActions.stream(), editingFailureActions.stream()).anyMatch(value ->
                value.action() == TalentDraft.TriggerAction.MODIFY_DAMAGE
                        || value.action() == TalentDraft.TriggerAction.CANCEL_DAMAGE)) {
            status = Component.translatable("screen.talent_editor.trigger.damage_event_required").getString();
            return null;
        }
        for (TalentDraft.TriggerActionDraft value : java.util.stream.Stream
                .concat(editingActions.stream(), editingFailureActions.stream()).toList()) {
            if (value.action() == TalentDraft.TriggerAction.ADD_EFFECT
                    && (value.durationTicks() < 1 || value.durationTicks() > 72_000
                    || value.amplifier() < 0 || value.amplifier() > 255)) {
                status = Component.translatable("screen.talent_editor.trigger.invalid_value").getString();
                return null;
            }
            if (value.action() == TalentDraft.TriggerAction.MODIFY_DAMAGE && !validDamage(value)) {
                status = Component.translatable("screen.talent_editor.trigger.invalid_damage").getString();
                return null;
            }
            if (value.action() == TalentDraft.TriggerAction.MODIFY_ENTITY_ATTRIBUTE
                    && !validAttributeChange(value)) {
                status = Component.translatable("screen.talent_editor.trigger.invalid_attribute_change").getString();
                return null;
            }
            if ((value.action() == TalentDraft.TriggerAction.DAMAGE_ENTITY
                    || value.action() == TalentDraft.TriggerAction.HEAL_ENTITY)
                    && !validEntityHealthChange(value)) {
                status = Component.translatable("screen.talent_editor.trigger.invalid_entity_health").getString();
                return null;
            }
            if (value.action() == TalentDraft.TriggerAction.QUEUE_DRAW_CONSTRAINT
                    && (value.searchValue() == null
                    || value.searchGuaranteed() < 1 || value.searchGuaranteed() > 5)) {
                status = Component.translatable("screen.talent_editor.trigger.invalid_search").getString();
                return null;
            }
        }
        return new TalentDraft.TriggerDraft(
                id, event, priority, chance, cooldown, activationLimit, chanceAttribute, chanceCounter,
                chanceCategory,
                chancePerPoint, distanceInterval, filter, filterValue, editingActions,
                ACTION_FAILURE_POLICIES.get(actionFailurePolicyIndex), editingFailureActions
        );
    }

    private TalentDraft.TriggerActionDraft captureAction(
            String event,
            int duration,
            int amplifier,
            double damageAmount,
            double damageMinimum,
            double damageMaximum,
            double entityRadius,
            int searchGuaranteed
    ) {
        TalentDraft.TriggerAction action = actions()[actionIndex];
        long searchSuccessIncrement;
        int searchMilestoneEvery;
        int searchMilestoneGuaranteed;
        try {
            searchSuccessIncrement = Long.parseLong(searchSuccessIncrementField.getValue().strip());
            searchMilestoneEvery = Integer.parseInt(searchMilestoneEveryField.getValue().strip());
            searchMilestoneGuaranteed = Integer.parseInt(searchMilestoneGuaranteedField.getValue().strip());
        } catch (NumberFormatException exception) {
            status = Component.translatable("screen.talent_editor.trigger.invalid_number").getString();
            return null;
        }
        if ((action == TalentDraft.TriggerAction.MODIFY_DAMAGE
                || action == TalentDraft.TriggerAction.CANCEL_DAMAGE) && !isDamageEvent(event)) {
            status = Component.translatable("screen.talent_editor.trigger.damage_event_required").getString();
            return null;
        }
        if (action == TalentDraft.TriggerAction.ADD_EFFECT
                && (duration < 1 || duration > 72_000 || amplifier < 0 || amplifier > 255)) {
            status = Component.translatable("screen.talent_editor.trigger.invalid_value").getString();
            return null;
        }
        if (action == TalentDraft.TriggerAction.MODIFY_DAMAGE) {
            String operation = DAMAGE_OPERATIONS.get(damageOperationIndex);
            boolean invalidAmount = !Double.isFinite(damageAmount)
                    || operation.equals("add") && (damageAmount < -1.0E9D || damageAmount > 1.0E9D)
                    || operation.equals("multiply") && (damageAmount < 0.0D || damageAmount > 1_000.0D);
            if (invalidAmount || !Double.isFinite(damageMinimum) || !Double.isFinite(damageMaximum)
                    || damageMinimum < 0.0D || damageMaximum > 1.0E9D
                    || damageMinimum > damageMaximum) {
                status = Component.translatable("screen.talent_editor.trigger.invalid_damage").getString();
                return null;
            }
        }
        if (supportsExtendedTarget(action)
                && (!Double.isFinite(entityRadius) || entityRadius < 0.5D || entityRadius > 64.0D)) {
            status = Component.translatable("screen.talent_editor.trigger.invalid_radius").getString();
            return null;
        }
        if (action == TalentDraft.TriggerAction.MODIFY_ENTITY_ATTRIBUTE) {
            TalentDraft.TriggerActionDraft candidate = new TalentDraft.TriggerActionDraft(
                    action, selectedEffect, duration, amplifier, TARGETS.get(targetIndex),
                    ambient, showParticles, showIcon, DAMAGE_OPERATIONS.get(damageOperationIndex),
                    damageAmount, damageMinimum, damageMaximum, entityRadius
            );
            if (!validAttributeChange(candidate)) {
                status = Component.translatable("screen.talent_editor.trigger.invalid_attribute_change").getString();
                return null;
            }
        }
        if (action == TalentDraft.TriggerAction.DAMAGE_ENTITY
                || action == TalentDraft.TriggerAction.HEAL_ENTITY) {
            TalentDraft.TriggerActionDraft candidate = new TalentDraft.TriggerActionDraft(
                    action, selectedEffect, duration, amplifier, TARGETS.get(targetIndex),
                    ambient, showParticles, showIcon, "add", damageAmount, 0.0D, 1.0E9D, entityRadius
            );
            if (!validEntityHealthChange(candidate)) {
                status = Component.translatable("screen.talent_editor.trigger.invalid_entity_health").getString();
                return null;
            }
        }
        if (action == TalentDraft.TriggerAction.QUEUE_DRAW_CONSTRAINT) {
            searchValue = ResourceLocation.tryParse(searchValueField.getValue().strip());
            String milestoneText = searchMilestoneValueField.getValue().strip();
            searchMilestoneValue = milestoneText.isEmpty() ? null : ResourceLocation.tryParse(milestoneText);
            boolean invalidSuccess = searchSuccessCounter == null
                    ? searchSuccessIncrement != 0L || searchMilestoneEvery != 0 || searchMilestoneValue != null
                    : searchSuccessIncrement < 1L || searchSuccessIncrement > 1_000_000_000_000L;
            boolean invalidMilestone = searchMilestoneEvery < 0 || searchMilestoneEvery > 1_000_000_000
                    || searchMilestoneEvery == 0 && searchMilestoneValue != null
                    || searchMilestoneEvery > 0 && (searchMilestoneValue == null
                    || searchMilestoneGuaranteed < 1 || searchMilestoneGuaranteed > 5);
            if (searchValue == null || searchGuaranteed < 1 || searchGuaranteed > 5
                    || invalidSuccess || invalidMilestone) {
                status = Component.translatable("screen.talent_editor.trigger.invalid_search").getString();
                return null;
            }
        }
        return new TalentDraft.TriggerActionDraft(
                action, selectedEffect, duration, amplifier,
                TARGETS.get(targetIndex), ambient, showParticles, showIcon,
                DAMAGE_OPERATIONS.get(damageOperationIndex), damageAmount, damageMinimum, damageMaximum,
                TalentDraft.DrawSearchTarget.values()[searchTargetIndex], searchValue, searchPool, searchGuaranteed,
                SEARCH_QUEUE_MODES.get(searchQueueModeIndex), searchSuccessCounter, searchSuccessIncrement,
                searchMilestoneEvery, TalentDraft.DrawSearchTarget.values()[searchMilestoneTargetIndex],
                searchMilestoneValue, searchMilestoneGuaranteed, entityRadius
        );
    }

    private static boolean validDamage(TalentDraft.TriggerActionDraft value) {
        boolean invalidAmount = !Double.isFinite(value.damageAmount())
                || value.damageOperation().equals("add")
                && (value.damageAmount() < -1.0E9D || value.damageAmount() > 1.0E9D)
                || value.damageOperation().equals("multiply")
                && (value.damageAmount() < 0.0D || value.damageAmount() > 1_000.0D);
        return !invalidAmount && Double.isFinite(value.damageMinimum())
                && Double.isFinite(value.damageMaximum()) && value.damageMinimum() >= 0.0D
                && value.damageMaximum() <= 1.0E9D && value.damageMinimum() <= value.damageMaximum();
    }

    private static boolean validAttributeChange(TalentDraft.TriggerActionDraft value) {
        boolean validAmount = Double.isFinite(value.damageAmount())
                && (value.damageOperation().equals("add")
                ? Math.abs(value.damageAmount()) <= 1.0E9D
                : value.damageOperation().equals("multiply")
                && value.damageAmount() >= 0.0D && value.damageAmount() <= 1_000.0D);
        return validAmount && Double.isFinite(value.damageMinimum())
                && Double.isFinite(value.damageMaximum())
                && value.damageMinimum() >= -1.0E9D && value.damageMaximum() <= 1.0E9D
                && value.damageMinimum() <= value.damageMaximum()
                && validExtendedTarget(value.target()) && validRadius(value.entityRadius());
    }

    private static boolean validEntityHealthChange(TalentDraft.TriggerActionDraft value) {
        return Double.isFinite(value.damageAmount()) && value.damageAmount() > 0.0D
                && value.damageAmount() <= 1_000_000.0D
                && validExtendedTarget(value.target()) && validRadius(value.entityRadius());
    }

    private static boolean validRadius(double radius) {
        return Double.isFinite(radius) && radius >= 0.5D && radius <= 64.0D;
    }

    private static boolean validExtendedTarget(String target) {
        return TARGETS.contains(target);
    }

    private void loadSelected() {
        List<TalentDraft.TriggerRow> rows = working.triggerRows();
        boolean editable = selected >= 0 && selected < rows.size() && rows.get(selected).editable();
        if (editable) {
            TalentDraft.TriggerDraft value = rows.get(selected).draft();
            idField.setValue(value.id());
            priorityField.setValue(Integer.toString(value.priority()));
            chanceField.setValue(Double.toString(value.chance()));
            chanceAttribute = value.chanceAttribute();
            chanceCounter = value.chanceCounter();
            chanceCategory = value.chanceCategory();
            chanceSourceIndex = chanceCategory != null ? 2 : chanceCounter == null ? 0 : 1;
            chancePerPointField.setValue(Double.toString(value.chancePerPoint()));
            distanceIntervalField.setValue(Double.toString(value.distanceInterval()));
            cooldownField.setValue(Integer.toString(value.cooldownTicks()));
            activationLimitField.setValue(Integer.toString(value.activationLimit()));
            editingActions = new ArrayList<>(value.actions());
            editingFailureActions = new ArrayList<>(value.failureActions());
            actionFailurePolicyIndex = Math.max(0,
                    ACTION_FAILURE_POLICIES.indexOf(value.actionFailurePolicy()));
            ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
            selectedAction = Math.max(0, Math.min(selectedAction, activeActions.size() - 1));
            filterValue = value.filterValue();
            eventIndex = Math.max(0, EVENTS.indexOf(value.event()));
            filterIndex = Math.max(0, List.of(filters()).indexOf(value.filter()));
            loadAction(activeActions.isEmpty()
                    ? TalentDraft.TriggerActionDraft.defaults() : activeActions.get(selectedAction));
        } else {
            idField.setValue("");
            priorityField.setValue("0");
            chanceField.setValue("1.0");
            chanceAttribute = null;
            chanceCounter = null;
            chanceCategory = null;
            chanceSourceIndex = 0;
            chancePerPointField.setValue("0.0");
            distanceIntervalField.setValue("1.0");
            cooldownField.setValue("0");
            activationLimitField.setValue("0");
            editingActions = new ArrayList<>(List.of(TalentDraft.TriggerActionDraft.defaults()));
            editingFailureActions = new ArrayList<>();
            actionFailurePolicyIndex = 0;
            failureBranch = false;
            selectedAction = 0;
            loadAction(editingActions.get(0));
            filterValue = null;
        }
        effectField.setValue(selectedEffect.toString());
        ResourceLocation chanceSource = chanceSource();
        chanceAttributeField.setValue(chanceSource == null ? "" : chanceSource.toString());
        filterValueField.setValue(filterValue == null ? "" : filterValue.toString());
        idField.setEditable(editable);
        priorityField.setEditable(editable);
        chanceField.setEditable(editable);
        chancePerPointField.setEditable(editable && chanceSource != null);
        distanceIntervalField.setEditable(editable && EVENTS.get(eventIndex).equals("player_move_distance"));
        cooldownField.setEditable(editable);
        activationLimitField.setEditable(editable);
        durationField.setEditable(editable);
        amplifierField.setEditable(editable);
        eventButton.active = editable;
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        actionButton.active = editable && !activeActions.isEmpty();
        actionBranchButton.active = editable;
        actionFailurePolicyButton.active = editable;
        onceButton.active = editable;
        previousActionButton.active = editable && selectedAction > 0;
        nextActionButton.active = editable && selectedAction + 1 < activeActions.size();
        addActionButton.active = editable && activeActions.size() < 256;
        removeActionButton.active = editable && (failureBranch || activeActions.size() > 1)
                && !activeActions.isEmpty();
        moveActionUpButton.active = editable && selectedAction > 0;
        moveActionDownButton.active = editable && selectedAction + 1 < activeActions.size();
        chooseChanceAttributeButton.active = editable;
        clearChanceAttributeButton.active = editable && chanceSource != null;
        chanceSourceButton.active = editable;
        targetButton.active = editable;
        filterButton.active = editable;
        chooseEffectButton.active = editable;
        damageAmountField.setEditable(editable);
        damageMinimumField.setEditable(editable);
        damageMaximumField.setEditable(editable);
        damageOperationButton.active = editable;
        searchTargetButton.active = editable;
        searchGuaranteedField.setEditable(editable);
        chooseSearchValueButton.active = editable;
        chooseSearchPoolButton.active = editable;
        clearSearchPoolButton.active = editable && searchPool != null;
        chooseFilterButton.active = editable && filterIndex > 0;
        ambientButton.active = editable;
        particlesButton.active = editable;
        iconButton.active = editable;
        removeButton.active = editable;
        updateLabels();
    }

    private void loadAction(TalentDraft.TriggerActionDraft value) {
        actionIndex = Math.max(0, List.of(actions()).indexOf(value.action()));
        durationField.setValue(Integer.toString(value.durationTicks()));
        amplifierField.setValue(Integer.toString(value.amplifier()));
        damageOperationIndex = Math.max(0, DAMAGE_OPERATIONS.indexOf(value.damageOperation()));
        damageAmountField.setValue(Double.toString(value.damageAmount()));
        damageMinimumField.setValue(Double.toString(value.damageMinimum()));
        damageMaximumField.setValue(Double.toString(value.damageMaximum()));
        entityRadiusField.setValue(Double.toString(value.entityRadius()));
        selectedEffect = value.effect();
        targetIndex = Math.max(0, TARGETS.indexOf(value.target()));
        ambient = value.ambient();
        showParticles = value.showParticles();
        showIcon = value.showIcon();
        searchTargetIndex = Math.max(0, List.of(TalentDraft.DrawSearchTarget.values())
                .indexOf(value.searchTarget()));
        searchValue = value.searchValue();
        searchPool = value.searchPool();
        searchQueueModeIndex = Math.max(0, SEARCH_QUEUE_MODES.indexOf(value.searchQueueMode()));
        searchSuccessCounter = value.searchSuccessCounter();
        searchMilestoneTargetIndex = Math.max(0, List.of(TalentDraft.DrawSearchTarget.values())
                .indexOf(value.searchMilestoneTarget()));
        searchMilestoneValue = value.searchMilestoneValue();
        searchGuaranteedField.setValue(Integer.toString(value.searchGuaranteed()));
        searchValueField.setValue(searchValue == null ? "" : searchValue.toString());
        searchPoolField.setValue(searchPool == null ? "" : searchPool.toString());
        searchSuccessCounterField.setValue(searchSuccessCounter == null ? "" : searchSuccessCounter.toString());
        searchSuccessIncrementField.setValue(Long.toString(value.searchSuccessIncrement()));
        searchMilestoneEveryField.setValue(Integer.toString(value.searchMilestoneEvery()));
        searchMilestoneValueField.setValue(searchMilestoneValue == null ? "" : searchMilestoneValue.toString());
        searchMilestoneGuaranteedField.setValue(Integer.toString(value.searchMilestoneGuaranteed()));
        if (effectField != null) effectField.setValue(selectedEffect.toString());
    }

    private void cycleEvent() { eventIndex = (eventIndex + 1) % EVENTS.size(); updateLabels(); }
    private void cycleAction() {
        if (activeActions().isEmpty()) return;
        actionIndex = (actionIndex + 1) % actions().length;
        TalentDraft.TriggerAction action = actions()[actionIndex];
        if (action == TalentDraft.TriggerAction.MODIFY_ENTITY_ATTRIBUTE) {
            selectedEffect = new ResourceLocation("minecraft", "generic.attack_damage");
            damageAmountField.setValue("1.0");
            damageMinimumField.setValue("-1.0E9");
            damageMaximumField.setValue("1.0E9");
        } else if (action == TalentDraft.TriggerAction.DAMAGE_ENTITY
                || action == TalentDraft.TriggerAction.HEAL_ENTITY) {
            damageAmountField.setValue("2.0");
        } else if (action == TalentDraft.TriggerAction.ADD_EFFECT
                || action == TalentDraft.TriggerAction.REMOVE_EFFECT) {
            selectedEffect = new ResourceLocation("minecraft", "speed");
        }
        if (!supportsExtendedTarget(action) && targetIndex > 1) targetIndex = 0;
        effectField.setValue(selectedEffect.toString());
        updateLabels();
    }
    private void cycleSearchTarget() {
        searchTargetIndex = (searchTargetIndex + 1) % TalentDraft.DrawSearchTarget.values().length;
        searchValue = null;
        searchValueField.setValue("");
        updateLabels();
    }
    private void cycleSearchQueueMode() {
        searchQueueModeIndex = (searchQueueModeIndex + 1) % SEARCH_QUEUE_MODES.size();
        updateLabels();
    }
    private void cycleSearchMilestoneTarget() {
        searchMilestoneTargetIndex = (searchMilestoneTargetIndex + 1)
                % TalentDraft.DrawSearchTarget.values().length;
        searchMilestoneValue = null;
        searchMilestoneValueField.setValue("");
        updateLabels();
    }

    private boolean storeCurrentAction() {
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        if (activeActions.isEmpty()) return true;
        int duration;
        int amplifier;
        int searchGuaranteed;
        double damageAmount;
        double damageMinimum;
        double damageMaximum;
        double entityRadius;
        try {
            duration = Integer.parseInt(durationField.getValue().strip());
            amplifier = Integer.parseInt(amplifierField.getValue().strip());
            searchGuaranteed = Integer.parseInt(searchGuaranteedField.getValue().strip());
            damageAmount = Double.parseDouble(damageAmountField.getValue().strip());
            damageMinimum = Double.parseDouble(damageMinimumField.getValue().strip());
            damageMaximum = Double.parseDouble(damageMaximumField.getValue().strip());
            entityRadius = Double.parseDouble(entityRadiusField.getValue().strip());
        } catch (NumberFormatException exception) {
            status = Component.translatable("screen.talent_editor.trigger.invalid_number").getString();
            return false;
        }
        TalentDraft.TriggerActionDraft action = captureAction(
                EVENTS.get(eventIndex), duration, amplifier, damageAmount, damageMinimum, damageMaximum,
                entityRadius, searchGuaranteed
        );
        if (action == null) return false;
        activeActions.set(selectedAction, action);
        return true;
    }

    private void previousAction() {
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        if (selectedAction <= 0 || !storeCurrentAction()) return;
        selectedAction--;
        loadAction(activeActions.get(selectedAction));
        updateLabels();
    }

    private void nextAction() {
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        if (selectedAction + 1 >= activeActions.size() || !storeCurrentAction()) return;
        selectedAction++;
        loadAction(activeActions.get(selectedAction));
        updateLabels();
    }

    private void addAction() {
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        if (activeActions.size() >= 256 || !storeCurrentAction()) return;
        selectedAction = activeActions.isEmpty() ? 0 : selectedAction + 1;
        activeActions.add(selectedAction, TalentDraft.TriggerActionDraft.defaults());
        loadAction(activeActions.get(selectedAction));
        status = "";
        updateLabels();
    }

    private void removeAction() {
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        if (activeActions.isEmpty() || !failureBranch && activeActions.size() <= 1) {
            status = Component.translatable("screen.talent_editor.trigger.action_required").getString();
            return;
        }
        activeActions.remove(selectedAction);
        selectedAction = Math.max(0, Math.min(selectedAction, activeActions.size() - 1));
        loadAction(activeActions.isEmpty()
                ? TalentDraft.TriggerActionDraft.defaults() : activeActions.get(selectedAction));
        status = "";
        updateLabels();
    }

    private void moveAction(int direction) {
        if (!storeCurrentAction()) return;
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        int target = selectedAction + direction;
        if (target < 0 || target >= activeActions.size()) return;
        Collections.swap(activeActions, selectedAction, target);
        selectedAction = target;
        updateLabels();
    }

    private ArrayList<TalentDraft.TriggerActionDraft> activeActions() {
        return failureBranch ? editingFailureActions : editingActions;
    }

    private void switchActionBranch() {
        if (!storeCurrentAction()) return;
        failureBranch = !failureBranch;
        selectedAction = 0;
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        loadAction(activeActions.isEmpty()
                ? TalentDraft.TriggerActionDraft.defaults() : activeActions.get(0));
        updateLabels();
    }

    private void cycleActionFailurePolicy() {
        actionFailurePolicyIndex = (actionFailurePolicyIndex + 1) % ACTION_FAILURE_POLICIES.size();
        updateLabels();
    }

    private void toggleOnce() {
        activationLimitField.setValue(activationLimitField.getValue().strip().equals("1") ? "0" : "1");
        updateLabels();
    }
    private void cycleDamageOperation() {
        damageOperationIndex = (damageOperationIndex + 1) % DAMAGE_OPERATIONS.size();
        updateLabels();
    }
    private void switchPage(int next) {
        if (pageIndex == 1 && selected >= 0 && actionButton.active && !storeCurrentAction()) return;
        pageIndex = Math.max(0, Math.min(3, next));
        updateLabels();
    }
    private void cycleTarget() {
        int size = supportsExtendedTarget(actions()[actionIndex]) ? TARGETS.size() : 2;
        targetIndex = (targetIndex + 1) % size;
        updateLabels();
    }
    private void cycleFilter() {
        filterIndex = (filterIndex + 1) % filters().length;
        filterValue = null;
        filterValueField.setValue("");
        updateLabels();
    }
    private void toggleAmbient() { ambient = !ambient; updateLabels(); }
    private void toggleParticles() { showParticles = !showParticles; updateLabels(); }
    private void toggleIcon() { showIcon = !showIcon; updateLabels(); }

    private void updateLabels() {
        ArrayList<TalentDraft.TriggerActionDraft> activeActions = activeActions();
        List<TalentDraft.TriggerRow> rows = working.triggerRows();
        boolean triggerEditable = selected >= 0 && selected < rows.size() && rows.get(selected).editable();
        boolean actionEditable = triggerEditable && !activeActions.isEmpty();
        actionButton.active = actionEditable;
        actionBranchButton.active = triggerEditable;
        actionFailurePolicyButton.active = triggerEditable;
        onceButton.active = triggerEditable;
        eventButton.setMessage(Component.translatable("screen.talent_editor.trigger.event." + EVENTS.get(eventIndex)));
        chanceSourceButton.setMessage(Component.translatable(
                "screen.talent_editor.trigger.chance_source." + CHANCE_SOURCES.get(chanceSourceIndex)
        ));
        actionBranchButton.setMessage(Component.translatable("screen.talent_editor.trigger.branch."
                + (failureBranch ? "failure" : "primary")));
        actionFailurePolicyButton.setMessage(Component.translatable(
                "screen.talent_editor.trigger.failure_policy."
                        + ACTION_FAILURE_POLICIES.get(actionFailurePolicyIndex)));
        onceButton.setMessage(Component.translatable("screen.talent_editor.trigger.once",
                Component.translatable("options." + (activationLimitField.getValue().strip().equals("1")
                        ? "on" : "off"))));
        if (activeActions.isEmpty()) {
            actionButton.setMessage(Component.translatable("screen.talent_editor.trigger.action.empty"));
        } else {
            actionButton.setMessage(Component.literal((selectedAction + 1) + "/" + activeActions.size() + " · ")
                    .append(Component.translatable("screen.talent_editor.trigger.action."
                            + actions()[actionIndex].name().toLowerCase(java.util.Locale.ROOT))));
        }
        searchTargetButton.setMessage(Component.translatable(
                "screen.talent_editor.trigger.search_target."
                        + TalentDraft.DrawSearchTarget.values()[searchTargetIndex].name()
                        .toLowerCase(java.util.Locale.ROOT)
        ));
        searchQueueModeButton.setMessage(Component.translatable(
                "screen.talent_editor.trigger.search_queue_mode." + SEARCH_QUEUE_MODES.get(searchQueueModeIndex)
        ));
        searchMilestoneTargetButton.setMessage(Component.translatable(
                "screen.talent_editor.trigger.search_target."
                        + TalentDraft.DrawSearchTarget.values()[searchMilestoneTargetIndex].name()
                        .toLowerCase(java.util.Locale.ROOT)
        ));
        damageOperationButton.setMessage(Component.translatable("screen.talent_editor.trigger.damage_operation."
                + DAMAGE_OPERATIONS.get(damageOperationIndex)));
        targetButton.setMessage(Component.translatable("screen.talent_editor.trigger.target." + TARGETS.get(targetIndex)));
        TalentDraft.TriggerFilter filter = filters()[filterIndex];
        filterButton.setMessage(Component.translatable("screen.talent_editor.trigger.filter."
                + filter.name().toLowerCase(java.util.Locale.ROOT)));
        if (chooseFilterButton != null) chooseFilterButton.active = filterButton.active && filterIndex > 0;
        boolean addEffect = actions()[actionIndex] == TalentDraft.TriggerAction.ADD_EFFECT;
        boolean modifyDamage = actions()[actionIndex] == TalentDraft.TriggerAction.MODIFY_DAMAGE;
        boolean modifyAttribute = actions()[actionIndex] == TalentDraft.TriggerAction.MODIFY_ENTITY_ATTRIBUTE;
        boolean entityHealth = actions()[actionIndex] == TalentDraft.TriggerAction.DAMAGE_ENTITY
                || actions()[actionIndex] == TalentDraft.TriggerAction.HEAL_ENTITY;
        boolean cancelDamage = actions()[actionIndex] == TalentDraft.TriggerAction.CANCEL_DAMAGE;
        boolean drawConstraint = actions()[actionIndex] == TalentDraft.TriggerAction.QUEUE_DRAW_CONSTRAINT;
        if (!drawConstraint && pageIndex == 3) pageIndex = 1;
        boolean statusEffectAction = addEffect
                || actions()[actionIndex] == TalentDraft.TriggerAction.REMOVE_EFFECT;
        boolean resourceAction = statusEffectAction || modifyAttribute;
        boolean numericAction = modifyDamage || modifyAttribute || entityHealth;
        boolean editableAddEffect = actionEditable && addEffect;
        boolean basePage = pageIndex == 0;
        boolean actionPage = pageIndex == 1;
        boolean conditionPage = pageIndex == 2;
        boolean searchPage = pageIndex == 3;
        searchPageButton.active = actionEditable && drawConstraint;
        idField.visible = basePage;
        priorityField.visible = basePage;
        chanceField.visible = basePage;
        cooldownField.visible = basePage;
        activationLimitField.visible = basePage;
        eventButton.visible = basePage;
        chanceAttributeField.visible = basePage;
        chooseChanceAttributeButton.visible = basePage;
        clearChanceAttributeButton.visible = basePage;
        chanceSourceButton.visible = basePage;
        chancePerPointField.visible = basePage;
        boolean movementEvent = EVENTS.get(eventIndex).equals("player_move_distance");
        distanceIntervalField.visible = basePage && movementEvent;
        actionButton.visible = actionPage;
        actionBranchButton.visible = actionPage;
        previousActionButton.visible = actionPage;
        nextActionButton.visible = actionPage;
        addActionButton.visible = actionPage;
        removeActionButton.visible = actionPage;
        moveActionUpButton.visible = actionPage;
        moveActionDownButton.visible = actionPage;
        effectField.visible = actionPage && resourceAction;
        chooseEffectButton.visible = actionPage && resourceAction;
        chooseEffectButton.active = actionButton.active && resourceAction;
        damageAmountField.visible = actionPage && numericAction;
        damageMinimumField.visible = actionPage && (modifyDamage || modifyAttribute);
        damageMaximumField.visible = actionPage && (modifyDamage || modifyAttribute);
        damageOperationButton.visible = actionPage && (modifyDamage || modifyAttribute);
        damageAmountField.setEditable(actionButton.active && numericAction);
        damageMinimumField.setEditable(actionButton.active && (modifyDamage || modifyAttribute));
        damageMaximumField.setEditable(actionButton.active && (modifyDamage || modifyAttribute));
        damageOperationButton.active = actionButton.active && (modifyDamage || modifyAttribute);
        durationField.visible = actionPage && addEffect;
        amplifierField.visible = actionPage && addEffect;
        durationField.setEditable(editableAddEffect);
        amplifierField.setEditable(editableAddEffect);
        targetButton.visible = conditionPage && (statusEffectAction || supportsExtendedTarget(actions()[actionIndex]));
        targetButton.active = actionButton.active
                && (statusEffectAction || supportsExtendedTarget(actions()[actionIndex]));
        entityRadiusField.visible = conditionPage && supportsExtendedTarget(actions()[actionIndex]);
        entityRadiusField.setEditable(actionButton.active && supportsExtendedTarget(actions()[actionIndex]));
        filterButton.visible = conditionPage;
        filterValueField.visible = conditionPage;
        chooseFilterButton.visible = conditionPage;
        actionFailurePolicyButton.visible = conditionPage;
        onceButton.visible = conditionPage;
        chooseFilterButton.active = actionButton.active && filterIndex > 0;
        ambientButton.visible = actionPage && addEffect;
        particlesButton.visible = actionPage && addEffect;
        iconButton.visible = actionPage && addEffect;
        ambientButton.active = editableAddEffect;
        particlesButton.active = editableAddEffect;
        iconButton.active = editableAddEffect;
        searchTargetButton.visible = actionPage && drawConstraint;
        searchTargetButton.active = actionButton.active && drawConstraint;
        searchGuaranteedField.visible = actionPage && drawConstraint;
        searchGuaranteedField.setEditable(actionButton.active && drawConstraint);
        searchValueField.visible = actionPage && drawConstraint;
        chooseSearchValueButton.visible = actionPage && drawConstraint;
        boolean primaryTag = TalentDraft.DrawSearchTarget.values()[searchTargetIndex]
                == TalentDraft.DrawSearchTarget.TAG;
        searchValueField.setEditable(actionButton.active && drawConstraint && primaryTag);
        chooseSearchValueButton.active = actionButton.active && drawConstraint && !primaryTag;
        searchPoolField.visible = actionPage && drawConstraint;
        chooseSearchPoolButton.visible = actionPage && drawConstraint;
        chooseSearchPoolButton.active = actionButton.active && drawConstraint;
        clearSearchPoolButton.visible = actionPage && drawConstraint;
        clearSearchPoolButton.active = actionButton.active && drawConstraint && searchPool != null;
        searchQueueModeButton.visible = searchPage && drawConstraint;
        searchSuccessCounterField.visible = searchPage && drawConstraint;
        chooseSearchSuccessCounterButton.visible = searchPage && drawConstraint;
        clearSearchSuccessCounterButton.visible = searchPage && drawConstraint;
        searchSuccessIncrementField.visible = searchPage && drawConstraint;
        searchMilestoneEveryField.visible = searchPage && drawConstraint;
        searchMilestoneTargetButton.visible = searchPage && drawConstraint;
        searchMilestoneGuaranteedField.visible = searchPage && drawConstraint;
        searchMilestoneValueField.visible = searchPage && drawConstraint;
        chooseSearchMilestoneValueButton.visible = searchPage && drawConstraint;
        boolean milestoneTag = TalentDraft.DrawSearchTarget.values()[searchMilestoneTargetIndex]
                == TalentDraft.DrawSearchTarget.TAG;
        searchQueueModeButton.active = actionButton.active && drawConstraint;
        chooseSearchSuccessCounterButton.active = actionButton.active && drawConstraint;
        clearSearchSuccessCounterButton.active = actionButton.active && drawConstraint
                && searchSuccessCounter != null;
        searchSuccessIncrementField.setEditable(actionButton.active && drawConstraint
                && searchSuccessCounter != null);
        searchMilestoneEveryField.setEditable(actionButton.active && drawConstraint
                && searchSuccessCounter != null);
        searchMilestoneTargetButton.active = actionButton.active && drawConstraint
                && searchSuccessCounter != null;
        searchMilestoneGuaranteedField.setEditable(actionButton.active && drawConstraint
                && searchSuccessCounter != null);
        searchMilestoneValueField.setEditable(actionButton.active && drawConstraint
                && searchSuccessCounter != null && milestoneTag);
        chooseSearchMilestoneValueButton.active = actionButton.active && drawConstraint
                && searchSuccessCounter != null && !milestoneTag;
        previousActionButton.active = actionEditable && selectedAction > 0;
        nextActionButton.active = actionEditable && selectedAction + 1 < activeActions.size();
        addActionButton.active = triggerEditable && activeActions.size() < 256;
        removeActionButton.active = actionEditable && (failureBranch || activeActions.size() > 1);
        moveActionUpButton.active = actionEditable && selectedAction > 0;
        moveActionDownButton.active = actionEditable && selectedAction + 1 < activeActions.size();
        ResourceLocation chanceSource = chanceSource();
        chooseChanceAttributeButton.active = triggerEditable;
        chanceSourceButton.active = triggerEditable;
        clearChanceAttributeButton.active = triggerEditable && chanceSource != null;
        chancePerPointField.setEditable(triggerEditable && chanceSource != null);
        distanceIntervalField.setEditable(triggerEditable && movementEvent);
        ambientButton.setMessage(toggleLabel("ambient", ambient));
        particlesButton.setMessage(toggleLabel("particles", showParticles));
        iconButton.setMessage(toggleLabel("icon_visible", showIcon));
    }

    private Component toggleLabel(String key, boolean value) {
        return Component.translatable("screen.talent_editor.trigger." + key,
                Component.translatable("options." + (value ? "on" : "off")));
    }

    private void chooseEffect() {
        if (minecraft == null || !storeSelected()) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(this,
                actions()[actionIndex] == TalentDraft.TriggerAction.MODIFY_ENTITY_ATTRIBUTE
                        ? ResourceReferencePickerScreen.Source.ATTRIBUTE
                        : ResourceReferencePickerScreen.Source.MOB_EFFECT,
                bootstrap, this::replaceEffect));
    }

    private void chooseChanceAttribute() {
        if (minecraft == null || !storeSelected()) return;
        ResourceReferencePickerScreen.Source source = switch (chanceSourceIndex) {
            case 0 -> ResourceReferencePickerScreen.Source.ATTRIBUTE;
            case 1 -> ResourceReferencePickerScreen.Source.COUNTER;
            default -> ResourceReferencePickerScreen.Source.CATEGORY;
        };
        minecraft.setScreen(new ResourceReferencePickerScreen(this,
                source, bootstrap, this::replaceChanceSource));
    }

    private void replaceChanceSource(ResourceLocation value) {
        if (chanceSourceIndex == 0) {
            chanceAttribute = value;
            chanceCounter = null;
            chanceCategory = null;
        } else if (chanceSourceIndex == 1) {
            chanceCounter = value;
            chanceAttribute = null;
            chanceCategory = null;
        } else {
            chanceCategory = value;
            chanceAttribute = null;
            chanceCounter = null;
        }
        chanceAttributeField.setValue(value.toString());
        chancePerPointField.setEditable(true);
        storeSelected();
    }

    private void clearChanceAttribute() {
        chanceAttribute = null;
        chanceCounter = null;
        chanceCategory = null;
        chanceAttributeField.setValue("");
        chancePerPointField.setValue("0.0");
        chancePerPointField.setEditable(false);
        updateLabels();
    }

    private void cycleChanceSource() {
        chanceSourceIndex = (chanceSourceIndex + 1) % CHANCE_SOURCES.size();
        chanceAttribute = null;
        chanceCounter = null;
        chanceCategory = null;
        chanceAttributeField.setValue("");
        chancePerPointField.setValue("0.0");
        updateLabels();
    }

    private ResourceLocation chanceSource() {
        return switch (chanceSourceIndex) {
            case 0 -> chanceAttribute;
            case 1 -> chanceCounter;
            default -> chanceCategory;
        };
    }

    private void replaceEffect(ResourceLocation effect) {
        selectedEffect = effect;
        effectField.setValue(effect.toString());
        storeSelected();
    }

    private void chooseSearchValue() {
        if (minecraft == null || selected < 0) return;
        ResourceReferencePickerScreen.Source source = searchSource(
                TalentDraft.DrawSearchTarget.values()[searchTargetIndex]
        );
        if (source == null) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(this, source, bootstrap, this::replaceSearchValue));
    }

    private void replaceSearchValue(ResourceLocation value) {
        searchValue = value;
        searchValueField.setValue(value.toString());
        storeSelected();
    }

    private void chooseSearchPool() {
        if (minecraft == null || selected < 0) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.POOL, bootstrap, this::replaceSearchPool
        ));
    }

    private void replaceSearchPool(ResourceLocation value) {
        searchPool = value;
        searchPoolField.setValue(value.toString());
        storeSelected();
    }

    private void clearSearchPool() {
        searchPool = null;
        searchPoolField.setValue("");
        storeSelected();
        updateLabels();
    }

    private void chooseSearchSuccessCounter() {
        if (minecraft == null || selected < 0) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.COUNTER, bootstrap, this::replaceSearchSuccessCounter
        ));
    }

    private void replaceSearchSuccessCounter(ResourceLocation value) {
        searchSuccessCounter = value;
        searchSuccessCounterField.setValue(value.toString());
        if (searchSuccessIncrementField.getValue().strip().equals("0")) {
            searchSuccessIncrementField.setValue("1");
        }
        storeSelected();
        updateLabels();
    }

    private void clearSearchSuccessCounter() {
        searchSuccessCounter = null;
        searchSuccessCounterField.setValue("");
        searchSuccessIncrementField.setValue("0");
        searchMilestoneEveryField.setValue("0");
        searchMilestoneValue = null;
        searchMilestoneValueField.setValue("");
        updateLabels();
    }

    private void chooseSearchMilestoneValue() {
        if (minecraft == null || selected < 0) return;
        ResourceReferencePickerScreen.Source source = searchSource(
                TalentDraft.DrawSearchTarget.values()[searchMilestoneTargetIndex]
        );
        if (source == null) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, source, bootstrap, this::replaceSearchMilestoneValue
        ));
    }

    private void replaceSearchMilestoneValue(ResourceLocation value) {
        searchMilestoneValue = value;
        searchMilestoneValueField.setValue(value.toString());
        if (searchMilestoneEveryField.getValue().strip().equals("0")) {
            searchMilestoneEveryField.setValue("1");
        }
        storeSelected();
    }

    private static ResourceReferencePickerScreen.Source searchSource(TalentDraft.DrawSearchTarget target) {
        return switch (target) {
            case TALENT -> ResourceReferencePickerScreen.Source.TALENT;
            case CATEGORY -> ResourceReferencePickerScreen.Source.CATEGORY;
            case RARITY -> ResourceReferencePickerScreen.Source.RARITY;
            case TAG -> null;
        };
    }

    private void chooseFilter() {
        if (minecraft == null || selected < 0 || filterIndex == 0) return;
        ResourceReferencePickerScreen.Source source = switch (filters()[filterIndex]) {
            case ENTITY_TYPE -> ResourceReferencePickerScreen.Source.ENTITY_TYPE;
            case ENTITY_TAG -> ResourceReferencePickerScreen.Source.ENTITY_TYPE_TAG;
            case DAMAGE_TAG -> ResourceReferencePickerScreen.Source.DAMAGE_TYPE_TAG;
            case ITEM -> ResourceReferencePickerScreen.Source.ITEM;
            case ITEM_TAG -> ResourceReferencePickerScreen.Source.ITEM_TAG;
            case BLOCK -> ResourceReferencePickerScreen.Source.BLOCK;
            case BLOCK_TAG -> ResourceReferencePickerScreen.Source.BLOCK_TAG;
            default -> throw new IllegalStateException();
        };
        minecraft.setScreen(new ResourceReferencePickerScreen(this, source, bootstrap, this::replaceFilter));
    }

    private void replaceFilter(ResourceLocation value) {
        filterValue = value;
        filterValueField.setValue(value.toString());
        storeSelected();
    }

    private void apply() {
        if (!storeSelected()) return;
        List<TalentDraft.TriggerRow> rows = working.triggerRows();
        int selectedRawIndex = selected >= 0 && selected < rows.size() ? rows.get(selected).rawIndex() : -1;
        owner.applyTriggerDraft(working, selectedRawIndex);
        if (minecraft != null) minecraft.setScreen(returnScreen);
    }

    private void returnWithoutApplying() {
        if (minecraft != null) minecraft.setScreen(returnScreen);
    }

    @Override
    public void onClose() { returnWithoutApplying(); }

    private static TalentDraft.TriggerFilter[] filters() { return TalentDraft.TriggerFilter.values(); }
    private static TalentDraft.TriggerAction[] actions() { return TalentDraft.TriggerAction.values(); }

    private static boolean isDamageEvent(String event) {
        return event.equals("damage_dealt_pre") || event.equals("damage_taken_pre")
                || event.equals("damage_dealt") || event.equals("damage_taken");
    }

    private static boolean supportsExtendedTarget(TalentDraft.TriggerAction action) {
        return action == TalentDraft.TriggerAction.MODIFY_ENTITY_ATTRIBUTE
                || action == TalentDraft.TriggerAction.DAMAGE_ENTITY
                || action == TalentDraft.TriggerAction.HEAL_ENTITY;
    }
}
