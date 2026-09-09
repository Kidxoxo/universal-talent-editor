package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.ActionTarget;
import com.kidxoxo.talenteditor.domain.AddEffectAction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TalentActiveStatusEffectServiceTest {
    @Test
    void clampsMaintainedEffectLeaseToAQuicklyExpiringWindow() {
        assertEquals(40, TalentActiveStatusEffectService.leaseTicks(action(1)));
        assertEquals(60, TalentActiveStatusEffectService.leaseTicks(action(60)));
        assertEquals(200, TalentActiveStatusEffectService.leaseTicks(action(72_000)));
    }

    private static AddEffectAction action(int duration) {
        return new AddEffectAction(new ResourceLocation("minecraft", "speed"), duration, 0,
                ActionTarget.SELF, true, false, true);
    }
}
