package com.kidxoxo.talenteditor.client;

import java.util.List;

/** Shared clock for several particle actions started by the same event. */
final class ParticlePreviewTimeline {
    private final List<Layer> layers;
    private final int durationTicks;

    ParticlePreviewTimeline(List<Layer> layers) {
        if (layers.isEmpty() || layers.size() > 8) {
            throw new IllegalArgumentException("Preview requires 1–8 particle actions");
        }
        int firstWave = layers.stream().mapToInt(Layer::count).sum();
        if (firstWave > 128) {
            throw new IllegalArgumentException("Combined first wave exceeds 128 particles");
        }
        this.layers = List.copyOf(layers);
        durationTicks = layers.stream().mapToInt(Layer::durationTicks).max().orElse(1);
    }

    int size() { return layers.size(); }
    int durationTicks() { return durationTicks; }
    int firstWaveCount() { return layers.stream().mapToInt(Layer::count).sum(); }
    boolean emitsAt(int layerIndex, int age) { return layers.get(layerIndex).emitsAt(age); }

    record Layer(int count, int durationTicks, int intervalTicks) {
        Layer {
            if (count < 1 || count > 32 || durationTicks < 1 || durationTicks > 60
                    || intervalTicks < 1 || intervalTicks > 20
                    || count * (1 + (durationTicks - 1) / intervalTicks) > 256) {
                throw new IllegalArgumentException("Invalid particle animation bounds");
            }
        }

        boolean emitsAt(int age) {
            return age >= 0 && age < durationTicks && age % intervalTicks == 0;
        }
    }
}
