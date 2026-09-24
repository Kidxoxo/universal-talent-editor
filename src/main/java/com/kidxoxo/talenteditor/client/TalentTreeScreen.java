package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Interactive overview of talent acquisition and synthesis relationships. */
public final class TalentTreeScreen extends Screen {
    private static final int ACQUIRE_COLOR = 0xFF58C7E8;
    private static final int SYNTHESIS_COLOR = 0xFFFFB84D;
    private static final int DISABLED_COLOR = 0xFF6E7785;
    private static final double MIN_ZOOM = 0.45D;
    private static final double MAX_ZOOM = 1.55D;

    private final EditorBootstrap bootstrap;
    private final TalentTreeLayout.Result layout;
    private final Map<ResourceLocation, EditorBootstrap.TalentSummary> talents = new LinkedHashMap<>();
    private final List<Edge> edges;
    private EditBox searchField;
    private Button relationFilterButton;
    private Button editTalentButton;
    private Button editAcquireButton;
    private Button editSynthesisButton;
    private Button addAcquireButton;
    private Button addSynthesisButton;
    private Button editEdgeButton;
    private Button removeEdgeButton;
    private Button edgeVisibilityButton;
    private ResourceLocation selectedTalent;
    private Edge selectedEdge;
    private RelationFilter relationFilter = RelationFilter.ALL;
    private boolean showAllEdges;
    private double zoom = 1.0D;
    private double panX;
    private double panY;
    private boolean viewInitialized;
    private boolean panning;
    private int panButton = -1;
    private long lastEdgeClick;
    private long lastNodeClick;
    private String searchValue = "";
    private String status = "";

    public TalentTreeScreen(EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.tree.title"));
        this.bootstrap = bootstrap;
        bootstrap.talents().stream().sorted(Comparator.comparing(value -> value.id().toString()))
                .forEach(talent -> talents.put(talent.id(), talent));
        this.layout = TalentTreeLayout.arrange(bootstrap.talents());
        this.edges = buildEdges(bootstrap.talents());
    }

