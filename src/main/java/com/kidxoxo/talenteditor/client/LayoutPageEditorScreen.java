package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LayoutPageEditorScreen extends EditorScreen {
    private static final int MAX_PAGES = 64;
    private static final int MAX_TOTAL_SLOTS = 4_096;
    private final LayoutEditScreen parent;
    private final List<LayoutDraft.SlotDraft> defaultSlots;
    private final ArrayList<LayoutDraft.PageDraft> pages;
    private final List<LayoutDraft.PageDraft> originalPages;
    private boolean loadingFields;
    private int selected = -1;
    private final EditorOptionPopup options=new EditorOptionPopup();
    private Button pagePicker,contents;
    private EditBox idField;
    private EditBox backgroundField;
    private Button backgroundPickerButton;
    private String status = "";

    public LayoutPageEditorScreen(
            LayoutEditScreen parent,
            List<LayoutDraft.PageDraft> pages,
            List<LayoutDraft.SlotDraft> defaultSlots
    ) {
        super(Component.translatable("screen.talent_editor.layout.page_editor.title"));
        this.parent = parent;
        this.pages = new ArrayList<>(pages);
        this.originalPages = List.copyOf(pages);
        this.defaultSlots = List.copyOf(defaultSlots);
        if (!pages.isEmpty()) selected = 0;
    }

    @Override
    protected void init() {
        int x=16,w=width-32,gap=6;
        pagePicker=addRenderableWidget(EditorWidgets.button(Component.empty(),b->options.open(title,pages.stream().map(page->Component.literal(page.id())).toList(),Math.max(0,selected),i->{storeSelected();selected=i;loadSelected();},height)).bounds(x,30,w,20).build());
        int q=(w-3*gap)/4;
        String[] keys={"add","up","down","remove"};
        Runnable[] actions={this::addPage,()->movePage(-1),()->movePage(1),this::removePage};
        for(int i=0;i<4;i++){int at=i;addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.layout.page."+keys[i]),b->actions[at].run()).bounds(x+i*(q+gap),57,q,20).build());}
        idField=new EditBox(font,x,100,w,20,Component.translatable("screen.talent_editor.layout.page.id"));idField.setMaxLength(64);addRenderableWidget(idField);
        backgroundField=new EditBox(font,x,136,w-54,20,Component.translatable("screen.talent_editor.layout.page.background"));backgroundField.setMaxLength(512);addRenderableWidget(backgroundField);
        backgroundPickerButton=addRenderableWidget(EditorWidgets.button(Component.literal("…"),b->openBackgroundPicker()).bounds(x+w-48,136,48,20).build());
        contents=addRenderableWidget(EditorWidgets.button(UiCanvasEditorScreen.tr("bindings"),b->openSlots()).bounds(x,166,w,20).build());
        int half=(w-gap)/2;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"),b->returnWithoutApplying()).bounds(x,height-28,half,20).build());
        addRenderableWidget(EditorWidgets.button(UiCanvasEditorScreen.tr("apply_layout"),b->apply()).bounds(x+half+gap,height-28,w-half-gap,20).build());
        loadSelected();
        idField.setResponder(value->{if(!loadingFields){storeSelected();status="";}});
        backgroundField.setResponder(value->{if(!loadingFields){storeSelected();status="";}});
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);super.render(g,mx,my,tick);
        EditorTheme.title(g,font,title,width);
        g.drawString(font,Component.translatable("screen.talent_editor.layout.page.id"),16,88,EditorTheme.MUTED,false);
        g.drawString(font,Component.translatable("screen.talent_editor.layout.page.background"),16,124,EditorTheme.MUTED,false);
        if(height>=290)g.drawWordWrap(font,UiCanvasEditorScreen.tr("pages_hint"),16,195,width-32,EditorTheme.MUTED);
        if(!status.isBlank())g.drawString(font,font.plainSubstrByWidth(status,width-32),16,height-42,0xFFAA66,false);
        options.render(g,font,width,height);
    }
    @Override public boolean mouseClicked(double x,double y,int b){return options.mouseClicked(x,y,b,width,height)||super.mouseClicked(x,y,b);}
    @Override public boolean mouseScrolled(double x,double y,double d){return options.mouseScrolled(d,height)||super.mouseScrolled(x,y,d);}
    @Override public boolean keyPressed(int k,int s,int m){return options.keyPressed(k,height)||super.keyPressed(k,s,m);}
    @Override public boolean charTyped(char c,int m){return options.isOpen()||super.charTyped(c,m);}

    private void addPage() {
        storeSelected();
        if (pages.size() >= MAX_PAGES) {
            status = Component.translatable("screen.talent_editor.layout.page.limit").getString();
            return;
        }
        Set<String> ids = new HashSet<>();
        pages.forEach(page -> ids.add(page.id()));
        int suffix = 1;
        while (ids.contains("page_" + suffix)) suffix++;
        pages.add(new LayoutDraft.PageDraft("page_" + suffix, "", defaultSlots));
        selected = pages.size() - 1;
        loadSelected();
        status = "";
    }

    private void removePage() {
        if (selected < 0 || selected >= pages.size()) return;
        pages.remove(selected);
        selected = Math.min(selected, pages.size() - 1);
        loadSelected();
        status = "";
    }

    private void movePage(int delta) {
        storeSelected();
        int target = selected + delta;
        if (selected < 0 || target < 0 || target >= pages.size()) return;
        Collections.swap(pages, selected, target);
        selected = target;
        loadSelected();
        status = "";
    }

    private void openSlots() {
        if (selected < 0 || selected >= pages.size() || minecraft == null) return;
        storeSelected();
        LayoutDraft.PageDraft page = pages.get(selected);
        List<LayoutDraft.SlotDraft> initial = page.slots().isEmpty() ? defaultSlots : page.slots();
        minecraft.setScreen(new LayoutBindingScreen(
                this, parent.bootstrap(), initial, defaultSlots.size(), this::updateSelectedSlots
        ));
    }

    private void openBackgroundPicker() {
        if (selected < 0 || selected >= pages.size() || minecraft == null) return;
        storeSelected();
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.TEXTURE, parent.bootstrap(), this::chooseBackground
        ));
    }

    private void chooseBackground(ResourceLocation id) {
        if (selected < 0 || selected >= pages.size()) return;
        LayoutDraft.PageDraft page = pages.get(selected);
        pages.set(selected, new LayoutDraft.PageDraft(page.id(), id.toString(), page.slots()));
    }

    private void updateSelectedSlots(List<LayoutDraft.SlotDraft> slots) {
        if (selected < 0 || selected >= pages.size()) return;
        LayoutDraft.PageDraft page = pages.get(selected);
        pages.set(selected, new LayoutDraft.PageDraft(page.id(), page.background(), slots));
    }

    private void apply() {
        storeSelected();
        HashSet<String> ids = new HashSet<>();
        HashSet<String> fixedTalents = new HashSet<>();
        int totalSlots = 0;
        for (LayoutDraft.PageDraft page : pages) {
            if (!page.id().matches("[a-z0-9_.-]{1,64}")) {
                status = Component.translatable("screen.talent_editor.layout.page.invalid_id", page.id()).getString();
                return;
            }
            if (!ids.add(page.id())) {
                status = Component.translatable("screen.talent_editor.layout.page.duplicate_id", page.id()).getString();
                return;
            }
            if (!page.background().isBlank()) {
                ResourceLocation parsed = ResourceLocation.tryParse(page.background());
                if (parsed == null || !page.background().contains(":")) {
                    status = Component.translatable("screen.talent_editor.layout.page.invalid_background",
                            page.id()).getString();
                    return;
                }
            }
            if (page.slots().isEmpty()) {
                status = Component.translatable("screen.talent_editor.layout.page.no_slots", page.id()).getString();
                return;
            }
            totalSlots += page.slots().size();
            for (var slot : page.slots()) {
                if (!slot.talent().isBlank() && !fixedTalents.add(slot.talent())) {
                    status = Component.translatable("screen.talent_editor.layout.slot.duplicate_binding",
                            slot.talent()).getString();
                    return;
                }
            }
        }
        if (totalSlots > MAX_TOTAL_SLOTS) {
            status = Component.translatable("screen.talent_editor.layout.page.slot_limit").getString();
            return;
        }
        parent.updatePages(pages);
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void storeSelected() {
        if (selected < 0 || selected >= pages.size() || idField == null || backgroundField == null) return;
        LayoutDraft.PageDraft page = pages.get(selected);
        pages.set(selected, new LayoutDraft.PageDraft(
                idField.getValue().strip(), backgroundField.getValue().strip(), page.slots()
        ));
    }

    private void loadSelected() {
        if (idField == null || backgroundField == null) return;
        loadingFields=true;
        try {
        pagePicker.setMessage(pages.isEmpty()?UiCanvasEditorScreen.tr("no_pages"):Component.literal((selected+1)+" / "+pages.size()+" · "+pages.get(selected).id()));
        pagePicker.active=!pages.isEmpty();contents.active=selected>=0&&selected<pages.size();
        if (selected < 0 || selected >= pages.size()) {
            idField.setValue("");
            backgroundField.setValue("");
            idField.setEditable(false);
            backgroundField.setEditable(false);
            backgroundPickerButton.active = false;
        } else {
            LayoutDraft.PageDraft page = pages.get(selected);
            idField.setEditable(true);
            backgroundField.setEditable(true);
            backgroundPickerButton.active = true;
            idField.setValue(page.id());
            backgroundField.setValue(page.background());
        }
        } finally { loadingFields=false; }
    }

    private void returnWithoutApplying() {
        if (minecraft == null) return;
        storeSelected();
        if(pages.equals(originalPages))minecraft.setScreen(parent);
        else minecraft.setScreen(new EditorConfirmScreen(
                yes->minecraft.setScreen(yes?parent:this),UiCanvasEditorScreen.tr("discard"),UiCanvasEditorScreen.tr("discard_confirm")));
    }

    @Override
    public void onClose() { returnWithoutApplying(); }

}
