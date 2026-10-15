package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.UiDocument;
import com.kidxoxo.talenteditor.domain.UiDocument.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;
import java.util.function.Consumer;

/** Shared by the real book/draw screens and the editor. Preview contexts never send packets. */
final class UiScene {
    record Card(Component title, Component subtitle, List<Component> body, Optional<ResourceLocation> icon,
                int color, boolean selected) { Card { body=List.copyOf(body); } }
    interface Context {
        Component text(String binding);
        Component label(String action);
        boolean enabled(String action);
        void action(String action,String target);
        Card card(Element element);
        void select(Element element);
        default String texture(Element element) { return element.texture(); }
    }
    private final UiDocument document;
    private final Context context;
    private final Map<String,Integer> scrolls=new HashMap<>();
    private final Map<String,Button> buttons=new HashMap<>();
    private final Map<String,Button> nativeVisuals=new HashMap<>();
    private Viewport viewport;
    UiScene(UiDocument document,Context context) { this.document=document;this.context=context; }
    UiDocument document() { return document; }
    void install(int width,int height,Consumer<Button> add) {
        viewport=Viewport.fit(document,0,0,width,height); buttons.clear();
        // Reversed widget order matches the topmost interactive element used by pointer hit testing.
        var elements=document.elements();
        for(int i=elements.size()-1;i>=0;i--) {
            Element e=elements.get(i);if(!e.type().equals("button") || e.opacity()==0)continue;
            Rect r=e.bounds(document.width(),document.height());
            Button button=new Button((int)(viewport.x()+r.x()*viewport.scale()),(int)(viewport.y()+r.y()*viewport.scale()),
                    Math.max(1,(int)(r.width()*viewport.scale())),Math.max(1,(int)(r.height()*viewport.scale())),
                    label(e),b->{ if(context.enabled(e.action())) context.action(e.action(),e.target()); },supplier -> supplier.get()) {
                @Override protected void renderWidget(GuiGraphics g,int mx,int my,float tick) { }
            };
            buttons.put(e.id(),button);add.accept(button);
        }
    }
    private Component label(Element e) { return e.text().isBlank()?context.label(e.action()):Component.literal(e.text()); }
    void render(GuiGraphics g,int mx,int my,int width,int height) {
        // Cover letterboxing as well as the fitted canvas without stretching its images.
        g.fill(0,0,width,height,document.backdrop());
        render(g,mx,my,Viewport.fit(document,0,0,width,height),false);
    }
    void render(GuiGraphics g,int mx,int my,Viewport view) {
        render(g,mx,my,view,true);
    }
    private void render(GuiGraphics g,int mx,int my,Viewport view,boolean canvasBackdrop) {
        viewport=view;
        g.pose().pushPose();g.pose().translate(view.x(),view.y(),0);g.pose().scale((float)view.scale(),(float)view.scale(),1);
        try {
            if(canvasBackdrop)g.fill(0,0,document.width(),document.height(),document.backdrop());
            for(Element e:document.elements()) {
                Button b=buttons.get(e.id());if(b!=null){b.active=context.enabled(e.action());b.setMessage(label(e));}
                if(e.opacity()<=0)continue;
                Rect r=e.bounds(document.width(),document.height());
                boolean hovered=r.contains(view.localX(mx),view.localY(my));
                renderElement(g,e,r,hovered,b!=null&&b.isFocused());
            }
        } finally { g.pose().popPose(); }
    }
    private void renderElement(GuiGraphics g,Element e,Rect r,boolean hovered,boolean focused) {
        boolean cardType=List.of("slot","offer","details").contains(e.type());
        Card card=cardType?context.card(e):null;
        if(cardType&&card==null)return;
        double opacity=e.opacity()*(e.type().equals("button")&&!context.enabled(e.action())?0.4:1);
        boolean highlight=focused||(hovered&&e.interactive())||(card!=null&&card.selected());
        boolean nativeButton=e.type().equals("button")&&e.buttonStyle().equals("vanilla");
        if(nativeButton) {
            Button visual=nativeVisuals.computeIfAbsent(e.id(),key->Button.builder(Component.empty(),ignored->{}).bounds(r.x(),r.y(),r.width(),r.height()).build());
            visual.active=context.enabled(e.action());visual.setAlpha((float)e.opacity());
            visual.setFocused(focused);
            visual.render(g,hovered?r.x()+1:-10000,hovered?r.y()+1:-10000,0);
        } else g.fill(r.x(),r.y(),r.right(),r.bottom(),alpha(highlight?e.hoverColor():e.color(),opacity));
        String texture=highlight&&!e.hoverTexture().isBlank()?e.hoverTexture():context.texture(e);
        image(g,texture,r,opacity);
        int sx=(int)Math.ceil(viewport.x()+r.x()*viewport.scale()),sy=(int)Math.ceil(viewport.y()+r.y()*viewport.scale());
        g.enableScissor(sx,sy,(int)Math.floor(viewport.x()+r.right()*viewport.scale()),(int)Math.floor(viewport.y()+r.bottom()*viewport.scale()));
        try {
            if(e.type().equals("text")||e.type().equals("button")) {
                Component text=e.type().equals("button")?label(e):e.binding().isBlank()?Component.literal(e.text()):context.text(e.binding());
                drawText(g,e,r,text,e.textX(),e.textY(),opacity);
            } else if(card!=null) {
                g.fill(r.x(),r.y(),r.x()+2,r.bottom(),alpha(0xFF000000|(card.color()&0xFFFFFF),opacity));
                if(e.iconSize()>0)card.icon().ifPresent(icon->image(g,icon.toString(),
                        new Rect(r.x()+e.iconX(),r.y()+e.iconY(),e.iconSize(),e.iconSize()),opacity));
                drawText(g,e,r,card.title(),e.textX(),e.textY(),opacity,Math.max(e.textY()+9,e.rarityY()));
                drawText(g,e,r,card.subtitle(),e.textX(),e.rarityY(),opacity,Math.max(e.rarityY()+9,e.bodyY()));
                if(!e.type().equals("slot")) {
                    int bodyTop=Math.min(r.bottom(),r.y()+e.bodyY());
                    g.enableScissor(sx,(int)Math.ceil(viewport.y()+bodyTop*viewport.scale()),
                            (int)Math.floor(viewport.x()+r.right()*viewport.scale()),(int)Math.floor(viewport.y()+r.bottom()*viewport.scale()));
                    try {
                        List<FormattedCharSequence> lines=lines(card,e,r);
                        int lineHeight=Math.max(1,(int)Math.ceil(11*e.textScale()));
                        int maximum=Math.max(0,lines.size()*lineHeight-(r.height()-e.bodyY()-3));
                        int scroll=Math.max(0,Math.min(scrolls.getOrDefault(e.id(),0),maximum));scrolls.put(e.id(),scroll);
                        int y=e.bodyY()-scroll;
                        for(var line:lines) { if(y+lineHeight>=e.bodyY()&&y<r.height())drawLine(g,e,r,line,e.textX(),y,opacity);y+=lineHeight; }
                        if(maximum>0) {
                            int track=Math.max(1,r.height()-e.bodyY());int thumb=Math.max(4,track*track/Math.max(1,lines.size()*lineHeight));
                            int top=bodyTop+(track-thumb)*scroll/maximum;
                            g.fill(r.right()-2,top,r.right(),Math.min(r.bottom(),top+thumb),alpha(e.textColor(),opacity));
                        }
                    } finally { g.disableScissor(); }
                }
            }
            if(focused)g.renderOutline(r.x(),r.y(),r.width(),r.height(),0xFFFFFFFF);
        } finally { g.disableScissor(); }
    }
    private List<FormattedCharSequence> lines(Card card,Element e,Rect r) {
        List<FormattedCharSequence> result=new ArrayList<>();
        for(Component text:card.body())result.addAll(Minecraft.getInstance().font.split(text,Math.max(1,(int)((r.width()-e.textX()-4)/e.textScale()))));
        return result;
    }
    private void drawText(GuiGraphics g,Element e,Rect r,Component text,int x,int y,double opacity) {
        drawText(g,e,r,text,x,y,opacity,r.height());
    }
    private void drawText(GuiGraphics g,Element e,Rect r,Component text,int x,int y,double opacity,int bottom) {
        var font=Minecraft.getInstance().font;
        var lines=font.split(text,Math.max(1,(int)((r.width()-x-2)/e.textScale())));
        for(var line:lines) {
            if(y+9*e.textScale()>Math.min(r.height(),bottom))break;
            int textWidth=(int)Math.ceil(font.width(line)*e.textScale());
            int lineX=e.align().equals("center")?(r.width()-textWidth)/2+x:e.align().equals("right")?r.width()-textWidth-x-2:x;
            drawLine(g,e,r,line,lineX,y,opacity);y+=(int)Math.ceil(11*e.textScale());
        }
    }
    private void drawLine(GuiGraphics g,Element e,Rect r,FormattedCharSequence text,int x,int y,double opacity) {
        int color=alpha(e.textColor(),opacity);if((color>>>24)<4)return;
        g.pose().pushPose();g.pose().translate(r.x()+x,r.y()+y,0);g.pose().scale((float)e.textScale(),(float)e.textScale(),1);
        g.drawString(Minecraft.getInstance().font,text,0,0,color,false);g.pose().popPose();
    }
    static void image(GuiGraphics g,String texture,Rect r,double opacity) {
        if(texture.isBlank()||opacity<=0)return;
        ResourceLocation id=ResourceLocation.tryParse(texture);
        if(id==null||Minecraft.getInstance().getResourceManager().getResource(id).isEmpty()) {
            g.fill(r.x(),r.y(),r.right(),r.bottom(),alpha(0x807F207F,opacity));return;
        }
        g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();g.setColor(1,1,1,(float)opacity);
        try { g.blit(id,r.x(),r.y(),0F,0F,r.width(),r.height(),r.width(),r.height());g.flush(); }
        finally { g.setColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.disableBlend(); }
    }
    static int alpha(int color,double opacity) { return ((int)Math.round((color>>>24)*opacity)<<24)|(color&0xFFFFFF); }
    boolean click(double mx,double my,int button) {
        if(button!=0||viewport==null)return false;
        var list=document.elements();
        for(int i=list.size()-1;i>=0;i--) {
            Element e=list.get(i);if(e.opacity()<=0||!e.interactive()||!e.bounds(document.width(),document.height()).contains(viewport.localX(mx),viewport.localY(my)))continue;
            if(e.type().equals("button")) { if(context.enabled(e.action())) { var b=buttons.get(e.id());if(b!=null)b.playDownSound(Minecraft.getInstance().getSoundManager());context.action(e.action(),e.target()); }return true; }
            if(context.card(e)!=null){context.select(e);return true;}
        }
        return false;
    }
    boolean scroll(double mx,double my,double delta) {
        if(viewport==null)return false;
        var list=document.elements();
        for(int i=list.size()-1;i>=0;i--) {
            Element e=list.get(i);if(!List.of("offer","details").contains(e.type())||e.opacity()==0)continue;
            Rect r=e.bounds(document.width(),document.height());Card card=context.card(e);
            if(card!=null&&r.contains(viewport.localX(mx),viewport.localY(my))) {
                int max=Math.max(0,lines(card,e,r).size()*(int)Math.ceil(11*e.textScale())-(r.height()-e.bodyY()-3));
                scrolls.put(e.id(),Math.max(0,Math.min(max,scrolls.getOrDefault(e.id(),0)-(int)Math.signum(delta)*22)));return true;
            }
        }
        return false;
    }
    void resetScroll() { scrolls.clear(); }
}
