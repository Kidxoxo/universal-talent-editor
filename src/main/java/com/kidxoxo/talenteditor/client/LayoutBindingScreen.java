package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import java.util.*;
import java.util.function.Consumer;

/** Page content only. Module geometry has exactly one editing surface: the UI canvas. */
final class LayoutBindingScreen extends EditorScreen {
    private final Screen parent;
    private final EditorBootstrap bootstrap;
    private final ArrayList<LayoutDraft.SlotDraft> slots;
    private final int capacity;
    private final List<LayoutDraft.SlotDraft> original;
    private final Consumer<List<LayoutDraft.SlotDraft>> save;
    private int selected,scroll;
    private String status="";
    private Button bind,clear,add,remove;
    LayoutBindingScreen(Screen parent,EditorBootstrap bootstrap,List<LayoutDraft.SlotDraft> slots,int capacity,Consumer<List<LayoutDraft.SlotDraft>> save) {
        super(UiCanvasEditorScreen.tr("bindings"));this.parent=parent;this.bootstrap=bootstrap;
        this.slots=new ArrayList<>(slots);this.original=List.copyOf(slots);this.capacity=capacity;this.save=save;
    }
    @Override protected void init() {
        int w=(width-38)/4;
        bind=button(10,32,w,"bind_talent",this::choose);
        clear=button(16+w,32,w,"auto_slot",()->{slots.set(selected,slots.get(selected).withTalent(""));status="";});
        add=button(22+2*w,32,w,"append_slot",()->{
            var next=LayoutContentSlots.next(slots);
            if(next.isEmpty()){status=UiCanvasEditorScreen.tr("no_legacy_space").getString();return;}
            slots.add(next.orElseThrow());selected=slots.size()-1;scroll=Math.max(0,selected-rows()+1);status="";
        });
        remove=button(28+3*w,32,w,"trim_slot",()->{
            if(!slots.get(slots.size()-1).talent().isBlank()){status=UiCanvasEditorScreen.tr("clear_tail").getString();return;}
            slots.remove(slots.size()-1);selected=Math.min(selected,slots.size()-1);status="";
        });
        button(10,height-26,(width-26)/2,"cancel",this::onClose);
        button(16+(width-26)/2,height-26,(width-26)/2,"apply_page",()->{
            if(slots.size()>capacity){status=UiCanvasEditorScreen.tr("capacity_exceeded",capacity).getString();return;}
            save.accept(List.copyOf(slots));minecraft.setScreen(parent);
        });
    }
    private Button button(int x,int y,int w,String key,Runnable action){return addRenderableWidget(EditorWidgets.button(UiCanvasEditorScreen.tr(key),b->action.run()).bounds(x,y,w,20).build());}
    private int rows(){return Math.max(1,(height-116)/24);}
    private void choose(){minecraft.setScreen(new ResourceReferencePickerScreen(this,ResourceReferencePickerScreen.Source.TALENT,bootstrap,id->{slots.set(selected,slots.get(selected).withTalent(id.toString()));status="";}));}
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);
        bind.active=clear.active=selected>=0&&selected<slots.size();add.active=slots.size()<capacity;remove.active=slots.size()>1;
        scroll=Math.max(0,Math.min(scroll,Math.max(0,slots.size()-rows())));
        for(int row=0;row<rows()&&scroll+row<slots.size();row++){
            int i=scroll+row,y=63+row*24;var slot=slots.get(i);
            if(i==selected)g.fill(10,y-2,width-10,y+20,0xFF285078);
            String label=(i+1)+" · "+(slot.talent().isBlank()?UiCanvasEditorScreen.tr("auto_slot").getString():slot.talent());
            g.drawString(font,font.plainSubstrByWidth(label,width-34),16,y+4,EditorTheme.TEXT,false);
        }
        super.render(g,mx,my,tick);EditorTheme.title(g,font,title,width);
        g.drawString(font,font.plainSubstrByWidth(UiCanvasEditorScreen.tr("binding_hint",slots.size(),capacity).getString(),width-20),10,height-51,EditorTheme.MUTED,false);
        if(!status.isBlank())g.drawString(font,font.plainSubstrByWidth(status,width-20),10,height-39,0xFFAA66,false);
    }
    @Override public boolean mouseClicked(double x,double y,int b){
        if(b==0&&x>=10&&x<width-10&&y>=61&&y<63+rows()*24){int i=scroll+(int)((y-61)/24);if(i<slots.size()){selected=i;return true;}}
        return super.mouseClicked(x,y,b);
    }
    @Override public boolean mouseScrolled(double x,double y,double d){scroll=Math.max(0,Math.min(Math.max(0,slots.size()-rows()),scroll-(int)Math.signum(d)*3));return true;}
    @Override public void onClose(){if(slots.equals(original))minecraft.setScreen(parent);else minecraft.setScreen(new EditorLeaveScreen(this,()->minecraft.setScreen(parent),()->{if(slots.size()<=capacity){save.accept(List.copyOf(slots));minecraft.setScreen(parent);}}));}
}
