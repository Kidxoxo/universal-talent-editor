package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.compile.UiDocumentCodec;
import com.kidxoxo.talenteditor.domain.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UiLayoutPacketTest {
    private TalentBookLayoutDefinition layout(){
        var ui=UiDocumentCodec.read("{\"elements\":[{\"id\":\"s\",\"type\":\"slot\",\"opacity\":0.35}]}",false);
        var draw=new com.google.gson.JsonObject();var offers=new com.google.gson.JsonArray();draw.add("elements",offers);
        for(int i=0;i<5;i++)offers.add(com.google.gson.JsonParser.parseString("{\"id\":\"o"+i+"\",\"type\":\"offer\",\"index\":"+i+"}"));
        var d=TalentBookLayoutDefinition.DEFAULT;
        return new TalentBookLayoutDefinition(d.id(),2,true,0,Optional.empty(),60,2,3,120,56,48,true,List.of(),List.of(),Optional.of(ui),Optional.of(UiDocumentCodec.read(draw.toString(),true)));
    }
    @Test void playerStateRoundTripsCustomAndLegacyLayouts(){
        for(var layout:List.of(layout(),TalentBookLayoutDefinition.DEFAULT)){
            var sent=new PlayerTalentStatePacket(new PlayerTalentState(12,3,false,List.of()),layout,true);
            FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
            try{PlayerTalentStatePacket.encode(sent,buffer);assertEquals(sent,PlayerTalentStatePacket.decode(buffer));assertEquals(0,buffer.readableBytes());}
            finally{buffer.release();}
        }
    }
    @Test void directDrawCarriesLayoutWithoutOpeningBookFirst(){
        var view=new DrawSessionView(UUID.randomUUID(),12,new ResourceLocation("talent_editor:default"),"Pool",3,1,List.of());
        var sent=new DrawSessionPacket(view,layout());var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{DrawSessionPacket.encode(sent,buffer);assertEquals(sent,DrawSessionPacket.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
    }
    @Test void networkRejectsInvalidUiInsteadOfPassingItToRenderer(){
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{buffer.writeBoolean(true);buffer.writeUtf("{\"elements\":[{\"id\":\"s\",\"type\":\"slot\",\"width\":0}]}");
            assertThrows(IllegalArgumentException.class,()->PlayerTalentStatePacket.decodeUi(buffer,false));}finally{buffer.release();}
    }
}
