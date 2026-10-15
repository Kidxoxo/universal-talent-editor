package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.UiDocument.Element;
import com.kidxoxo.talenteditor.network.PlayerTalentState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Local sample data: no reference to EditorNetwork or any gameplay mutation. */
final class UiPreviewContext implements UiScene.Context {
    private final boolean draw;
    private int selected, page;
    private boolean effects;
    private String status="";
    private final List<LayoutDraft.PageDraft> pages;
    UiPreviewContext(boolean draw) { this(draw,List.of()); }
    UiPreviewContext(boolean draw,List<LayoutDraft.PageDraft> pages) { this.draw=draw;this.pages=List.copyOf(pages); }
    public String texture(Element e) {
        return e.binding().equals("page_background") && page<pages.size() && !pages.get(page).background().isBlank() ? pages.get(page).background() : e.texture();
    }
    public Component text(String binding) {
        return switch(binding) {
            case "title" -> Component.translatable(draw?"screen.talent_editor.draw.title":"screen.talent_editor.book.title");
            case "summary" -> draw ? Component.translatable("screen.talent_editor.draw.summary_compact",12,1)
                    : Component.translatable("screen.talent_editor.book.summary_compact",6);
            case "page" -> Component.translatable("screen.talent_editor.book.page",page+1,Math.max(3,pages.size()+1));
            case "mechanics" -> Component.translatable("screen.talent_editor.ui.sample_mechanics");
            case "status" -> Component.literal(status);default -> Component.empty();
        };
    }
    public Component label(String action) {
        if(action.equals("previous"))return Component.literal("<");
        if(action.equals("next"))return Component.literal(">");
        return Component.translatable("screen.talent_editor.ui.action."+action);
    }
    public boolean enabled(String action) { return switch(action) {
        case "previous" -> page>0; case "next" -> page<Math.max(2,pages.size()); default -> true;
    }; }
    public void action(String action,String target) {
        if(action.equals("next"))page=Math.min(Math.max(2,pages.size()),page+1);if(action.equals("previous"))page=Math.max(0,page-1);
        if(action.equals("effects"))effects=!effects;
        status=Component.translatable("screen.talent_editor.ui.preview_action",label(action)).getString();
    }
    public UiScene.Card card(Element e) {
        int index=e.type().equals("details")?selected:e.index();
        var state=ClientEditorState.playerTalentState();
        List<PlayerTalentState.TalentView> talents=state==null?List.of():state.talents();
        if(!talents.isEmpty())return UiCards.talent(talents.get(index%talents.size()),index==selected,effects,List.of());
        return new UiScene.Card(Component.translatable("screen.talent_editor.ui.sample_talent",index+1),
                Component.translatable("screen.talent_editor.ui.sample_rarity"),
                List.of(Component.translatable("screen.talent_editor.ui.sample_description")),
                Optional.of(new ResourceLocation("minecraft","textures/item/enchanted_book.png")),0x69CBFF,index==selected);
    }
    public void select(Element e) { selected=e.index(); }
}
