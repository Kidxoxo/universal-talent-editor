package com.kidxoxo.talenteditor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Keeps vanilla slider keyboard/drag handling while matching the rest of the editor. */
abstract class EditorSlider extends AbstractSliderButton {
    EditorSlider(int x,int y,int width,int height,Component label,double value){super(x,y,width,height,label,value);}
    @Override public void renderWidget(GuiGraphics g,int mx,int my,float tick){
        g.fill(getX(),getY(),getX()+width,getY()+height,EditorTheme.SURFACE);
        int position=getX()+4+(int)Math.round(value*(width-8));
        g.fill(getX()+4,getY()+height-5,getX()+width-4,getY()+height-3,EditorTheme.BORDER);
        g.fill(getX()+4,getY()+height-5,position,getY()+height-3,EditorTheme.ACCENT);
        g.fill(position-2,getY()+height-7,position+2,getY()+height-1,EditorTheme.ACCENT);
        EditorTheme.outline(g,getX(),getY(),width,height,isHoveredOrFocused()?EditorTheme.ACCENT:EditorTheme.BORDER);
        var font=Minecraft.getInstance().font;
        g.drawString(font,getMessage(),getX()+(width-font.width(getMessage()))/2,getY()+3,active?EditorTheme.TEXT:EditorTheme.MUTED,false);
    }
}
