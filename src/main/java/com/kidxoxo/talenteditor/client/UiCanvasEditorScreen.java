package com.kidxoxo.talenteditor.client;

import com.google.gson.*;
import com.kidxoxo.talenteditor.compile.UiDocumentCodec;
import com.kidxoxo.talenteditor.domain.UiDocument;
import com.kidxoxo.talenteditor.domain.UiDocument.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;

final class UiCanvasEditorScreen extends EditorScreen {
    private final LayoutEditScreen parent;
    final boolean draw;
    private final Consumer<Optional<UiDocument>> save;
    private final UiDocument initial;
    private final UiEditHistory history;
    private final UiPreviewContext preview;
    private final EditorOptionPopup options=new EditorOptionPopup();
    private UiDocument dragging, dragBase;
    private Set<String> dragSelection=Set.of();
    private int selected=-1,grid=4;
    private boolean resizing,previewOnly;
    private double startX,startY;
    private Element startElement;
    private String status="";
    private Viewport viewport;
    private UiScene scene;
    private final Map<String,Button> controls=new HashMap<>();
    private int layerScroll;
    private double zoom=1,panX,panY;
    private boolean panning;
    private boolean layersExpanded=true;
    private final Set<String> selection=new LinkedHashSet<>();
    private JsonObject copiedStyle;
    private boolean showLayers(){return width>=600&&layersExpanded;}
    private Set<String> selectedIds(){
        if(selection.isEmpty()&&selected>=0&&selected<document().elements().size())selection.add(document().elements().get(selected).id());
        return Set.copyOf(selection);
    }
    private void select(int index,boolean multiple){
        selected=index;String id=document().elements().get(index).id();
        if(!multiple)selection.clear();
        if(multiple&&selection.contains(id))selection.remove(id);else selection.add(id);
        if(selection.isEmpty())selected=-1;
        else if(!selection.contains(id))selected=java.util.stream.IntStream.range(0,document().elements().size())
                .filter(i->selection.contains(document().elements().get(i).id())).findFirst().orElse(-1);
    }
    private String layerLabel(Element e){return (e.locked()?"[L] ":"")+(e.label().isBlank()?e.id():e.label());}
    private boolean protectedSelection(boolean deleting){return document().elements().stream().filter(e->selectedIds().contains(e.id())).anyMatch(e->e.locked()||e.type().equals("offer")||(deleting&&e.type().equals("slot")&&e.index()<parent.requiredPageSlots()));}

    private Rect canvasBounds(){int x=showLayers()?164:8;return new Rect(x,56,Math.max(1,width-x-8),Math.max(1,height-105));}
    private Viewport canvasViewport(){Rect r=canvasBounds();var fit=Viewport.fit(document(),r.x(),r.y(),r.width(),r.height());double scale=fit.scale()*zoom;return new Viewport(r.x()+r.width()/2.0-document().width()*scale/2+panX,r.y()+r.height()/2.0-document().height()*scale/2+panY,scale);}
    private int layerRows(){return Math.max(1,(height-160)/20);}
    private void fit(){finishDrag();zoom=1;panX=panY=0;}