    @Override
    protected void init() {
        int left = canvasLeft();
        int right = canvasRight();
        int y = 28;
        int locateWidth = 44;
        int filterWidth = 92;
        int small = 24;
        int resetWidth = 44;
        int gaps = 5 * 4;
        int searchWidth = Math.max(54, right - left - locateWidth - filterWidth - small * 2
                - resetWidth - gaps);
        searchField = new EditBox(font, left, y, searchWidth, 20,
                Component.translatable("screen.talent_editor.tree.search"));
        searchField.setHint(Component.translatable("screen.talent_editor.tree.search"));
        searchField.setValue(searchValue);
        searchField.setResponder(value -> searchValue = value);
        addRenderableWidget(searchField);
        int x = left + searchWidth + 4;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.tree.locate"),
                button -> locateSearch()).bounds(x, y, locateWidth, 20).build());
        x += locateWidth + 4;
        relationFilterButton = addRenderableWidget(Button.builder(filterLabel(), button -> cycleFilter())
                .bounds(x, y, filterWidth, 20).build());
        x += filterWidth + 4;
        addRenderableWidget(Button.builder(Component.literal("−"), button -> zoomAtCenter(1.0D / 1.2D))
                .bounds(x, y, small, 20).build());
        x += small + 4;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.tree.reset"),
                button -> resetView()).bounds(x, y, resetWidth, 20).build());
        x += resetWidth + 4;
        addRenderableWidget(Button.builder(Component.literal("+"), button -> zoomAtCenter(1.2D))
                .bounds(x, y, Math.max(small, right - x), 20).build());

        int sideX = canvasRight() + 9;
        int sideWidth = Math.max(90, width - sideX - 8);
        int gap = 4;
        int half = (sideWidth - gap) / 2;
        editTalentButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.tree.edit_talent"),
                        button -> editSelected(TalentTreeEditRequest.Page.TALENT))
                .bounds(sideX, 55, sideWidth, 20).build());
        editAcquireButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.tree.edit_acquire"),
                        button -> editSelected(TalentTreeEditRequest.Page.ACQUISITION))
                .bounds(sideX, 79, half, 20).build());
        editSynthesisButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.tree.edit_synthesis"),
                        button -> editSelected(TalentTreeEditRequest.Page.SYNTHESIS))
                .bounds(sideX + half + gap, 79, sideWidth - half - gap, 20).build());
        addAcquireButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.tree.add_acquire"),
                        button -> addRelation(RelationType.ACQUISITION))
                .bounds(sideX, 103, half, 20).build());
        addSynthesisButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.tree.add_synthesis"),
                        button -> addRelation(RelationType.SYNTHESIS))
                .bounds(sideX + half + gap, 103, sideWidth - half - gap, 20).build());
        editEdgeButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.tree.edit_relation"),
                        button -> editSelectedEdge())
                .bounds(sideX, 127, half, 20).build());
        removeEdgeButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.tree.remove_relation"),
                        button -> removeSelectedEdge())
                .bounds(sideX + half + gap, 127, sideWidth - half - gap, 20).build());
        edgeVisibilityButton = addRenderableWidget(Button.builder(edgeVisibilityLabel(), button -> {
                    showAllEdges = !showAllEdges;
                    edgeVisibilityButton.setMessage(edgeVisibilityLabel());
                }).bounds(sideX, 151, sideWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnHome())
                .bounds(sideX, height - 28, sideWidth, 20).build());
        if (!viewInitialized) resetView();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = canvasLeft();
        int top = canvasTop();
        int right = canvasRight();
        int bottom = canvasBottom();
        EditorTheme.panel(graphics, left - 2, top - 2, Math.max(1, right - left + 4),
                Math.max(1, bottom - top + 4));
        EditorTheme.panel(graphics, right + 6, 28, Math.max(1, width - right - 12),
                Math.max(1, height - 62));
        graphics.enableScissor(left, top, right, bottom);
        renderEdges(graphics, mouseX, mouseY);
        renderNodes(graphics, mouseX, mouseY);
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 9, EditorTheme.TEXT);
        renderSidebar(graphics, right + 10);
        graphics.drawString(font, Component.translatable("screen.talent_editor.tree.legend.acquire"),
                left + 4, bottom - 12, ACQUIRE_COLOR, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.tree.legend.synthesis"),
                left + 112, bottom - 12, SYNTHESIS_COLOR, false);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, Math.max(40, right - left - 8)),
                    left + 4, top + 4, 0xFFFFAA55, false);
        }
    }

    private void renderEdges(GuiGraphics graphics, int mouseX, int mouseY) {
        EditorBootstrap.TalentSummary hovered = nodeAt(mouseX, mouseY);
        Edge hoveredEdge = hovered == null ? edgeAt(mouseX, mouseY) : null;
        ResourceLocation focus = selectedTalent != null ? selectedTalent
                : hovered == null ? null : hovered.id();
        List<Edge> visible = visibleEdges(focus);
        for (int pass = 0; pass < 2; pass++) for (Edge edge : visible) {
            boolean related = relatedTo(edge, focus);
            if (showAllEdges && (pass == 0) == related) continue;
            if (!showAllEdges && pass != 0) continue;
            EdgeGeometry geometry = geometry(edge);
            if (geometry == null) continue;
            boolean selected = edge.equals(selectedEdge) || edge.equals(hoveredEdge);
            int color = edge.active() ? edge.type().color() : DISABLED_COLOR;
            if (showAllEdges && !related && !selected) color = (color & 0x00FFFFFF) | 0x55000000;
            int thickness = selected ? 3 : 2;
            drawSegment(graphics, geometry.sourceX(), geometry.sourceY(), geometry.middleX(),
                    geometry.sourceY(), color, thickness, edge.type() == RelationType.SYNTHESIS);
            drawSegment(graphics, geometry.middleX(), geometry.sourceY(), geometry.middleX(),
                    geometry.targetY(), color, thickness, edge.type() == RelationType.SYNTHESIS);
            drawSegment(graphics, geometry.middleX(), geometry.targetY(), geometry.targetX(),
                    geometry.targetY(), color, thickness, edge.type() == RelationType.SYNTHESIS);
            int direction = geometry.targetX() >= geometry.middleX() ? -1 : 1;
            graphics.fill(geometry.targetX() + direction * 5, geometry.targetY() - 3,
                    geometry.targetX() + direction, geometry.targetY() + 4, color);
        }
    }

    private void renderNodes(GuiGraphics graphics, int mouseX, int mouseY) {
        for (EditorBootstrap.TalentSummary talent : talents.values()) {
            NodeBounds bounds = nodeBounds(talent.id());
            if (bounds == null || bounds.right() < canvasLeft() || bounds.x() > canvasRight()
                    || bounds.bottom() < canvasTop() || bounds.y() > canvasBottom()) continue;
            boolean selected = talent.id().equals(selectedTalent);
            boolean hovered = bounds.contains(mouseX, mouseY);
            int rarity = rarityColor(talent.rarity());
            graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(),
                    selected ? 0xE02A415D : hovered ? 0xD024344B : 0xD016202F);
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + Math.max(2, (int) Math.round(4 * zoom)),
                    bounds.bottom(), talent.enabled() && talent.available() ? rarity : DISABLED_COLOR);
            if (selected) {
                graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 2, EditorTheme.ACCENT);
                graphics.fill(bounds.x(), bounds.bottom() - 2, bounds.right(), bounds.bottom(), EditorTheme.ACCENT);
            }
            int iconSize = Math.max(10, Math.min(24, (int) Math.round(22 * zoom)));
            int iconX = bounds.x() + Math.max(5, (int) Math.round(8 * zoom));
            int iconY = bounds.y() + Math.max(5, (bounds.height() - iconSize) / 2);
            boolean iconDrawn = talent.icon().isPresent() && minecraft != null
                    && minecraft.getResourceManager().getResource(talent.icon().orElseThrow()).isPresent();
            if (iconDrawn) renderIcon(graphics, talent.icon().orElseThrow(), iconX, iconY, iconSize);
            else {
                graphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0xFF303B4F);
                String initial = talent.name().isBlank() ? "?" : talent.name().substring(0, 1);
                graphics.drawCenteredString(font, initial, iconX + iconSize / 2,
                        iconY + Math.max(1, (iconSize - 8) / 2), rarity);
            }
            int textX = iconX + iconSize + Math.max(4, (int) Math.round(5 * zoom));
            int textWidth = Math.max(10, bounds.right() - textX - 4);
            graphics.drawString(font, font.plainSubstrByWidth(talent.name(), textWidth), textX,
                    bounds.y() + Math.max(5, (int) Math.round(8 * zoom)),
                    talent.available() ? EditorTheme.TEXT : DISABLED_COLOR, false);
            if (zoom >= 0.62D) {
                graphics.drawString(font, font.plainSubstrByWidth(talent.id().toString(), textWidth), textX,
                        bounds.y() + Math.max(17, (int) Math.round(25 * zoom)), EditorTheme.MUTED, false);
            }
        }
    }

    private void renderSidebar(GuiGraphics graphics, int x) {
        int available = Math.max(80, width - x - 8);
        EditorBootstrap.TalentSummary talent = selectedSummary();
        int y = 179;
        if (talent == null) {
            graphics.drawWordWrap(font, Component.translatable("screen.talent_editor.tree.select_help"),
                    x, y, available, EditorTheme.MUTED);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(talent.name(), available), x, y,
                    rarityColor(talent.rarity()), false);
            y += 12;
            graphics.drawString(font, font.plainSubstrByWidth(talent.id().toString(), available), x, y,
                    EditorTheme.MUTED, false);
            y += 14;
            graphics.drawString(font, Component.translatable("screen.talent_editor.tree.acquire_summary",
                    talent.acquireTalents().size(), talent.acquireOtherConditions(),
                    Component.translatable("screen.talent_editor.tree.mode." + talent.acquireMode())),
                    x, y, ACQUIRE_COLOR, false);
            y += 12;
            graphics.drawString(font, Component.translatable("screen.talent_editor.tree.synthesis_summary",
                    talent.synthesisTalents().size(), talent.synthesisOtherCosts()),
                    x, y, talent.synthesisEnabled() ? SYNTHESIS_COLOR : DISABLED_COLOR, false);
            if (!talent.acquireRelationsEditable()) {
                y += 14;
                graphics.drawWordWrap(font, Component.translatable(
                        "screen.talent_editor.tree.advanced_acquire"), x, y, available, 0xFFFFAA55);
            }
        }
        if (selectedEdge != null) {
            int edgeY = Math.min(height - 62, y + 26);
            graphics.drawString(font, Component.translatable(
                    "screen.talent_editor.tree.selected_relation",
                    Component.translatable(selectedEdge.type().translationKey())),
                    x, edgeY, selectedEdge.type().color(), false);
            graphics.drawString(font, font.plainSubstrByWidth(
                    selectedEdge.source() + " → " + selectedEdge.target(), available),
                    x, edgeY + 12, EditorTheme.MUTED, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (!insideCanvas(mouseX, mouseY)) return false;
        EditorBootstrap.TalentSummary node = nodeAt(mouseX, mouseY);
        if (node != null && button == 0) {
            long now = Util.getMillis();
            boolean reopen = node.id().equals(selectedTalent) && now - lastNodeClick <= 380L;
            selectedTalent = node.id();
            selectedEdge = null;
            lastNodeClick = now;
            updateButtons();
            if (reopen) editSelected(TalentTreeEditRequest.Page.TALENT);
            return true;
        }
        Edge edge = edgeAt(mouseX, mouseY);
        if (edge != null && (button == 0 || button == 1)) {
            long now = Util.getMillis();
            boolean reopen = edge.equals(selectedEdge) && now - lastEdgeClick <= 500L;
            selectedEdge = edge;
            selectedTalent = edge.target();
            lastEdgeClick = now;
            updateButtons();
            if (button == 1 || reopen) editSelectedEdge();
            return true;
        }
        if (button == 0 || button == 2) {
            selectedEdge = null;
            if (button == 0) selectedTalent = null;
            panning = true;
            panButton = button;
            updateButtons();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning && button == panButton) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == panButton) {
            panning = false;
            panButton = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!insideCanvas(mouseX, mouseY)) return super.mouseScrolled(mouseX, mouseY, delta);
        double factor = delta > 0 ? 1.12D : 1.0D / 1.12D;
        setZoom(factor, mouseX, mouseY);
        return true;
    }

    private void editSelected(TalentTreeEditRequest.Page page) {
        if (selectedTalent == null) return;
        beginRequest(TalentTreeEditRequest.open(selectedTalent, page));
    }

    private void addRelation(RelationType type) {
        EditorBootstrap.TalentSummary target = selectedSummary();
        if (target == null || minecraft == null) return;
        if (type == RelationType.ACQUISITION && !target.acquireRelationsEditable()) {
            status = Component.translatable("screen.talent_editor.tree.advanced_acquire").getString();
            return;
        }
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.TALENT, bootstrap,
                source -> addRelation(type, source)
        ));
    }

    private void addRelation(RelationType type, ResourceLocation source) {
        if (selectedTalent == null) return;
        if (selectedTalent.equals(source)) {
            status = Component.translatable("screen.talent_editor.tree.self_relation").getString();
            return;
        }
        beginRequest(TalentTreeEditRequest.mutate(
                selectedTalent, type.page(), TalentTreeEditRequest.Mutation.ADD, source
        ));
    }

    private void editSelectedEdge() {
        if (selectedEdge == null) return;
        beginRequest(TalentTreeEditRequest.open(selectedEdge.target(), selectedEdge.type().page()));
    }

    private void removeSelectedEdge() {
        if (selectedEdge == null || !selectedEdge.editable()) return;
        beginRequest(TalentTreeEditRequest.mutate(
                selectedEdge.target(), selectedEdge.type().page(),
                TalentTreeEditRequest.Mutation.REMOVE, selectedEdge.source()
        ));
    }

    private void beginRequest(TalentTreeEditRequest request) {
        status = Component.translatable("screen.talent_editor.loading_talent").getString();
        ClientEditorState.requestTalentTreeEdit(request);
        EditorNetwork.requestTalent(request.target());
    }

    private void locateSearch() {
        String query = searchField == null ? searchValue : searchField.getValue();
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return;
        EditorBootstrap.TalentSummary found = talents.values().stream()
                .filter(talent -> (talent.id() + " " + talent.name()).toLowerCase(Locale.ROOT)
                        .contains(normalized)).findFirst().orElse(null);
        if (found == null) {
            status = Component.translatable("screen.talent_editor.tree.not_found").getString();
            return;
        }
        selectedTalent = found.id();
        selectedEdge = null;
        status = "";
        centerOn(found.id());
        updateButtons();
    }

    private void cycleFilter() {
        relationFilter = relationFilter.next();
        selectedEdge = null;
        relationFilterButton.setMessage(filterLabel());
        updateButtons();
    }

    private Component filterLabel() {
        return Component.translatable("screen.talent_editor.tree.filter." + relationFilter.serializedName());
    }

    private Component edgeVisibilityLabel() {
        return Component.translatable("screen.talent_editor.tree.edges."
                + (showAllEdges ? "all" : "focused"));
    }

    private void resetView() {
        int availableWidth = Math.max(80, canvasRight() - canvasLeft() - 24);
        int availableHeight = Math.max(60, canvasBottom() - canvasTop() - 24);
        double fit = Math.min(
                availableWidth / (double) Math.max(1, layout.width()),
                availableHeight / (double) Math.max(1, layout.height())
        );
        // A large graph must remain readable; fitting hundreds of talents to one screen
        // makes both nodes and their connections indistinguishable.
        zoom = fit >= 0.75D ? Math.min(1.0D, fit) : 1.0D;
        panX = fit >= 0.75D ? (canvasRight() - canvasLeft() - layout.width() * zoom) / 2.0D : 12.0D;
        panY = fit >= 0.75D ? (canvasBottom() - canvasTop() - layout.height() * zoom) / 2.0D : 12.0D;
        viewInitialized = true;
    }

    private void zoomAtCenter(double factor) {
        setZoom(factor, (canvasLeft() + canvasRight()) / 2.0D,
                (canvasTop() + canvasBottom()) / 2.0D);
    }

    private void setZoom(double factor, double anchorX, double anchorY) {
        double old = zoom;
        double next = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, old * factor));
        if (Math.abs(next - old) < 1.0E-8D) return;
        double worldX = (anchorX - canvasLeft() - panX) / old;
        double worldY = (anchorY - canvasTop() - panY) / old;
        zoom = next;
        panX = anchorX - canvasLeft() - worldX * next;
        panY = anchorY - canvasTop() - worldY * next;
    }

    private void centerOn(ResourceLocation id) {
        TalentTreeLayout.Position position = layout.positions().get(id);
        if (position == null) return;
        double centerX = position.x() + TalentTreeLayout.NODE_WIDTH / 2.0D;
        double centerY = position.y() + TalentTreeLayout.NODE_HEIGHT / 2.0D;
        panX = (canvasRight() - canvasLeft()) / 2.0D - centerX * zoom;
        panY = (canvasBottom() - canvasTop()) / 2.0D - centerY * zoom;
    }

    private void updateButtons() {
        EditorBootstrap.TalentSummary talent = selectedSummary();
        boolean selected = talent != null;
        if (editTalentButton != null) editTalentButton.active = selected;
        if (editAcquireButton != null) editAcquireButton.active = selected;
        if (editSynthesisButton != null) editSynthesisButton.active = selected;
        if (addAcquireButton != null) addAcquireButton.active = selected && talent.acquireRelationsEditable();
        if (addSynthesisButton != null) addSynthesisButton.active = selected;
        if (editEdgeButton != null) editEdgeButton.active = selectedEdge != null;
        if (removeEdgeButton != null) removeEdgeButton.active = selectedEdge != null && selectedEdge.editable();
    }

    private EditorBootstrap.TalentSummary selectedSummary() {
        return selectedTalent == null ? null : talents.get(selectedTalent);
    }

    private List<Edge> visibleEdges(ResourceLocation focus) {
        return edges.stream().filter(edge -> relationFilter.includes(edge.type()))
                .filter(edge -> showAllEdges || relatedTo(edge, focus)).toList();
    }

    private static boolean relatedTo(Edge edge, ResourceLocation focus) {
        return focus != null && (edge.source().equals(focus) || edge.target().equals(focus));
    }

    private EditorBootstrap.TalentSummary nodeAt(double mouseX, double mouseY) {
        ArrayList<EditorBootstrap.TalentSummary> reversed = new ArrayList<>(talents.values());
        for (int index = reversed.size() - 1; index >= 0; index--) {
            NodeBounds bounds = nodeBounds(reversed.get(index).id());
            if (bounds != null && bounds.contains(mouseX, mouseY)) return reversed.get(index);
        }
        return null;
    }

    private Edge edgeAt(double mouseX, double mouseY) {
        List<Edge> visible = visibleEdges(selectedTalent);
        Edge closest = null;
        double bestDistance = 6.0D;
        for (int index = visible.size() - 1; index >= 0; index--) {
            Edge edge = visible.get(index);
            EdgeGeometry geometry = geometry(edge);
            if (geometry == null) continue;
            double distance = geometry.distance(mouseX, mouseY);
            if (distance < bestDistance) {
                bestDistance = distance;
                closest = edge;
            }
        }
        return closest;
    }

    private NodeBounds nodeBounds(ResourceLocation id) {
        TalentTreeLayout.Position position = layout.positions().get(id);
        if (position == null) return null;
        int x = canvasLeft() + (int) Math.round(panX + position.x() * zoom);
        int y = canvasTop() + (int) Math.round(panY + position.y() * zoom);
        int nodeWidth = Math.max(48, (int) Math.round(TalentTreeLayout.NODE_WIDTH * zoom));
        int nodeHeight = Math.max(24, (int) Math.round(TalentTreeLayout.NODE_HEIGHT * zoom));
        return new NodeBounds(x, y, x + nodeWidth, y + nodeHeight);
    }

    private EdgeGeometry geometry(Edge edge) {
        NodeBounds source = nodeBounds(edge.source());
        NodeBounds target = nodeBounds(edge.target());
        if (source == null || target == null) return null;
        int offset = edge.type() == RelationType.ACQUISITION ? -3 : 3;
        int sourceX = source.right();
        int sourceY = source.y() + source.height() / 2 + offset;
        int targetX = target.x();
        int targetY = target.y() + target.height() / 2 + offset;
        int middleX = targetX > sourceX
                ? (sourceX + targetX) / 2 : Math.max(sourceX, target.right()) + 18;
        return new EdgeGeometry(sourceX, sourceY, middleX, targetX, targetY);
    }

    private int rarityColor(ResourceLocation rarity) {
        return bootstrap.rarities().stream().filter(value -> value.id().equals(rarity)).findFirst()
                .map(value -> 0xFF000000 | (value.color() & 0xFFFFFF)).orElse(0xFFB8C4D8);
    }

    private static List<Edge> buildEdges(List<EditorBootstrap.TalentSummary> talents) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        talents.forEach(talent -> ids.add(talent.id()));
        LinkedHashSet<Edge> edges = new LinkedHashSet<>();
        for (EditorBootstrap.TalentSummary target : talents) {
            target.acquireTalents().stream().filter(ids::contains).forEach(source -> edges.add(new Edge(
                    source, target.id(), RelationType.ACQUISITION,
                    target.acquireRelationsEditable(), true
            )));
            target.synthesisTalents().stream().filter(ids::contains).forEach(source -> edges.add(new Edge(
                    source, target.id(), RelationType.SYNTHESIS, true, target.synthesisEnabled()
            )));
        }
        return edges.stream().sorted(Comparator
                .comparing((Edge edge) -> edge.source().toString())
                .thenComparing(edge -> edge.target().toString())
                .thenComparing(edge -> edge.type().ordinal())).toList();
    }

    private static void drawSegment(
            GuiGraphics graphics, int x1, int y1, int x2, int y2,
            int color, int thickness, boolean dashed
    ) {
        if (!dashed) {
            graphics.fill(Math.min(x1, x2), Math.min(y1, y2),
                    Math.max(x1, x2) + thickness, Math.max(y1, y2) + thickness, color);
            return;
        }
        if (y1 == y2) {
            int start = Math.min(x1, x2);
            int end = Math.max(x1, x2);
            for (int x = start; x <= end; x += 8) {
                graphics.fill(x, y1, Math.min(end + 1, x + 5), y1 + thickness, color);
            }
        } else {
            int start = Math.min(y1, y2);
            int end = Math.max(y1, y2);
            for (int y = start; y <= end; y += 8) {
                graphics.fill(x1, y, x1 + thickness, Math.min(end + 1, y + 5), color);
            }
        }
    }

    private static void renderIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y, int size) {
        float scale = size / 16.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0D);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(icon, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        graphics.pose().popPose();
    }

    private boolean insideCanvas(double x, double y) {
        return x >= canvasLeft() && x < canvasRight() && y >= canvasTop() && y < canvasBottom();
    }

    private int sidebarWidth() {
        return Math.min(250, Math.max(174, width / 4));
    }

    private int canvasLeft() { return 10; }
    private int canvasTop() { return 54; }
    private int canvasRight() { return Math.max(canvasLeft() + 80, width - sidebarWidth() - 14); }
    private int canvasBottom() { return Math.max(canvasTop() + 40, height - 34); }

    private void returnHome() {
        if (minecraft != null) minecraft.setScreen(new EditorHomeScreen(bootstrap));
    }

    @Override
    public void onClose() {
        returnHome();
    }

    private enum RelationType {
        ACQUISITION("acquisition", ACQUIRE_COLOR, TalentTreeEditRequest.Page.ACQUISITION),
        SYNTHESIS("synthesis", SYNTHESIS_COLOR, TalentTreeEditRequest.Page.SYNTHESIS);

        private final String serializedName;
        private final int color;
        private final TalentTreeEditRequest.Page page;

        RelationType(String serializedName, int color, TalentTreeEditRequest.Page page) {
            this.serializedName = serializedName;
            this.color = color;
            this.page = page;
        }

        int color() { return color; }
        TalentTreeEditRequest.Page page() { return page; }
        String translationKey() { return "screen.talent_editor.tree.relation." + serializedName; }
    }

    private enum RelationFilter {
        ALL("all"),
        ACQUISITION("acquisition"),
        SYNTHESIS("synthesis");

        private final String serializedName;

        RelationFilter(String serializedName) { this.serializedName = serializedName; }
        String serializedName() { return serializedName; }
        RelationFilter next() { return values()[(ordinal() + 1) % values().length]; }
        boolean includes(RelationType type) {
            return this == ALL || this == ACQUISITION && type == RelationType.ACQUISITION
                    || this == SYNTHESIS && type == RelationType.SYNTHESIS;
        }
    }

    private record Edge(
            ResourceLocation source,
            ResourceLocation target,
            RelationType type,
            boolean editable,
            boolean active
    ) {
    }

    private record NodeBounds(int x, int y, int right, int bottom) {
        int width() { return right - x; }
        int height() { return bottom - y; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right && mouseY >= y && mouseY < bottom;
        }
    }

    private record EdgeGeometry(int sourceX, int sourceY, int middleX, int targetX, int targetY) {
        double distance(double x, double y) {
            return Math.min(
                    distanceToSegment(x, y, sourceX, sourceY, middleX, sourceY),
                    Math.min(distanceToSegment(x, y, middleX, sourceY, middleX, targetY),
                            distanceToSegment(x, y, middleX, targetY, targetX, targetY))
            );
        }

        private static double distanceToSegment(
                double px, double py, double x1, double y1, double x2, double y2
        ) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            if (dx == 0.0D && dy == 0.0D) return Math.hypot(px - x1, py - y1);
            double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
            t = Math.max(0.0D, Math.min(1.0D, t));
            return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
        }
    }
}
