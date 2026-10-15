package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.ValidationResultPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

final class EditorValidationScreen extends EditorScreen {
    private final Screen parent;private final String detail;private List<FormattedCharSequence> lines=List.of();private int scroll;
    EditorValidationScreen(Screen parent,ValidationResultPacket result){
        super(EditorDefinitionScreen.tr("validation_details"));this.parent=parent;
        detail=result.issues().stream().map(i->i.severity()+" · "+i.code()+"\n"+i.source()+" · "+i.path()+"\n"+i.message()).reduce((a,b)->a+"\n\n"+b).orElse("");
    }
    @Override protected void init(){
        lines=font.split(Component.literal(detail),width-40);
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"),b->onClose()).bounds(16,height-28,100,20).build());
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("copy_details"),b->minecraft.keyboardHandler.setClipboard(detail)).bounds(width-156,height-28,140,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);EditorTheme.title(g,font,title,width);int rows=Math.max(1,(height-74)/12);scroll=Math.min(scroll,Math.max(0,lines.size()-rows));
        for(int i=0;i<rows&&scroll+i<lines.size();i++)g.drawString(font,lines.get(scroll+i),20,32+i*12,EditorTheme.TEXT,false);
        super.render(g,mx,my,tick);
    }
    @Override public boolean mouseScrolled(double x,double y,double d){scroll=Math.max(0,scroll-(int)Math.signum(d)*3);return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
