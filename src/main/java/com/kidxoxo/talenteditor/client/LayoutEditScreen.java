package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.ValidationResultPacket;
import com.kidxoxo.talenteditor.validation.ValidationIssue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.List;

public final class LayoutEditScreen extends EditorDefinitionScreen {
    private final EditorBootstrap bootstrap;
    private final LayoutDraft draft;
    private ResourceLocation definitionId;
    private boolean existing;
    private long baseRevision;
    private boolean enabled;
    private boolean showGeneratedEffects;
    private List<LayoutDraft.SlotDraft> slots;
    private List<LayoutDraft.PageDraft> pages;
    private String idValue;
    private String backgroundValue;
    private String priorityValue;
    private String panelPercentValue;
    private String columnsValue;
    private String rowsValue;
    private String slotWidthValue;
    private String slotHeightValue;
    private String iconSizeValue;
    private Optional<com.kidxoxo.talenteditor.domain.UiDocument> bookUi;
    private Optional<com.kidxoxo.talenteditor.domain.UiDocument> drawUi;
    private EditBox idField;
    private String localStatus = "";
    private boolean dirty;

    private LayoutEditScreen(EditorBootstrap bootstrap, LayoutDraft draft, ResourceLocation id, boolean existing) {
        super(Component.translatable(existing
                ? "screen.talent_editor.layout.edit.title" : "screen.talent_editor.layout.create.title"));
        this.bootstrap = bootstrap;
        this.draft = draft;
        this.bookUi = draft.ui(false);
        this.drawUi = draft.ui(true);
        this.definitionId = id;
        this.existing = existing;
        this.baseRevision = bootstrap.revision();
        this.enabled = draft.enabled();
        this.showGeneratedEffects = draft.showGeneratedEffects();
        this.slots = draft.slots();
        this.pages = draft.pages();
        this.idValue = id.toString();
        this.backgroundValue = draft.background();
        this.priorityValue = Integer.toString(draft.priority());
        this.panelPercentValue = Integer.toString(draft.panelPercent());
        this.columnsValue = Integer.toString(draft.columns());
        this.rowsValue = Integer.toString(draft.rows());
        this.slotWidthValue = Integer.toString(draft.slotWidth());
        this.slotHeightValue = Integer.toString(draft.slotHeight());
        this.iconSizeValue = Integer.toString(draft.iconSize());
    }

    public static LayoutEditScreen createNew(EditorBootstrap bootstrap) {
        return new LayoutEditScreen(
                bootstrap, LayoutDraft.create(), new ResourceLocation("custom", "new_layout"), false
        );
    }

    public static LayoutEditScreen editExisting(EditorBootstrap bootstrap, ResourceLocation id, String json) {
        return new LayoutEditScreen(bootstrap, LayoutDraft.parse(json), id, true);
    }

    static LayoutEditScreen copyOf(EditorBootstrap bootstrap, ResourceLocation id, String json) {
        return new LayoutEditScreen(bootstrap,LayoutDraft.parse(json),id,false);
    }

