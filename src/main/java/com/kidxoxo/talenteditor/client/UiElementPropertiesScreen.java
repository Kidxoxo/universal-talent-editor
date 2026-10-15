package com.kidxoxo.talenteditor.client;

import com.google.gson.*;
import com.kidxoxo.talenteditor.compile.UiDocumentCodec;
import com.kidxoxo.talenteditor.domain.UiDocument;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Paged form keeps every property reachable at Minecraft's minimum GUI size. */
final class UiElementPropertiesScreen extends EditorScreen {
    private static final Set<String> STRINGS=Set.of("anchor","texture","hoverTexture","text","binding","action","target","align","buttonStyle","label");
    private static final Set<String> COLORS=Set.of("color","hoverColor","textColor","backdrop");
    private final UiCanvasEditorScreen parent;
    private final int index;
    private final Map<String,String> values=new LinkedHashMap<>();
    private final List<String> allKeys;
    private final Map<String,String> initialValues;
    private UiScene liveScene;private String liveKey="";
    private int formWidth(){return width>=700?Math.min(700,width*3/5):width;}
    private List<String> keys;
    private int group;
    private final EditorOptionPopup options=new EditorOptionPopup();
    private int page,pageSize;
    private String status="";
    UiElementPropertiesScreen(UiCanvasEditorScreen parent,int index){
        super(UiCanvasEditorScreen.tr(index<0?"canvas":"properties"));this.parent=parent;this.index=index;
        var root=UiDocumentCodec.toJson(parent.document());JsonObject object=index<0?root:root.getAsJsonArray("elements").get(index).getAsJsonObject();
        if(index<0)keys=List.of("width","height","backdrop");
        else{
            String type=parent.document().elements().get(index).type();
            ArrayList<String> list=new ArrayList<>(List.of("label","locked","x","y","width","height","anchor","opacity","color","hoverColor","texture","hoverTexture"));
            if(!type.equals("image")&&!type.equals("panel"))list.addAll(List.of("textColor","textScale","textX","textY","align"));
            if(type.equals("text"))list.addAll(List.of("text","binding"));
            if(type.equals("image")&&!parent.draw)list.add("binding");
            if(type.equals("button"))list.addAll(List.of("text","action","target","buttonStyle"));
            if(List.of("slot","offer","details").contains(type))list.addAll(List.of("iconSize","iconX","iconY","rarityY","bodyY"));
            keys=List.copyOf(list);
        }
        for(String key:keys)values.put(key,COLORS.contains(key)?String.format(Locale.ROOT,"#%08X",object.get(key).getAsInt()):object.get(key).getAsString());
        allKeys=keys;initialValues=Map.copyOf(values);chooseKeys();
    }
    private int category(String key){
        if(List.of("x","y","width","height","anchor","locked").contains(key))return 0;
        if(List.of("opacity","color","hoverColor","textColor","texture","hoverTexture","buttonStyle").contains(key))return 1;
        return 2;
    }
    private boolean relevant(String key){
        if(index<0)return true;
        String type=parent.document().elements().get(index).type();
        boolean interactive=List.of("button","slot","offer").contains(type);
        if((key.equals("hoverColor")||key.equals("hoverTexture"))&&!interactive)return false;
        if(key.equals("target"))return !parent.draw&&values.getOrDefault("action","").equals("draw");
        if((key.equals("color")||key.equals("hoverColor"))&&type.equals("button")&&values.getOrDefault("buttonStyle","").equals("vanilla"))return false;
        return true;
    }
    private void chooseKeys(){keys=index<0?allKeys:allKeys.stream().filter(this::relevant).filter(key->category(key)==group).toList();}
    private int offset(){return index<0?0:28;}
    @Override protected void init(){
        chooseKeys();
        pageSize=Math.max(2,Math.min(12,((height-103-offset())/38)*2));page=Math.max(0,Math.min(page,(keys.size()-1)/pageSize));
        if(index>=0){
            String[] groups={"geometry","appearance","content"};int tabWidth=(formWidth()-28)/3;
            for(int i=0;i<3;i++){int chosen=i;Button tab=addRenderableWidget(EditorWidgets.button(UiCanvasEditorScreen.tr("group."+groups[i]),b->{group=chosen;page=0;chooseKeys();rebuildWidgets();}).bounds(10+i*(tabWidth+4),27,tabWidth,20).build());tab.active=allKeys.stream().anyMatch(key->relevant(key)&&category(key)==chosen);}
        }
        int w=(formWidth()-30)/2;
        for(int i=page*pageSize;i<Math.min(keys.size(),(page+1)*pageSize);i++){
            String key=keys.get(i);int local=i-page*pageSize,x=10+(local%2)*(w+10),y=40+offset()+(local/2)*38;
            List<String> choices=choices(key);
            if(!choices.isEmpty()){
                addRenderableWidget(EditorWidgets.button(choiceLabel(key,values.get(key)),b->options.open(UiCanvasEditorScreen.tr("field."+key),
                        choices.stream().map(v->choiceLabel(key,v)).toList(),Math.max(0,choices.indexOf(values.get(key))),choice->{
                            String next=choices.get(choice);
                            if(key.equals("anchor"))changeAnchor(next);else values.put(key,next);
                            rebuildWidgets();
                        },height)).bounds(x,y,w,20).build());
            }else{
                boolean texture=key.equals("texture")||key.equals("hoverTexture");
                boolean picker=texture||key.equals("target")||COLORS.contains(key);
                boolean opacity=key.equals("opacity");
                EditBox field=new EditBox(font,x,y,picker?w-44:opacity?Math.min(64,w/3):w,20,UiCanvasEditorScreen.tr("field."+key));
                field.setTooltip(Tooltip.create(UiCanvasEditorScreen.tr("field."+key).copy().append(" · ").append(fieldHint(key))));
                field.setMaxLength(key.equals("text")?2048:512);field.setValue(values.get(key));field.setResponder(value->{values.put(key,value);status="";});addRenderableWidget(field);
                if(opacity){
                    double initial;try{initial=Double.parseDouble(values.get(key));}catch(Exception ex){initial=1;}
                    int fw=Math.min(64,w/3);
                    class OpacitySlider extends EditorSlider {
                        OpacitySlider(double initialValue){super(x+fw+4,y,w-fw-4,20,Component.empty(),Math.max(0,Math.min(1,initialValue)));updateMessage();}
                        void sync(String input){try{value=Math.max(0,Math.min(1,Double.parseDouble(input)));updateMessage();}catch(NumberFormatException ignored){}}
                        protected void updateMessage(){setMessage(Component.literal(Math.round(value*100)+"%"));}
                        protected void applyValue(){field.setValue(String.format(Locale.ROOT,"%.2f",value));}
                    }
                    OpacitySlider slider=addRenderableWidget(new OpacitySlider(initial));
                    field.setResponder(value->{values.put(key,value);status="";slider.sync(value);});
                    slider.setTooltip(Tooltip.create(EditorDefinitionScreen.tr("alpha_hint")));

                }
                if(picker)addRenderableWidget(EditorWidgets.button(Component.literal("…"),b->{
                    if(COLORS.contains(key))minecraft.setScreen(new EditorColorScreen(this,values.get(key),v->values.put(key,v)));
                    else minecraft.setScreen(new ResourceReferencePickerScreen(this,texture?ResourceReferencePickerScreen.Source.TEXTURE:ResourceReferencePickerScreen.Source.POOL,parent.parent().bootstrap(),id->{values.put(key,id.toString());status="";}));
                }).bounds(x+w-40,y,40,20).tooltip(Tooltip.create(EditorDefinitionScreen.tr(COLORS.contains(key)?"color":"choose_resource"))).build());
            }
        }
        int bw=(formWidth()-40)/4;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"),b->onClose()).bounds(10,height-26,bw,20).build());
        var previous=addRenderableWidget(EditorWidgets.button(Component.literal("<"),b->{page--;rebuildWidgets();}).bounds(16+bw,height-26,bw,20).build());previous.active=page>0;previous.visible=keys.size()>pageSize;
        var next=addRenderableWidget(EditorWidgets.button(Component.literal(">"),b->{page++;rebuildWidgets();}).bounds(22+bw*2,height-26,bw,20).build());next.active=(page+1)*pageSize<keys.size();next.visible=keys.size()>pageSize;
        addRenderableWidget(EditorWidgets.button(UiCanvasEditorScreen.tr("apply_canvas"),b->apply()).bounds(28+bw*3,height-26,bw,20).build());
    }
    private Component fieldHint(String key){
        if(COLORS.contains(key))return UiCanvasEditorScreen.tr("color_hint");
        if(key.equals("opacity"))return Component.literal("0–1 · ").append(EditorDefinitionScreen.tr("alpha_hint"));
        if(key.equals("textScale"))return Component.literal("0.5–3");
        if(List.of("x","y").contains(key))return Component.literal("−4096–4096 px");
        if(List.of("width","height").contains(key))return Component.literal(index<0?(key.equals("width")?"240–4096 px":"135–4096 px"):"4–4096 px");
        return UiCanvasEditorScreen.tr("field."+key);
    }
    private Component choiceLabel(String key,String value){
        if(key.equals("locked"))return Component.translatable(Boolean.parseBoolean(value)?"options.on":"options.off");
        if(value.isBlank())return UiCanvasEditorScreen.tr("literal");
        return UiCanvasEditorScreen.tr((key.equals("anchor")?"anchor.":key.equals("binding")?"binding.":key.equals("align")?"align.":key.equals("buttonStyle")?"buttonStyle.":"action.")+value);
    }
    private List<String> choices(String key){return switch(key){case "anchor"->UiDocumentCodec.ANCHORS;
        case "action"->parent.draw?UiDocumentCodec.DRAW_ACTIONS:UiDocumentCodec.BOOK_ACTIONS;
        case "align"->List.of("left","center","right");case "buttonStyle"->List.of("vanilla","custom");
        case "locked"->List.of("false","true");
        case "binding"->index>=0&&parent.document().elements().get(index).type().equals("image")?List.of("","page_background"):
                parent.draw?UiDocumentCodec.DRAW_BINDINGS:List.of("","title","summary","page");default->List.of();};}
    private void changeAnchor(String next){
        try{
            UiDocument current=UiDocumentCodec.read(build().toString(),parent.draw);var old=current.elements().get(index);var r=old.bounds(current.width(),current.height());
            values.put("anchor",next);
            int ax=next.endsWith("right")?current.width()-r.width():next.endsWith("center")?(current.width()-r.width())/2:0;
            int ay=next.startsWith("bottom")?current.height()-r.height():next.startsWith("middle")?(current.height()-r.height())/2:0;
            values.put("x",Integer.toString(r.x()-ax));values.put("y",Integer.toString(r.y()-ay));
        }catch(RuntimeException ex){status=ex.getMessage();}
    }
    private JsonObject build(){
        JsonObject root=UiDocumentCodec.toJson(parent.document());JsonObject e=index<0?root:root.getAsJsonArray("elements").get(index).getAsJsonObject();
        for(String key:allKeys){String value=values.get(key);
            // Hidden controls keep the original valid data, not incomplete text from a previously visible control.
            if(!relevant(key))continue;
            if(key.equals("locked"))e.addProperty(key,Boolean.parseBoolean(value));
            else if(STRINGS.contains(key))e.addProperty(key,value);
            else if(COLORS.contains(key)){
                String hex=value.strip().replaceFirst("^#","");
                if(!hex.matches("[0-9A-Fa-f]{8}"))throw new IllegalArgumentException(UiCanvasEditorScreen.tr("field."+key).getString()+": "+UiCanvasEditorScreen.tr("color_hint").getString());
                e.addProperty(key,(int)Long.parseLong(hex,16));
            }else try {
                if(key.equals("opacity")||key.equals("textScale"))e.addProperty(key,Double.parseDouble(value.strip()));
                else e.addProperty(key,Integer.parseInt(value.strip()));
            } catch(NumberFormatException ex) {throw new IllegalArgumentException(UiCanvasEditorScreen.tr("field."+key).getString()+": "+EditorDefinitionScreen.tr("invalid_number").getString());}
        }return root;
    }
    private void apply(){try{UiDocument next=UiDocumentCodec.read(build().toString(),parent.draw);parent.replace(next);minecraft.setScreen(parent);}catch(RuntimeException ex){status=ex.getMessage();}}
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);super.render(g,mx,my,tick);EditorTheme.title(g,font,title,width);if(index>=0)EditorTheme.selectedTab(g,10+group*((formWidth()-28)/3+4),27,(formWidth()-28)/3);
        int w=(formWidth()-30)/2;
        for(int i=page*pageSize;i<Math.min(keys.size(),(page+1)*pageSize);i++){
            int local=i-page*pageSize,x=10+(local%2)*(w+10),y=29+offset()+(local/2)*38;
            g.drawString(font,UiCanvasEditorScreen.tr("field."+keys.get(i)),x,y,EditorTheme.MUTED,false);
            String key=keys.get(i);
            if(key.equals("texture")||key.equals("hoverTexture")){
                String value=values.get(key);var id=net.minecraft.resources.ResourceLocation.tryParse(value);
                if(!value.isBlank()&&(id==null||minecraft.getResourceManager().getResource(id).isEmpty()))g.drawString(font,"!",x+w-10,y,EditorTheme.WARNING,false);
            }
        }
        g.drawString(font,UiCanvasEditorScreen.tr("property_hint",page+1,Math.max(1,(keys.size()+pageSize-1)/pageSize)),10,height-53,EditorTheme.MUTED,false);
        if(!status.isBlank())g.drawString(font,font.plainSubstrByWidth(status,width-20),10,height-40,0xFFAA66,false);
        if(width>=700){
            String state=values.toString();if(!state.equals(liveKey)){liveKey=state;try{liveScene=new UiScene(UiDocumentCodec.read(build().toString(),parent.draw),new UiPreviewContext(parent.draw,parent.parent().pages()));}catch(RuntimeException ex){liveScene=null;}}
            int x=formWidth()+8;EditorTheme.panel(g,x,32,width-x-8,height-74);
            g.drawString(font,UiCanvasEditorScreen.tr("preview_mode"),x+6,40,EditorTheme.TEXT,false);
            if(liveScene!=null)liveScene.render(g,mx,my,UiDocument.Viewport.fit(liveScene.document(),x+4,60,width-x-16,height-110));
        }
        options.render(g,font,width,height);
    }
    @Override public boolean mouseClicked(double x,double y,int b){return options.mouseClicked(x,y,b,width,height)||super.mouseClicked(x,y,b);}
    @Override public boolean mouseScrolled(double x,double y,double d){return options.mouseScrolled(d,height)||super.mouseScrolled(x,y,d);}
    @Override public boolean keyPressed(int k,int s,int m){return options.keyPressed(k,height)||super.keyPressed(k,s,m);}
    @Override public boolean charTyped(char c,int m){return options.isOpen()||super.charTyped(c,m);}
    @Override public void onClose(){if(values.equals(initialValues))minecraft.setScreen(parent);else minecraft.setScreen(new EditorLeaveScreen(this,()->minecraft.setScreen(parent),this::apply));}
}
