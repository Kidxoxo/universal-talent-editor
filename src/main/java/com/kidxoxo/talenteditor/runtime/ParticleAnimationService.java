package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.SpawnParticlesAction;
import com.kidxoxo.talenteditor.domain.ParticleTint;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.ParticleWavePacket;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server owned, bounded animations; vanilla particle packets make modded simple particles available to nearby clients. */
@Mod.EventBusSubscriber(modid = TalentEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ParticleAnimationService {
    private static final ArrayList<Animation> ACTIVE = new ArrayList<>();
    private static final Map<Key, Long> LAST_STARTED = new HashMap<>();
    private static final Map<UUID, Budget> PER_TICK = new HashMap<>();

    private ParticleAnimationService() {}

    public static boolean start(ServerPlayer player, SpawnParticlesAction action) {
        if (!(ForgeRegistries.PARTICLE_TYPES.getValue(action.particle()) instanceof SimpleParticleType particle)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Key key = new Key(player.getUUID(), action);
        if (now - LAST_STARTED.getOrDefault(key, Long.MIN_VALUE / 2) < action.cooldownTicks()) return false;
        if (ACTIVE.stream().filter(animation -> animation.player == player).count() >= 8 || ACTIVE.size() >= 128) {
            return false;
        }
        if (!emit(player, particle, action, 0)) return false;
        LAST_STARTED.put(key, now);
        if (action.durationTicks() > 1) ACTIVE.add(new Animation(player, particle, action, now));
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Animation> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            ServerPlayer player = animation.player;
            if (player.isRemoved()) { iterator.remove(); continue; }
            long age = player.serverLevel().getGameTime() - animation.started;
            if (age >= animation.action.durationTicks() || age < 0) { iterator.remove(); continue; }
            if (age > 0 && age % animation.action.intervalTicks() == 0) {
                emit(player, animation.particle, animation.action, age);
            }
        }
        if (LAST_STARTED.size() > 4096) LAST_STARTED.clear();
        if (PER_TICK.size() > 4096) PER_TICK.clear();
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        ACTIVE.clear();
        LAST_STARTED.clear();
        PER_TICK.clear();
    }

    private static boolean emit(ServerPlayer player, SimpleParticleType particle,
                                SpawnParticlesAction action, long age) {
        ServerLevel level = player.serverLevel();
        long tick = level.getGameTime();
        Budget budget = PER_TICK.computeIfAbsent(player.getUUID(), id -> new Budget());
        if (budget.tick != tick) { budget.tick = tick; budget.count = 0; }
        if (budget.count + action.count() > 128) return false;
        budget.count += action.count();
        double y = player.getY() + switch (action.anchor()) {
            case "feet" -> 0.15;
            case "head" -> player.getBbHeight() * 0.9;
            default -> player.getBbHeight() * 0.52;
        };
        double x = player.getX(), z = player.getZ();
        double rangeSquared = action.viewDistance() * action.viewDistance();
        if (action.alphaFrom() != 1 || action.alphaTo() != 1
                || action.colorFrom() >= 0 || action.colorTo() >= 0 || action.hueShiftDegrees() != 0
                || action.sizeFrom() != 1 || action.sizeTo() != 1
                || java.util.Set.of("spiral", "ring", "fountain").contains(action.motion())) {
            float progress = action.durationTicks() <= 1 ? 0
                    : (float) age / (action.durationTicks() - 1);
            float alpha = (float) (action.alphaFrom()
                    + (action.alphaTo() - action.alphaFrom()) * progress);
            int color = ParticleTint.at(action.colorFrom(), action.colorTo(), progress);
            float size = (float) (action.sizeFrom()
                    + (action.sizeTo() - action.sizeFrom()) * progress);
            ParticleWavePacket wave = new ParticleWavePacket(action.particle(), action.motion(),
                    x, y, z, action.count(), action.radius(), action.speed(), alpha, (int) age,
                    player.getRandom().nextLong(), color, action.hueShiftDegrees(), size);
            for (ServerPlayer viewer : level.players()) {
                if (viewer.distanceToSqr(x, y, z) <= rangeSquared) EditorNetwork.sendParticleWave(viewer, wave);
            }
            return true;
        }
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(x, y, z) > rangeSquared) continue;
            if (action.motion().equals("burst")) {
                level.sendParticles(viewer, particle, action.viewDistance() > 32, x, y, z, action.count(),
                        action.radius(), action.radius() * 0.7, action.radius(), action.speed());
                continue;
            }
            RandomSource random = player.getRandom();
            for (int i = 0; i < action.count(); i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double height = (random.nextDouble() - 0.5) * action.radius();
                double distance = action.radius() * (0.5 + random.nextDouble() * 0.5);
                double dx = Math.cos(angle), dz = Math.sin(angle);
                boolean inward = action.motion().equals("inward");
                double px = x + (inward ? dx * distance : dx * 0.15);
                double py = y + height;
                double pz = z + (inward ? dz * distance : dz * 0.15);
                double velocity = action.speed() * (inward ? -1 : 1);
                level.sendParticles(viewer, particle, action.viewDistance() > 32, px, py, pz, 0,
                        dx * velocity, height / Math.max(action.radius(), 0.1) * velocity,
                        dz * velocity, 1.0);
            }
        }
        return true;
    }

    private record Key(UUID player, SpawnParticlesAction action) {}
    private record Animation(ServerPlayer player, SimpleParticleType particle,
                             SpawnParticlesAction action, long started) {}
    private static final class Budget { long tick = Long.MIN_VALUE; int count; }
}
