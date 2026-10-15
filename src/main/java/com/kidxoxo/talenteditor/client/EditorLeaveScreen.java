package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class EditorLeaveScreen extends EditorScreen {
    private final Screen parent;
    private final Runnable discard, apply;
    private final java.util.Map<String,java.util.List<String>> inputs;
    EditorLeaveScreen(Screen parent, Runnable discard, Runnable apply) {
        super(EditorDefinitionScreen.tr("unsaved")); this.parent=parent; this.discard=discard; this.apply=apply;
        inputs = parent instanceof EditorScreen editor ? editor.captureInputs() : java.util.Map.of();
    }
    @Override protected void init() {
        int w=Math.min(360,width-32),x=(width-w)/2,y=height/2;
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("continue"),b->onClose()).bounds(x,y,w,20).build());
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("discard"), EditorTheme.Tone.DANGER,b ->discard.run()).bounds(x,y+25,w,20).build());
        if(apply!=null)addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr(parent instanceof EditorDefinitionScreen?"save_return":"apply_return"),EditorTheme.Tone.PRIMARY,b->{restoreParent();apply.run();}).bounds(x,y+50,w,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        renderBackground(g);super.render(g,mx,my,tick);EditorTheme.title(g,font,title,width);
        g.drawWordWrap(font,EditorDefinitionScreen.tr("unsaved_hint"),(width-Math.min(360,width-32))/2,height/2-40,Math.min(360,width-32),EditorTheme.MUTED);
    }
    private void restoreParent(){minecraft.setScreen(parent);if(parent instanceof EditorScreen editor)editor.restoreInputs(inputs);}
    @Override public void onClose(){restoreParent();}
}
