package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.PlaySoundAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class PlaySoundActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "sound", "source", "volume", "pitch");

    @Override
    public ResourceLocation id() {
        return PlaySoundAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE,
                ActionPlacement.ON_SYNTHESIZE, ActionPlacement.ITEM_USE);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation sound = JsonAccess.requiredId(
                json, "sound", "minecraft:entity.player.levelup", true,
                context.source(), context.path(), context.report()
        );
        if (!context.environment().hasSoundEvent(sound)) {
            context.report().warning("UNREGISTERED_SOUND_EVENT", context.source(),
                    JsonAccess.child(context.path(), "sound"),
                    "Sound event is not registered on the server and will be sent as a direct client resource ID: "
                            + sound);
        }
        String rawSource = JsonAccess.string(
                json, "source", "players", false, context.source(), context.path(), context.report()
        );
        SoundSource source;
        try {
            source = SoundSource.valueOf(rawSource.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_SOUND_SOURCE", context.source(),
                    JsonAccess.child(context.path(), "source"),
                    "Expected master, music, records, weather, blocks, hostile, neutral, players, ambient, or voice");
            source = SoundSource.PLAYERS;
        }
        double volume = JsonAccess.decimal(
                json, "volume", 1.0D, 0.0D, 16.0D, context.source(), context.path(), context.report()
        );
        double pitch = JsonAccess.decimal(
                json, "pitch", 1.0D, 0.01D, 4.0D, context.source(), context.path(), context.report()
        );
        return Optional.of(new PlaySoundAction(sound, source, (float) volume, (float) pitch));
    }
}
