package com.kidxoxo.talenteditor.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleColorAccessor {
    @Accessor("rCol") float talentEditor$getRed();
    @Accessor("gCol") float talentEditor$getGreen();
    @Accessor("bCol") float talentEditor$getBlue();
}