    UiCanvasEditorScreen(LayoutEditScreen parent,boolean draw,UiDocument initial,Consumer<Optional<UiDocument>> save) {
        super(tr("editor"));this.parent=parent;this.draw=draw;this.initial=initial;this.save=save;
        history=new UiEditHistory(initial);preview=new UiPreviewContext(draw,parent.pages());
    }
    static Component tr(String key,Object... args) { return Component.translatable("screen.talent_editor.ui."+key,args); }
    UiDocument document() { return dragging==null?history.value():dragging; }
    LayoutEditScreen parent() { return parent; }
    void replace(UiDocument next) { finishDrag(); history.commit(next);selection.removeIf(id->next.elements().stream().noneMatch(e->e.id().equals(id)));selected=Math.min(selected,next.elements().size()-1);scene=null; }
    @Override protected void init() {
        finishDrag();
        controls.clear();
        int w=Math.max(32,(width-22)/6);
        tool(0,0,w,"add",this::add);tool(1,0,w,"layers",this::layers);tool(2,0,w,"properties",()->properties(false));
        tool(3,0,w,"canvas",()->properties(true));tool(4,0,w,"undo",()->restore(false));tool(5,0,w,"redo",()->restore(true));
        tool(0,1,w,"duplicate",this::duplicate);tool(1,1,w,"delete",this::delete);tool(2,1,w,"arrange",this::arrange);
        tool(3,1,w,"lock",this::toggleLock);tool(4,1,w,"snap",()->{grid=grid==1?4:grid==4?8:1;});
        tool(5,1,w,"preview",this::previewMenu);
        int bottom=height-25,bw=Math.max(42,(width-22)/4);
        button(7,bottom,bw,"cancel",this::onClose);
        button(12+bw,bottom,bw,"fit",this::fit);
        button(17+bw*2,bottom,bw,"more",this::more);
        button(22+bw*3,bottom,bw,"apply_layout",this::apply);
    }
    private void tool(int col,int row,int w,String key,Runnable action){button(7+col*(w+2),7+row*23,w,key,action);}
    private void button(int x,int y,int w,String key,Runnable action){controls.put(key,addRenderableWidget(EditorWidgets.button(tr(key),b->action.run()).bounds(x,y,w,20).build()));}
    private void more(){
        finishDrag();
        options.open(tr("recovery"),List.of(tr("reset"),tr("rebuild_base"),tr("legacy"),tr("edit_page_contents")),0,i->{
            if(i==0)minecraft.setScreen(new EditorConfirmScreen(yes->{if(yes)replace(initial);minecraft.setScreen(this);},tr("reset"),tr("reset_confirm")));
            else if(i==1)minecraft.setScreen(new EditorConfirmScreen(yes->{if(yes){replace(parent.baseUi(draw));fit();}minecraft.setScreen(this);},tr("rebuild_base"),tr("rebuild_confirm")));
            else if(i==2)minecraft.setScreen(new EditorConfirmScreen(yes->{if(yes){save.accept(Optional.empty());minecraft.setScreen(parent);}else minecraft.setScreen(this);},tr("legacy"),tr("legacy_confirm")));
            else {save.accept(Optional.of(document()));parent.editPageContents();}
        },height);
    }
    private void updateControls(){
        controls.get("undo").active=history.canUndo()&&!previewOnly;
        controls.get("redo").active=history.canRedo()&&!previewOnly;
        for(String key:List.of("properties","duplicate","delete","arrange","lock"))controls.get(key).active=selected>=0&&!previewOnly;
        controls.get("duplicate").active=selected>=0&&!previewOnly&&!protectedSelection(false);
        controls.get("delete").active=selected>=0&&!previewOnly&&!protectedSelection(true);
        controls.get("duplicate").setTooltip(net.minecraft.client.gui.components.Tooltip.create(tr("selection_limits")));
        controls.get("delete").setTooltip(net.minecraft.client.gui.components.Tooltip.create(tr("selection_limits")));
        controls.get("add").active=controls.get("canvas").active=!previewOnly;
        controls.get("snap").setMessage(tr("snap_value",grid));
        controls.get("layers").setMessage(tr(width>=600?(layersExpanded?"collapse_layers":"expand_layers"):"layers"));
        controls.get("preview").setMessage(tr(previewOnly?"edit_mode":"preview"));
    }
    private void apply(){finishDrag();save.accept(Optional.of(history.value()));minecraft.setScreen(parent);}
    private void restore(boolean redo){finishDrag();if(redo)history.redo();else history.undo();selection.removeIf(id->document().elements().stream().noneMatch(e->e.id().equals(id)));selected=selection.isEmpty()?-1:java.util.stream.IntStream.range(0,document().elements().size()).filter(i->selection.contains(document().elements().get(i).id())).findFirst().orElse(-1);scene=null;}
    private void properties(boolean canvas){finishDrag();if(canvas||selected>=0)minecraft.setScreen(new UiElementPropertiesScreen(this,canvas?-1:selected));}
    private void layers(){
        finishDrag();
        if(width>=600){layersExpanded=!layersExpanded;fit();return;}
        var list=document().elements();options.open(tr("layers"),list.stream().map(e->Component.literal(layerLabel(e)+" · ").append(tr("type."+e.type()))).toList(),
                Math.max(0,selected),index->{select(index,false);previewOnly=false;},height);
    }
    private void add(){
        finishDrag();
        List<String> types=draw?List.of("panel","image","text","button"):List.of("panel","image","text","button","slot","details");
        options.open(tr("add"),types.stream().map(t->tr("type."+t)).toList(),0,i->{
            String type=types.get(i);JsonObject e=UiTemplates.element(uniqueId(type),type,40,60,100,type.equals("details")?100:28);
            if(type.equals("text"))e.addProperty("text",tr("new_text").getString());
            if(type.equals("slot"))e.addProperty("index",document().slotCount());
            JsonObject root=UiDocumentCodec.toJson(document());root.getAsJsonArray("elements").add(e);
            if(commitJson(root))select(document().elements().size()-1,false);
        },height);
    }
    private String uniqueId(String prefix){Set<String> ids=new HashSet<>();document().elements().forEach(e->ids.add(e.id()));int i=1;while(ids.contains(prefix+"_"+i))i++;return prefix+"_"+i;}
    private void duplicate(){
        finishDrag();
        if(selected<0||protectedSelection(false))return;
        var ids=selectedIds();JsonObject root=UiDocumentCodec.toJson(document());var array=root.getAsJsonArray("elements");
        var used=new HashSet<String>();document().elements().forEach(e->used.add(e.id()));var created=new LinkedHashSet<String>();int slot=document().slotCount();
        for(int i=0;i<document().elements().size();i++){var e=document().elements().get(i);if(!ids.contains(e.id()))continue;
            var copy=array.get(i).getAsJsonObject().deepCopy();String id=e.type()+"_copy";int suffix=1;while(used.contains(id))id=e.type()+"_copy_"+suffix++;
            used.add(id);created.add(id);copy.addProperty("id",id);copy.addProperty("x",Math.min(4096,e.x()+8));copy.addProperty("y",Math.min(4096,e.y()+8));if(e.type().equals("slot"))copy.addProperty("index",slot++);array.add(copy);
        }
        if(commitJson(root)){selection.clear();selection.addAll(created);selected=document().elements().size()-1;}
    }
    private void delete(){
        finishDrag();
        if(selected<0||protectedSelection(true))return;
        var ids=selectedIds();JsonObject root=UiDocumentCodec.toJson(document());JsonArray array=root.getAsJsonArray("elements");
        for(int i=array.size()-1;i>=0;i--)if(ids.contains(array.get(i).getAsJsonObject().get("id").getAsString()))array.remove(i);
        var slots=new ArrayList<JsonObject>();for(var value:array){var e=value.getAsJsonObject();if(e.get("type").getAsString().equals("slot"))slots.add(e);}
        slots.sort(Comparator.comparingInt(e->e.get("index").getAsInt()));for(int i=0;i<slots.size();i++)slots.get(i).addProperty("index",i);
        if(commitJson(root)){selection.clear();selected=-1;}
    }
    private void reorder(int delta){
        finishDrag();
        if(selected<0||selected+delta<0||selected+delta>=document().elements().size())return;
        JsonObject root=UiDocumentCodec.toJson(document());JsonArray a=root.getAsJsonArray("elements");JsonElement other=a.get(selected+delta);a.set(selected+delta,a.get(selected));a.set(selected,other);
        if(commitJson(root))selected+=delta;
    }
    boolean commitJson(JsonObject root){try{replace(UiDocumentCodec.read(root.toString(),draw));status="";return true;}catch(RuntimeException ex){status=ex.getMessage();return false;}}
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);
        updateControls();Rect area=canvasBounds();viewport=canvasViewport();
        g.fill(area.x(),area.y(),area.right(),area.bottom(),0xFF202838);
        if(scene==null||!scene.document().equals(document()))scene=new UiScene(document(),preview);
        g.enableScissor(area.x(),area.y(),area.right(),area.bottom());
        try{
            scene.render(g,mx,my,viewport);
            if(!previewOnly){
                for(int i=0;i<document().elements().size();i++){
                    Element e=document().elements().get(i);Rect r=e.bounds(document().width(),document().height());
                    int x=(int)(viewport.x()+r.x()*viewport.scale()),y=(int)(viewport.y()+r.y()*viewport.scale());
                    int w=Math.max(1,(int)(r.width()*viewport.scale())),h=Math.max(1,(int)(r.height()*viewport.scale()));
                    g.renderOutline(x,y,w,h,selectedIds().contains(e.id())?0xFFFFCD66:0x306EA2CC);
                    if(selectedIds().contains(e.id())){g.fill(x+w-5,y+h-5,x+w,y+h,0xFFFFCD66);}
                }
            }
        }finally{g.disableScissor();}
        if(showLayers()){
            EditorTheme.panel(g,8,56,148,height-105);
            g.drawString(font,tr("layers"),14,62,EditorTheme.TEXT,false);
            int count=document().elements().size();layerScroll=Math.max(0,Math.min(layerScroll,Math.max(0,count-layerRows())));
            for(int row=0;row<layerRows()&&layerScroll+row<count;row++){
                int i=count-1-layerScroll-row,y=78+row*20;var e=document().elements().get(i);
                EditorTheme.row(g,12,y-1,140,19,selectedIds().contains(e.id()),mx>=12&&mx<152&&my>=y-1&&my<y+18);
                g.drawString(font,font.plainSubstrByWidth(layerLabel(e),130),16,y+4,i==selected?0xFFFFFFFF:EditorTheme.MUTED,false);
            }
            if(selected>=0){var e=document().elements().get(selected);g.drawString(font,"X "+e.x()+"  Y "+e.y(),14,height-80,EditorTheme.TEXT,false);g.drawString(font,e.width()+" × "+e.height(),14,height-67,EditorTheme.MUTED,false);}
        }
        super.render(g,mx,my,tick);
        String hint=previewOnly?tr("preview_hint").getString():tr("canvas_hint",Math.round(zoom*100)).getString();
        if(selected>=0&&!previewOnly)hint=document().elements().get(selected).id()+" · "+hint;
        g.drawString(font,font.plainSubstrByWidth(hint,width-16),8,height-46,EditorTheme.MUTED,false);
        if(!status.isBlank())g.drawString(font,font.plainSubstrByWidth(status,width-16),8,height-36,0xFFAA66,false);
        options.render(g,font,width,height);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        finishDrag();
        if(options.mouseClicked(mx,my,button,width,height))return true;
        if(showLayers()&&mx>=8&&mx<156&&my>=78&&my<78+layerRows()*20){
            int i=document().elements().size()-1-layerScroll-(int)((my-78)/20);
            if(i>=0&&i<document().elements().size()){select(i,hasControlDown());previewOnly=false;if(button==1)properties(false);}return true;
        }
        if(canvasBounds().contains(mx,my)&&viewport!=null){
            setFocused(null);
            if(button==2){panning=true;return true;}
            if(previewOnly){scene.click(mx,my,button);return true;}
            double x=viewport.localX(mx),y=viewport.localY(my);
            // The selected resize handle stays reachable even beneath a higher layer.
            if(button==0&&selected>=0&&selectedIds().size()==1&&!document().elements().get(selected).locked()){Rect r=document().elements().get(selected).bounds(document().width(),document().height());
                if(Math.abs(x-r.right())<7/viewport.scale()&&Math.abs(y-r.bottom())<7/viewport.scale()){startDrag(x,y,true);return true;}}
            var list=document().elements();for(int i=list.size()-1;i>=0;i--)if(!list.get(i).locked()&&list.get(i).bounds(document().width(),document().height()).contains(x,y)){
                if(hasControlDown()){select(i,true);return true;}
                if(!selectedIds().contains(list.get(i).id()))select(i,false);else selected=i;
                if(button==1)properties(false);else if(button==0)startDrag(x,y,false);return true;
            }
            selected=-1;selection.clear();return true;
        }
        return super.mouseClicked(mx,my,button);
    }
    private void startDrag(double x,double y,boolean resize){
        finishDrag();
        if(selected<0||selected>=document().elements().size())return;
        startX=x;startY=y;startElement=document().elements().get(selected);resizing=resize;
        dragBase=history.value();dragging=dragBase;dragSelection=selectedIds();
    }
    private void finishDrag(){
        if(dragging==null)return;
        history.commit(dragging);
        dragging=null;dragBase=null;dragSelection=Set.of();startElement=null;scene=null;
    }
    @Override public void removed(){finishDrag();panning=false;super.removed();}
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(panning&&button==2){panX+=dx;panY+=dy;return true;}
        if(dragging==null||dragBase==null||viewport==null||button!=0)return super.mouseDragged(mx,my,button,dx,dy);
        int snap=hasShiftDown()?1:grid;int deltaX=UiEditHistory.snap(viewport.localX(mx)-startX,snap),deltaY=UiEditHistory.snap(viewport.localY(my)-startY,snap);
        int index=java.util.stream.IntStream.range(0,dragBase.elements().size())
                .filter(i->dragBase.elements().get(i).id().equals(startElement.id())).findFirst().orElse(-1);
        if(index<0){finishDrag();return true;}
        JsonObject root=UiDocumentCodec.toJson(dragBase);JsonObject e=root.getAsJsonArray("elements").get(index).getAsJsonObject();
        if(resizing){
            int newWidth=Math.max(4,Math.min(4096,startElement.width()+deltaX)),newHeight=Math.max(4,Math.min(4096,startElement.height()+deltaY));
            e.addProperty("width",newWidth);e.addProperty("height",newHeight);
            // Compensate the anchor so the top-left stays fixed while dragging the bottom-right.
            e.addProperty("x",clamp(startElement.x()+(startElement.anchor().endsWith("right")?newWidth-startElement.width():startElement.anchor().endsWith("center")?(newWidth-startElement.width())/2:0)));
            e.addProperty("y",clamp(startElement.y()+(startElement.anchor().startsWith("bottom")?newHeight-startElement.height():startElement.anchor().startsWith("middle")?(newHeight-startElement.height())/2:0)));
        }else{dragging=UiSelectionOps.move(dragBase,dragSelection,deltaX,deltaY,draw);scene=null;return true;}
        dragging=UiDocumentCodec.read(root.toString(),draw);scene=null;return true;
    }
    private static int clamp(int value){return Math.max(-4096,Math.min(4096,value));}
    @Override public boolean mouseReleased(double mx,double my,int button){if(button==2){panning=false;return true;}if(button==0&&dragging!=null){finishDrag();return true;}return super.mouseReleased(mx,my,button);}
    @Override public boolean mouseScrolled(double mx,double my,double delta){
        if(options.mouseScrolled(delta,height))return true;
        if(showLayers()&&mx>=8&&mx<156&&my>=56&&my<height-49){layerScroll=Math.max(0,Math.min(Math.max(0,document().elements().size()-layerRows()),layerScroll-(int)Math.signum(delta)*3));return true;}
        if(canvasBounds().contains(mx,my)&&viewport!=null){
            if(hasControlDown()){
                finishDrag();double x=viewport.localX(mx),y=viewport.localY(my);zoom=Math.max(.5,Math.min(4,zoom*Math.pow(1.15,delta)));
                Viewport next=canvasViewport();panX+=mx-(next.x()+x*next.scale());panY+=my-(next.y()+y*next.scale());return true;
            }
            if(previewOnly&&scene!=null)return scene.scroll(mx,my,delta);
        }
        return super.mouseScrolled(mx,my,delta);
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key<GLFW.GLFW_KEY_LEFT_SHIFT||key>GLFW.GLFW_KEY_RIGHT_SUPER)finishDrag();
        if(options.keyPressed(key,height))return true;
        if(!previewOnly&&hasControlDown()&&key==GLFW.GLFW_KEY_Z){restore(hasShiftDown());return true;}
        if(!previewOnly&&hasControlDown()&&key==GLFW.GLFW_KEY_Y){restore(true);return true;}
        if(hasControlDown()&&key==GLFW.GLFW_KEY_S){apply();return true;}
        if(!previewOnly&&hasControlDown()&&key==GLFW.GLFW_KEY_D){duplicate();return true;}
        if(!previewOnly&&hasControlDown()&&key==GLFW.GLFW_KEY_A){selection.clear();selected=-1;for(int i=0;i<document().elements().size();i++){var e=document().elements().get(i);if(!e.locked()){selection.add(e.id());selected=i;}}return true;}
        if(key==GLFW.GLFW_KEY_HOME){fit();return true;}
        if(key==GLFW.GLFW_KEY_ENTER&&!previewOnly&&selected>=0){properties(false);return true;}
        if(key==GLFW.GLFW_KEY_DELETE&&!previewOnly){delete();return true;}
        if(selected>=0&&!previewOnly&&key>=GLFW.GLFW_KEY_RIGHT&&key<=GLFW.GLFW_KEY_UP){
            int step=hasShiftDown()?10:1;
            replace(UiSelectionOps.move(document(),selectedIds(),key==GLFW.GLFW_KEY_RIGHT?step:key==GLFW.GLFW_KEY_LEFT?-step:0,key==GLFW.GLFW_KEY_DOWN?step:key==GLFW.GLFW_KEY_UP?-step:0,draw));return true;
        }
        return super.keyPressed(key,scan,mods);
    }
    @Override public boolean charTyped(char c,int mods){return options.isOpen()||super.charTyped(c,mods);}
    @Override public void onClose(){
        finishDrag();
        if(!history.value().equals(initial))minecraft.setScreen(new EditorConfirmScreen(yes->minecraft.setScreen(yes?parent:this),tr("discard"),tr("discard_confirm")));
        else minecraft.setScreen(parent);
    }

    private void previewMenu(){
        finishDrag();
        if(previewOnly){previewOnly=false;scene=null;return;}
        options.open(tr("preview"),List.of(tr("preview_mode"),tr("fullscreen")),0,i->{if(i==0){previewOnly=true;scene=null;}else minecraft.setScreen(new UiLayoutPreviewScreen(this,document(),draw,parent.pages()));},height);
    }
    private void toggleLock(){
        finishDrag();
        if(selected<0)return;var ids=selectedIds();boolean lock=document().elements().stream().anyMatch(e->ids.contains(e.id())&&!e.locked());
        var root=UiDocumentCodec.toJson(document());for(var value:root.getAsJsonArray("elements")){var e=value.getAsJsonObject();if(ids.contains(e.get("id").getAsString()))e.addProperty("locked",lock);}commitJson(root);
    }
    private void arrange(){
        finishDrag();
        if(selected<0)return;
        var modes=UiSelectionOps.Align.values();var labels=new ArrayList<Component>();for(var mode:modes)labels.add(tr("align_selection."+mode.name().toLowerCase(Locale.ROOT)));
        labels.add(tr("lower"));labels.add(tr("raise"));labels.add(tr("copy_style"));labels.add(tr("paste_style"));
        options.open(tr("arrange"),labels,0,i->{try{
            if(i<modes.length)replace(UiSelectionOps.align(document(),selectedIds(),modes[i],draw));
            else if(i==modes.length)reorder(-1);else if(i==modes.length+1)reorder(1);
            else if(i==modes.length+2)copiedStyle=UiDocumentCodec.toJson(document()).getAsJsonArray("elements").get(selected).getAsJsonObject().deepCopy();
            else if(copiedStyle!=null)replace(UiSelectionOps.pasteStyle(document(),selectedIds(),copiedStyle,draw));
        }catch(RuntimeException ex){status=ex.getMessage();}},height,i->{
            long count=document().elements().stream().filter(e->selectedIds().contains(e.id())&&!e.locked()).count();
            if(i<modes.length)return count>0&&(i<6||count>=3);
            if(i==modes.length)return selectedIds().size()==1&&count==1&&selected>0;
            if(i==modes.length+1)return selectedIds().size()==1&&count==1&&selected<document().elements().size()-1;
            if(i==modes.length+2)return selectedIds().size()==1;
            return copiedStyle!=null&&count>0;
        });
    }
}
