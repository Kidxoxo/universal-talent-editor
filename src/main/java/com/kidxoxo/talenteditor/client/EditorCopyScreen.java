package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;
import java.util.stream.Collectors;

final class EditorCopyScreen extends EditorScreen {
    private final Screen parent;private final EditorBootstrap bootstrap;private final DefinitionKind kind;private final ResourceLocation source;private final String json;
    private EditBox id;private String status="";
    EditorCopyScreen(Screen parent,EditorBootstrap bootstrap,DefinitionKind kind,ResourceLocation source,String json){
        super(EditorDefinitionScreen.tr("copy_definition"));this.parent=parent;this.bootstrap=bootstrap;this.kind=kind;this.source=source;this.json=json;
    }
    static Set<ResourceLocation> ids(EditorBootstrap b,DefinitionKind kind){
        return switch(kind){
            case TALENT -> b.talents().stream().map(EditorBootstrap.TalentSummary::id).collect(Collectors.toSet());
            case RARITY -> b.rarities().stream().map(EditorBootstrap.RaritySummary::id).collect(Collectors.toSet());
            case POOL -> b.pools().stream().map(EditorBootstrap.PoolSummary::id).collect(Collectors.toSet());
            case COUNTER -> b.counters().stream().map(EditorBootstrap.CounterSummary::id).collect(Collectors.toSet());
            case ITEM -> b.configuredItems().stream().map(EditorBootstrap.ConfiguredItemSummary::id).collect(Collectors.toSet());
            case LAYOUT -> b.layouts().stream().map(EditorBootstrap.LayoutSummary::id).collect(Collectors.toSet());
            default -> Set.of();
        };
    }
    @Override protected void init(){
        int w=Math.min(560,width-32),x=(width-w)/2;id=new EditBox(font,x,80,w,20,title);id.setMaxLength(256);
        var taken=ids(bootstrap,kind);String candidate=source+"_copy";int i=2;while(taken.contains(ResourceLocation.tryParse(candidate)))candidate=source+"_copy_"+i++;
        id.setValue(candidate);addRenderableWidget(id);
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"),b->onClose()).bounds(x,height-28,(w-6)/2,20).build());
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("create_copy"),b->create()).bounds(x+(w+6)/2,height-28,(w-6)/2,20).build());
    }
    private void create(){
        var value=ResourceLocation.tryParse(id.getValue().strip());var latest=ClientEditorState.bootstrap();if(latest==null)latest=bootstrap;
        if(value==null||!id.getValue().contains(":")){status=EditorDefinitionScreen.tr("copy_invalid").getString();return;}
        if(ids(latest,kind).contains(value)){status=EditorDefinitionScreen.tr("copy_exists").getString();return;}
        EditorDefinitionScreen next=switch(kind){
            case TALENT -> TalentEditScreen.copyOf(latest,value,json);
            case RARITY -> RarityEditScreen.copyOf(latest,value,json);
            case POOL -> PoolEditScreen.copyOf(latest,value,json);
            case COUNTER -> CounterEditScreen.copyOf(latest,value,json);
            case ITEM -> ConfiguredItemEditScreen.copyOf(latest,value,json);
            case LAYOUT -> LayoutEditScreen.copyOf(latest,value,json);
            default -> throw new IllegalStateException();
        };
        next.markCopy();minecraft.setScreen(next);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);super.render(g,mx,my,tick);EditorTheme.title(g,font,title,width);
        int w=Math.min(560,width-32),x=(width-w)/2;
        g.drawWordWrap(font,EditorDefinitionScreen.tr("copy_hint",source),x,32,w,EditorTheme.MUTED);
        g.drawString(font,font.plainSubstrByWidth(status,w),x,110,EditorTheme.ERROR,false);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
