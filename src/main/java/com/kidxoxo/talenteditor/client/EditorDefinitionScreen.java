package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Shared save lifecycle. Responses are addressed to the submitting screen, never to a global result slot. */
abstract class EditorDefinitionScreen extends EditorScreen {
    private static final AtomicLong REQUESTS=new AtomicLong();
    private static final Map<Long,EditorDefinitionScreen> WAITING=new HashMap<>();
    private final EditorDraftState session=new EditorDraftState();
    private final Map<AbstractWidget,Boolean> disabled=new IdentityHashMap<>();
    private DefinitionKind kind;
    private ValidationResultPacket result;
    private String resultSnapshot;
    private String renderedDraft, renderedFields;
    private static final com.google.gson.Gson FIELD_GSON=new com.google.gson.Gson();
    private Runnable afterSave;
    private boolean committing;
    private boolean copying, unsavedCopy;
    private EditorBootstrap copyBootstrap;
    private final EditorOptionPopup definitionOptions=new EditorOptionPopup();
    protected EditorDefinitionScreen(Component title){super(title);}
    static net.minecraft.network.chat.MutableComponent tr(String key,Object... args){return Component.translatable("screen.talent_editor.workflow."+key,args);}
    protected abstract String draftState();
    protected abstract void saveDraft();
    protected abstract void committed(ValidationResultPacket value);
    protected final String fieldsState(){
        var values=new ArrayList<String>();
        for(var child:children())if(child instanceof EditBox f)values.add(f.getValue());
        return FIELD_GSON.toJson(values);
    }
    protected final void watchDefinition(DefinitionKind value){kind=value;invalidateDraftView();session.initialize(draftState());}
    final boolean submissionPending(){return session.pending();}
    final void markCopy(){unsavedCopy=true;}
    protected static EditorBootstrap latestBootstrap(EditorBootstrap fallback){
        EditorBootstrap latest=ClientEditorState.bootstrap();
        return latest!=null&&latest.revision()>=fallback.revision()?latest:fallback;
    }
    protected final void definitionActions(EditorBootstrap bootstrap, ResourceLocation id, boolean existing, Runnable references){
        if(session.pending())return;
        List<Component> items=existing?List.of(tr("references"),tr("delete"),tr("copy_definition")):List.of(tr("copy_definition"));
        definitionOptions.open(tr("more"),items,0,index->{
            if(existing&&index==0){references.run();return;}
            if(existing&&index==1){
                EditorBootstrap latest=latestBootstrap(bootstrap);
                var ref=new DefinitionReferenceScreen(this,latest,kind,id,DefinitionReferenceScreen.inboundCount(latest,kind,id));
                minecraft.setScreen(ref);ref.openDeletionFromActions();return;
            }
            copying=true;copyBootstrap=latestBootstrap(bootstrap);
            try{saveDraft();}finally{copying=false;}
        },height);
    }
    protected final void sendDefinition(long revision,ResourceLocation id,String json,boolean commit){
        if(session.pending())return;
        if(copying){minecraft.setScreen(new EditorCopyScreen(this,copyBootstrap,kind,id,json));return;}
        if(unsavedCopy&&ClientEditorState.bootstrap()!=null&&EditorCopyScreen.ids(ClientEditorState.bootstrap(),kind).contains(id)){
            minecraft.setScreen(new EditorConfirmScreen(ignored->minecraft.setScreen(this),tr("copy_exists"),tr("copy_exists")));return;
        }
        long request=REQUESTS.incrementAndGet();session.begin(request,draftState());committing=commit;result=null;
        WAITING.put(request,this);
        switch(kind){
            case TALENT -> EditorNetwork.submitTalent(revision,id,json,commit,request);
            case POOL -> EditorNetwork.submitPool(revision,id,json,commit,request);
            case RARITY -> EditorNetwork.submitRarity(revision,id,json,commit,request);
            case COUNTER -> EditorNetwork.submitCounter(revision,id,json,commit,request);
            case LAYOUT -> EditorNetwork.submitLayout(revision,id,json,commit,request);
            case ITEM -> EditorNetwork.submitConfiguredItem(revision,id,json,commit,request);
            default -> throw new IllegalStateException("Unsupported editor definition");
        }
    }
    static void receive(ValidationResultPacket value){
        var screen=WAITING.remove(value.requestId());if(screen==null)return;
        screen.accept(value);
    }
    static void clearPending(){WAITING.clear();}
    private void accept(ValidationResultPacket value){
        if(!session.complete(value.requestId(),value.committed()))return;
        invalidateDraftView();
        disabled.forEach((widget,active)->widget.active=active);disabled.clear();
        if(value.committed()){committed(value);session.saved(draftState());unsavedCopy=false;}
        result=value;resultSnapshot=draftState();
        Runnable leave=afterSave;afterSave=null;
        if(value.committed()&&leave!=null)leave.run();
    }
    protected final void leaveDefinition(Runnable leave){
        if(session.pending())return;
        if(!unsavedCopy&&!session.dirty(draftState())){leave.run();return;}
        minecraft.setScreen(new EditorLeaveScreen(this,leave,()->{
            afterSave=leave;saveDraft();if(!session.pending())afterSave=null;
        }));
    }
    protected final void renderSubmissionResult(GuiGraphics g,int x,int w,String local){
        String fields=fieldsState();
        if(renderedDraft==null||!Objects.equals(fields,renderedFields)){renderedDraft=draftState();renderedFields=fields;}
        String current=renderedDraft;
        if(result!=null&&!Objects.equals(resultSnapshot,current))result=null;
        Component message=Component.literal(local);int color=EditorTheme.MUTED;
        if(session.pending()){message=tr(committing?"saving":"validating");color=EditorTheme.ACCENT;}
        else if(result!=null){
            if(result.conflict()){message=Component.translatable("screen.talent_editor.result.conflict",result.activeRevision());color=EditorTheme.ERROR;}
            else if(!result.accepted()){message=tr("issues",result.issues().size());color=EditorTheme.ERROR;}
            else if(!result.issues().isEmpty()){message=tr("warnings",result.issues().size());color=EditorTheme.WARNING;}
            else{message=result.committed()?Component.translatable("screen.talent_editor.result.saved",result.activeRevision()):Component.translatable("screen.talent_editor.result.valid");color=EditorTheme.SUCCESS;}
        }else if(unsavedCopy||session.dirty(current)){message=tr("unsaved").append(local.isBlank()||local.equals(Component.translatable("screen.talent_editor.result.waiting").getString())?"":" · "+local);color=EditorTheme.WARNING;}
        g.drawString(font,font.plainSubstrByWidth(message.getString(),w),x,height-42,color,false);
        definitionOptions.render(g,font,width,height);
    }
    protected final void invalidateDraftView(){renderedDraft=null;renderedFields=null;}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){invalidateDraftView();return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){invalidateDraftView();return super.mouseReleased(x,y,button);}
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        if(session.pending())for(var child:children())if(child instanceof AbstractWidget w){disabled.putIfAbsent(w,w.active);w.active=false;}
        super.render(g,mx,my,tick);
    }
    @Override public boolean mouseClicked(double x,double y,int b){invalidateDraftView();
        if(session.pending())return true;
        if(definitionOptions.mouseClicked(x,y,b,width,height))return true;
        if(b==0&&y>=height-47&&y<height-30&&result!=null&&!result.issues().isEmpty()){
            minecraft.setScreen(new EditorValidationScreen(this,result));return true;
        }
        return super.mouseClicked(x,y,b);
    }
    @Override public boolean charTyped(char c,int m){invalidateDraftView();return session.pending()||definitionOptions.isOpen()||super.charTyped(c,m);}
    @Override public boolean mouseScrolled(double x,double y,double d){invalidateDraftView();return definitionOptions.mouseScrolled(d,height)||super.mouseScrolled(x,y,d);}
    @Override public boolean keyPressed(int key,int scan,int mods){invalidateDraftView();
        if(session.pending())return true;
        if(definitionOptions.keyPressed(key,height))return true;
        if(hasControlDown()&&key==GLFW.GLFW_KEY_S){saveDraft();return true;}
        return super.keyPressed(key,scan,mods);
    }
}
