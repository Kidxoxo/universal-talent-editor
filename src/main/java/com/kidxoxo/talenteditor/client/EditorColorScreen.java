package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

final class EditorColorScreen extends EditorScreen {
    private final Screen parent;private final Consumer<String> apply;private String value;private EditBox hex;private boolean syncing;
    private final List<Channel> sliders=new ArrayList<>();
    private static final int[] PALETTE={0xFFFFFFFF,0xFF000000,0xFF17293C,0xFF7DCAD6,0xFF98D6B0,0xFFE8C383,0xFFF09B9B,0xFF897DD6,0x80FFFFFF,0x80000000,0x00000000,0xFFDCAB72};
    EditorColorScreen(Screen parent,String value,Consumer<String> apply){super(EditorDefinitionScreen.tr("color"));this.parent=parent;this.value=value;this.apply=apply;}
    static Integer parse(String value){try{return value.matches("#[0-9a-fA-F]{8}")?(int)Long.parseLong(value.substring(1),16):null;}catch(RuntimeException e){return null;}}
    @Override protected void init(){
        int w=Math.min(420,width-32),x=(width-w)/2;
        hex=new EditBox(font,x,32,w-78,20,title);hex.setMaxLength(9);hex.setValue(value);hex.setResponder(v->{value=v;if(!syncing)sync();});addRenderableWidget(hex);
        sliders.clear();for(int i=0;i<4;i++){var s=new Channel(x,61+i*25,w,i);sliders.add(s);addRenderableWidget(s);}sync();
        int cell=(w-22)/12;for(int i=0;i<PALETTE.length;i++){int color=PALETTE[i];addRenderableWidget(EditorWidgets.button(Component.empty(),b->{hex.setValue(String.format(Locale.ROOT,"#%08X",color));}).bounds(x+i*(cell+2),166,cell,20).build());}
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"),b->onClose()).bounds(x,height-28,(w-6)/2,20).build());
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("apply_color"), EditorTheme.Tone.PRIMARY,b ->{if(parse(value)!=null){apply.accept(value);minecraft.setScreen(parent);}}).bounds(x+(w+6)/2,height-28,(w-6)/2,20).build());
    }
    private void sync(){Integer color=parse(value);if(color!=null)for(var s:sliders)s.setChannel((color>>>s.shift())&255);}
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);super.render(g,mx,my,tick);EditorTheme.title(g,font,title,width);
        int w=Math.min(420,width-32),x=(width-w)/2;Integer color=parse(value);
        for(int i=0;i<8;i++)for(int j=0;j<3;j++)g.fill(x+w-70+i*8,32+j*7,x+w-62+i*8,39+j*7,(i+j)%2==0?0xFFBBBBBB:0xFF666666);
        if(color!=null)g.fill(x+w-70,32,x+w-6,53,color);
        int cell=(w-22)/12;for(int i=0;i<PALETTE.length;i++)g.fill(x+i*(cell+2)+4,171,x+i*(cell+2)+cell-4,181,PALETTE[i]);
        g.drawString(font,font.plainSubstrByWidth(EditorDefinitionScreen.tr(color==null?"color_invalid":"alpha_hint").getString(),w),x,196,color==null?EditorTheme.ERROR:EditorTheme.MUTED,false);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    private final class Channel extends EditorSlider {
        private final int channel;
        Channel(int x,int y,int w,int channel){super(x,y,w,20,Component.empty(),0);this.channel=channel;updateMessage();}
        int shift(){return channel==0?24:(3-channel)*8;}
        void setChannel(int v){value=v/255.0;updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal((channel==0?"A":channel==1?"R":channel==2?"G":"B")+"  "+Math.round(value*255)));}
        @Override protected void applyValue(){Integer current=parse(EditorColorScreen.this.value);if(current==null)current=0xFFFFFFFF;int color=(current&~(255<<shift()))|((int)Math.round(value*255)<<shift());syncing=true;hex.setValue(String.format(Locale.ROOT,"#%08X",color));syncing=false;}
    }
}