    @Override
    protected void init() {
        int formWidth=Math.min(700,width-32),x=(width-formWidth)/2,gap=6;
        int priorityWidth=Math.max(65,formWidth/5);
        idField=field(x,38,formWidth-priorityWidth-gap,idValue,256,value->idValue=value);
        idField.setEditable(!existing);
        field(x+formWidth-priorityWidth,38,priorityWidth,priorityValue,8,value->priorityValue=value);
        int half=(formWidth-gap)/2;
        addRenderableWidget(EditorWidgets.button(enabledLabel(),b->{dirty=true;enabled=!enabled;b.setMessage(enabledLabel());ClientEditorState.clearValidation();})
                .bounds(x,66,half,20).build());
        addRenderableWidget(EditorWidgets.button(effectsLabel(),b->{dirty=true;showGeneratedEffects=!showGeneratedEffects;b.setMessage(effectsLabel());ClientEditorState.clearValidation();})
                .bounds(x+half+gap,66,formWidth-half-gap,20).build());
        addRenderableWidget(EditorWidgets.button(UiCanvasEditorScreen.tr("book_editor"),b->openUi(false)).bounds(x,107,half,24).build());
        addRenderableWidget(EditorWidgets.button(UiCanvasEditorScreen.tr("draw_editor"),b->openUi(true)).bounds(x+half+gap,107,formWidth-half-gap,24).build());
        addRenderableWidget(EditorWidgets.button(UiCanvasEditorScreen.tr("page_contents",pages.size()),b->openPageEditor()).bounds(x,160,formWidth,20).build());
        int bottom=height-28,bw=(formWidth-gap*3)/4;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"),b->returnList()).bounds(x,bottom,bw,20).build());
        Button references=addRenderableWidget(EditorWidgets.button(DefinitionReferenceScreen.buttonLabel(DefinitionReferenceScreen.inboundCount(bootstrap,DefinitionKind.LAYOUT,definitionId)),b->definitionActions(bootstrap,ResourceLocation.tryParse(idField.getValue().strip()),existing,this::openReferences)).bounds(x+bw+gap,bottom,bw,20).build());
        references.active=true;
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.validate"),b->submit(false)).bounds(x+2*(bw+gap),bottom,bw,20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.save"), EditorTheme.Tone.PRIMARY,b ->submit(true)).bounds(x+3*(bw+gap),bottom,formWidth-3*(bw+gap),20).build());
        watchDefinition(DefinitionKind.LAYOUT);
    }

    private EditBox field(int x, int y, int width, String value, int maxLength,
                          java.util.function.Consumer<String> responder) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(next -> {
            dirty=true;
            responder.accept(next);
            ClientEditorState.clearValidation();
            localStatus = "";
        });
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics g,int mx,int my,float tick) {
        renderBackground(g);
        int formWidth=Math.min(700,width-32),x=(width-formWidth)/2;
        EditorTheme.panel(g,x-6,94,formWidth+12,92);
        super.render(g,mx,my,tick);
        EditorTheme.title(g,font,title,width);
        g.drawString(font,Component.translatable("screen.talent_editor.layout.id"),x,26,EditorTheme.MUTED,false);
        int priorityWidth=Math.max(65,formWidth/5);
        g.drawString(font,Component.translatable("screen.talent_editor.layout.priority"),x+formWidth-priorityWidth,26,EditorTheme.MUTED,false);
        int half=(formWidth-6)/2;
        g.drawString(font,font.plainSubstrByWidth(UiCanvasEditorScreen.tr(bookUi.isPresent()?"custom_base":"current_base").getString(),half),x,138,EditorTheme.MUTED,false);
        g.drawString(font,font.plainSubstrByWidth(UiCanvasEditorScreen.tr(drawUi.isPresent()?"custom_base":"current_base").getString(),half),x+half+6,138,EditorTheme.MUTED,false);
        if(height>=290) g.drawWordWrap(font,UiCanvasEditorScreen.tr("layout_flow"),x,194,formWidth,EditorTheme.MUTED);
        renderResult(g,x,formWidth);
    }

    private void renderResult(GuiGraphics graphics, int x, int formWidth) {
        renderSubmissionResult(graphics, x, formWidth, localStatus);
    }

    private void submit(boolean commit) {
        if(submissionPending())return;
        Parsed parsed = parseForm();
        if (parsed == null) return;
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        sendDefinition(baseRevision, parsed.id(), parsed.json(), commit);
    }

    private Parsed parseForm() {
        String rawId = idValue.strip();
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null || !rawId.contains(":")) return invalid("screen.talent_editor.layout.invalid_id");
        Optional<ResourceLocation> background = Optional.empty();
        String rawBackground = backgroundValue.strip();
        if (!rawBackground.isBlank()) {
            ResourceLocation parsedBackground = ResourceLocation.tryParse(rawBackground);
            if (parsedBackground == null || !rawBackground.contains(":")) {
                return invalid("screen.talent_editor.layout.invalid_background");
            }
            background = Optional.of(parsedBackground);
        }
        int priority = parseInt(priorityValue, -100_000, 100_000);
        int panel = parseInt(panelPercentValue, 25, 70);
        int columns = parseInt(columnsValue, 1, 8);
        int rows = parseInt(rowsValue, 1, 10);
        int slotWidth = parseInt(slotWidthValue, 24, 128);
        int slotHeight = parseInt(slotHeightValue, 22, 96);
        int iconSize = parseInt(iconSizeValue, 8, 64);
        if (priority == Integer.MIN_VALUE || panel < 0 || columns < 0 || rows < 0
                || slotWidth < 0 || slotHeight < 0 || iconSize < 0 || iconSize > slotHeight - 4) {
            return invalid("screen.talent_editor.layout.invalid_geometry");
        }
        String json = LayoutDraft.parse(draft.snapshot()).toJson(
                enabled, priority, rawBackground, panel, columns, rows, slotWidth, slotHeight,
                iconSize, showGeneratedEffects, slots, pages
        );
        com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        if (bookUi.isPresent()) root.add("bookUi", com.kidxoxo.talenteditor.compile.UiDocumentCodec.toJson(bookUi.orElseThrow()));
        else root.remove("bookUi");
        if (drawUi.isPresent()) root.add("drawUi", com.kidxoxo.talenteditor.compile.UiDocumentCodec.toJson(drawUi.orElseThrow()));
        else root.remove("drawUi");
        json = root.toString();
        if (json.length() > com.kidxoxo.talenteditor.editor.DefinitionService.MAX_JSON_CHARACTERS) {
            localStatus = UiCanvasEditorScreen.tr("too_large").getString(); return null;
        }
        List<TalentBookLayoutDefinition.SlotDefinition> slotDefinitions = slots.stream()
                .map(LayoutDraft.SlotDraft::definition)
                .toList();
        List<TalentBookLayoutDefinition.PageDefinition> pageDefinitions = pages.stream()
                .map(page -> new TalentBookLayoutDefinition.PageDefinition(
                        page.id(), page.background().isBlank()
                                ? Optional.empty() : Optional.of(new ResourceLocation(page.background())),
                        page.slots().stream().map(LayoutDraft.SlotDraft::definition).toList()
                ))
                .toList();
        TalentBookLayoutDefinition definition = new TalentBookLayoutDefinition(
                id, 2, enabled, priority, background, panel, columns, rows,
                slotWidth, slotHeight, iconSize, showGeneratedEffects, slotDefinitions, pageDefinitions, bookUi, drawUi
        );
        return new Parsed(id, json, definition);
    }

    private void openUi(boolean draw) {
        if (minecraft == null) return;
        var current = draw ? drawUi : bookUi;
        com.kidxoxo.talenteditor.domain.UiDocument initial;
        if (current.isPresent()) {
            // A draft that exceeds the submission limit must remain editable so it can be reduced.
            initial = current.orElseThrow();
        } else {
            initial = baseUi(draw);
        }
        minecraft.setScreen(new UiCanvasEditorScreen(this, draw, initial, value -> {
                    dirty=true;
                    if (draw) drawUi = value; else bookUi = value;
                    ClientEditorState.clearValidation();
                    localStatus = UiCanvasEditorScreen.tr("applied_hint").getString();
                }));
    }

    private Parsed invalid(String key) {
        ClientEditorState.clearValidation();
        localStatus = Component.translatable(key).getString();
        return null;
    }

    private Component enabledLabel() {
        return Component.translatable(enabled
                ? "screen.talent_editor.layout.enabled_yes" : "screen.talent_editor.layout.enabled_no");
    }

    private Component effectsLabel() {
        return Component.translatable(showGeneratedEffects
                ? "screen.talent_editor.layout.effects_yes" : "screen.talent_editor.layout.effects_no");
    }

    EditorBootstrap bootstrap() { return bootstrap; }
    com.kidxoxo.talenteditor.domain.UiDocument baseUi(boolean draw) {
        var legacy = new TalentBookLayoutDefinition(definitionId,2,enabled,0,
                backgroundValue.isBlank()?Optional.empty():Optional.ofNullable(ResourceLocation.tryParse(backgroundValue)),draft.panelPercent(),draft.columns(),draft.rows(),
                draft.slotWidth(),draft.slotHeight(),draft.iconSize(),showGeneratedEffects,
                slots.stream().map(LayoutDraft.SlotDraft::definition).toList(),
                pages.stream().map(page->new TalentBookLayoutDefinition.PageDefinition(page.id(),
                        page.background().isBlank()?Optional.empty():Optional.of(new ResourceLocation(page.background())),
                        page.slots().stream().map(LayoutDraft.SlotDraft::definition).toList())).toList());
        return UiTemplates.create(draw,legacy,ClientEditorState.editorSettings().automaticSynthesis());
    }

    List<LayoutDraft.PageDraft> pages() { return pages; }
    int requiredPageSlots() { return pages.stream().mapToInt(page->page.slots().size()).max().orElse(0); }

    void editPageContents() { openPageEditor(); }

    private void openPageEditor() {
        if (minecraft != null) {
            minecraft.setScreen(new LayoutPageEditorScreen(this, pages, defaultPageSlots()));
        }
    }

    void updatePages(List<LayoutDraft.PageDraft> value) {
        dirty=true;
        pages = List.copyOf(value);
        ClientEditorState.clearValidation();
    }

    private List<LayoutDraft.SlotDraft> defaultPageSlots() {
        var source=slots.isEmpty() ? gridTemplate() : slots;
        int capacity=bookUi.map(com.kidxoxo.talenteditor.domain.UiDocument::slotCount)
                .orElse(Math.max(source.size(),requiredPageSlots()));
        return LayoutContentSlots.template(source,capacity);
    }

    private List<LayoutDraft.SlotDraft> gridTemplate() {
        int columns = validOr(columnsValue, 1, 8, draft.columns());
        int rows = validOr(rowsValue, 1, 10, draft.rows());
        int width = validOr(slotWidthValue, 24, 128, draft.slotWidth());
        int height = validOr(slotHeightValue, 22, 96, draft.slotHeight());
        int icon = Math.min(validOr(iconSizeValue, 8, 64, draft.iconSize()), height - 4);
        java.util.ArrayList<LayoutDraft.SlotDraft> result = new java.util.ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                result.add(new LayoutDraft.SlotDraft(
                        "grid_" + row + "_" + column,
                        column * width, row * height, width, height, icon
                ));
            }
        }
        return List.copyOf(result);
    }

    private static int validOr(String value, int minimum, int maximum, int fallback) {
        int parsed = parseInt(value, minimum, maximum);
        return parsed == Integer.MIN_VALUE ? fallback : parsed;
    }

    private void openReferences() {
        EditorBootstrap bootstrap=latestBootstrap(this.bootstrap);
        if (!existing || minecraft == null) return;
        int count = DefinitionReferenceScreen.inboundCount(bootstrap, DefinitionKind.LAYOUT, definitionId);
        minecraft.setScreen(new DefinitionReferenceScreen(
                this, bootstrap, DefinitionKind.LAYOUT, definitionId, count
        ));
    }

    private void returnList() { leaveDefinition(this::leaveNow); }

    private void leaveNow() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new LayoutListScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() { returnList(); }

    private static int parseInt(String value, int minimum, int maximum) {
        try {
            int parsed = Integer.parseInt(value.strip());
            return parsed >= minimum && parsed <= maximum ? parsed : Integer.MIN_VALUE;
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private record Parsed(ResourceLocation id, String json, TalentBookLayoutDefinition definition) {
    }

    @Override protected String draftState() { return fieldsState()+enabled+showGeneratedEffects+slots+pages+bookUi+drawUi+draft.snapshot(); }
    @Override protected void saveDraft() { submit(true); }
    @Override protected void committed(ValidationResultPacket value) { baseRevision=value.activeRevision(); existing=true; idField.setEditable(false); var saved=ResourceLocation.tryParse(idField.getValue().strip()); if(saved!=null)definitionId=saved; dirty=false; }
}
