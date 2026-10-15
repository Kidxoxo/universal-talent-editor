package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

final class EditorImportPreviewScreen extends EditorScreen {
    private final EditorSettingsScreen parent;private final String confirmation;private final Component summary;
    private List<FormattedCharSequence> lines=List.of();private int scroll;
    EditorImportPreviewScreen(EditorSettingsScreen parent,String json){
        super(EditorDefinitionScreen.tr("import_preview"));this.parent=parent;
        JsonObject root=JsonParser.parseString(json).getAsJsonObject();JsonObject token=new JsonObject();token.add("path",root.get("path"));token.add("sha256",root.get("sha256"));confirmation=token.toString();
        StringBuilder counts=new StringBuilder();for(String key:List.of("talents","rarities","pools","counters","layouts","items")){
            var value=root.getAsJsonObject("counts").get(key);counts.append(EditorDefinitionScreen.tr("count."+key,value==null?0:value.getAsInt()).getString()).append("  ");
        }
        summary=EditorDefinitionScreen.tr("import_preview_hint",root.get("path").getAsString(),root.get("world").getAsString(),counts.toString(),root.get("resources").getAsInt());
    }
    @Override protected void init(){
        lines=font.split(summary,width-40);
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"),b->onClose()).bounds(16,height-28,(width-38)/2,20).build());
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("confirm_import"), EditorTheme.Tone.PRIMARY,b ->{minecraft.setScreen(parent);parent.confirmImport(confirmation);}).bounds(22+(width-38)/2,height-28,(width-38)/2,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);super.render(g,mx,my,tick);EditorTheme.title(g,font,title,width);int rows=Math.max(1,(height-70)/12);scroll=Math.min(scroll,Math.max(0,lines.size()-rows));
        for(int i=0;i<rows&&scroll+i<lines.size();i++)g.drawString(font,lines.get(scroll+i),20,32+12*i,EditorTheme.TEXT,false);
    }
    @Override public boolean mouseScrolled(double x,double y,double d){scroll=Math.max(0,scroll-(int)Math.signum(d)*3);return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
