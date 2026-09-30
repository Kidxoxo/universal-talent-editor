package com.kidxoxo.talenteditor.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Particle.class)
public interface ParticleAlphaAccessor {
    @Invoker("setAlpha") void talentEditor$setAlpha(float alpha);
}
