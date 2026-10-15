package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.UiDocument;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

final class UiLayoutPreviewScreen extends Screen {
    private final Screen parent;
    private final UiScene scene;
    UiLayoutPreviewScreen(Screen parent,UiDocument document,boolean draw) {
        this(parent,document,draw,java.util.List.of());
    }
    UiLayoutPreviewScreen(Screen parent,UiDocument document,boolean draw,java.util.List<LayoutDraft.PageDraft> pages) {
        super(UiCanvasEditorScreen.tr("preview"));this.parent=parent;scene=new UiScene(document,new UiPreviewContext(draw,pages));
    }
    @Override protected void init(){scene.install(width,height,button->addRenderableWidget(button));}
    @Override public void render(GuiGraphics g,int x,int y,float tick){
        scene.render(g,x,y,width,height);g.drawString(font,UiCanvasEditorScreen.tr("preview_hint"),4,2,0xFFFFAA55,false);
    }
    @Override public boolean mouseClicked(double x,double y,int b){return scene.click(x,y,b)||super.mouseClicked(x,y,b);}
    @Override public boolean mouseScrolled(double x,double y,double d){return scene.scroll(x,y,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
