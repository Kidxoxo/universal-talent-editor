package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.*;
import com.kidxoxo.talenteditor.player.PlayerTalentData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TriggerSourceIsolationTest {
    private static final ResourceLocation ID=new ResourceLocation("test:shared");
    private static final CompiledTrigger TRIGGER=new CompiledTrigger("same",TalentEventTypes.PLAYER_SECOND,0,
            new CompiledCondition.Always(true),List.of());
    private final TriggerBinding talent=new TriggerBinding(ID,TRIGGER);
    private final TriggerBinding item=new TriggerBinding(DefinitionKind.ITEM,ID,TRIGGER);

    @Test void selectsOnlyTheOwnedSourceWhenIdsOverlap() {
        var both=List.of(talent,item);
        assertEquals(List.of(talent),TalentTriggerEngine.ownedBindings(both,Set.of(ID),Set.of()));
        assertEquals(List.of(item),TalentTriggerEngine.ownedBindings(both,Set.of(),Set.of(ID)));
        assertEquals(2,TalentTriggerEngine.ownedBindings(both,Set.of(ID),Set.of(ID)).size());
        assertTrue(TalentTriggerEngine.ownedBindings(both,Set.of(),Set.of()).isEmpty());
        assertNotEquals(talent.stateKey(),item.stateKey());
        assertEquals("test:shared#same",talent.stateKey());
    }

    @Test void independentStateSurvivesNbtAndTalentRemoval() {
        PlayerTalentData data=new PlayerTalentData();data.grant(ID);
        data.setTriggerLastActivation(talent.stateKey(),20);data.setTriggerLastActivation(item.stateKey(),40);
        data.incrementTriggerActivation(talent.stateKey());data.incrementTriggerActivation(item.stateKey());
        data.claimTriggerDistanceInterval(item.stateKey(),2,10);
        PlayerTalentData restored=new PlayerTalentData();restored.deserializeNBT(data.serializeNBT());restored.revoke(ID);
        assertEquals(-1,restored.triggerLastActivation(talent.stateKey()));
        assertEquals(40,restored.triggerLastActivation(item.stateKey()));
        assertEquals(1,restored.triggerActivationCount(item.stateKey()));
        assertEquals(2,restored.triggerDistanceProgress(item.stateKey()));
    }

    @Test void migratesUnambiguousLegacyItemStateOnlyOnce() {
        PlayerTalentData data=new PlayerTalentData();
        data.setTriggerLastActivation(talent.stateKey(),30);data.incrementTriggerActivation(talent.stateKey());
        data.claimTriggerDistanceInterval(talent.stateKey(),3,10);
        data.migrateItemTriggerState(ID,true);
        assertEquals(30,data.triggerLastActivation(item.stateKey()));
        assertEquals(1,data.triggerActivationCount(item.stateKey()));
        assertEquals(3,data.triggerDistanceProgress(item.stateKey()));
        assertEquals(-1,data.triggerLastActivation(talent.stateKey()));
        data.setTriggerLastActivation(talent.stateKey(),60);data.migrateItemTriggerState(ID,true);
        assertEquals(30,data.triggerLastActivation(item.stateKey()));
        assertEquals(60,data.triggerLastActivation(talent.stateKey()));
    }

    @Test void ambiguousLegacyKeysRemainTalentOwned() {
        PlayerTalentData data=new PlayerTalentData();data.setTriggerLastActivation(talent.stateKey(),30);
        data.migrateItemTriggerState(ID,false);data.migrateItemTriggerState(ID,true);
        assertEquals(30,data.triggerLastActivation(talent.stateKey()));
        assertEquals(-1,data.triggerLastActivation(item.stateKey()));
    }

    @Test void unconditionalActionsNeverNeedAPlayerContext() {
        var action=new AttributeModifierAction(new ResourceLocation("minecraft:generic.armor"),2,
                AttributeOperation.ADDITION,"plain",true);
        assertTrue(ActionConditionRuntime.active(action,(net.minecraft.server.level.ServerPlayer)null,ID));
        assertTrue(ActionConditionRuntime.active(null,null,null,ID));
    }
}
