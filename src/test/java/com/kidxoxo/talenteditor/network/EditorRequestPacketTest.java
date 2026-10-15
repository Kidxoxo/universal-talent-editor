package com.kidxoxo.talenteditor.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EditorRequestPacketTest {
    @Test void allDefinitionSubmissionsKeepTheirRequestIdentity() {
        var id=new ResourceLocation("test:copy");var b=new FriendlyByteBuf(Unpooled.buffer());
        try {
            var talent=new SubmitTalentPacket(4,id,"{}",true,12345678901L);SubmitTalentPacket.encode(talent,b);assertEquals(talent,SubmitTalentPacket.decode(b));
            var rarity=new SubmitRarityPacket(4,id,"{}",false,12);SubmitRarityPacket.encode(rarity,b);assertEquals(rarity,SubmitRarityPacket.decode(b));
            var pool=new SubmitPoolPacket(4,id,"{}",true,13);SubmitPoolPacket.encode(pool,b);assertEquals(pool,SubmitPoolPacket.decode(b));
            var counter=new SubmitCounterPacket(4,id,"{}",true,14);SubmitCounterPacket.encode(counter,b);assertEquals(counter,SubmitCounterPacket.decode(b));
            var layout=new SubmitLayoutPacket(4,id,"{}",true,15);SubmitLayoutPacket.encode(layout,b);assertEquals(layout,SubmitLayoutPacket.decode(b));
            var item=new SubmitConfiguredItemPacket(4,id,"{}",true,16);SubmitConfiguredItemPacket.encode(item,b);assertEquals(item,SubmitConfiguredItemPacket.decode(b));
            assertEquals(0,b.readableBytes());
        } finally {b.release();}
    }
    @Test void successConflictAndValidationResultsKeepTheirRequestIdentity() {
        var b=new FriendlyByteBuf(Unpooled.buffer());
        try {for(boolean committed:List.of(true,false)) {
            var result=new ValidationResultPacket(committed,committed,!committed,5,"hash",List.of(),9001);
            ValidationResultPacket.encode(result,b);assertEquals(result,ValidationResultPacket.decode(b));
        }}finally{b.release();}
    }
}
