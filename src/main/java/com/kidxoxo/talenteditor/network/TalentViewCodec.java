package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

final class TalentViewCodec {
    private static final int MAX_TEXT = PlayerTalentStateFactory.MAX_TEXT_CHARACTERS;
    private static final int MAX_OPERATION = 32;
    private static final int MAX_FORMULA = 512;

    private TalentViewCodec() {
    }

    static void encode(FriendlyByteBuf buffer, PlayerTalentState.TalentView talent) {
        requireSize(talent.description().size(), PlayerTalentStateFactory.MAX_DESCRIPTIONS, "description");
        requireSize(talent.attributeEffects().size(), PlayerTalentStateFactory.MAX_EFFECTS, "attribute effect");
        buffer.writeResourceLocation(talent.id());
        encodeText(buffer, talent.name());
        buffer.writeResourceLocation(talent.rarity());
        encodeText(buffer, talent.rarityName());
        buffer.writeInt(talent.rarityColor());
        buffer.writeBoolean(talent.icon().isPresent());
        talent.icon().ifPresent(buffer::writeResourceLocation);
        buffer.writeBoolean(talent.defined());
        buffer.writeBoolean(talent.available());
        buffer.writeBoolean(talent.showGeneratedEffects());
        buffer.writeVarInt(talent.description().size());
        for (PlayerTalentState.StyledTextView line : talent.description()) encodeText(buffer, line);
        buffer.writeVarInt(talent.attributeEffects().size());
        for (PlayerTalentState.AttributeEffectView effect : talent.attributeEffects()) {
            buffer.writeResourceLocation(effect.attribute());
            buffer.writeDouble(effect.amount());
            buffer.writeUtf(effect.operation(), MAX_OPERATION);
            buffer.writeBoolean(effect.showInDescription());
            buffer.writeUtf(effect.formula(), MAX_FORMULA);
        }
    }

    static PlayerTalentState.TalentView decode(FriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        PlayerTalentState.StyledTextView name = decodeText(buffer);
        ResourceLocation rarity = buffer.readResourceLocation();
        PlayerTalentState.StyledTextView rarityName = decodeText(buffer);
        int rarityColor = buffer.readInt();
        java.util.Optional<ResourceLocation> icon = buffer.readBoolean()
                ? java.util.Optional.of(buffer.readResourceLocation()) : java.util.Optional.empty();
        boolean defined = buffer.readBoolean();
        boolean available = buffer.readBoolean();
        boolean showGeneratedEffects = buffer.readBoolean();
        int descriptionCount = count(
                buffer.readVarInt(), PlayerTalentStateFactory.MAX_DESCRIPTIONS, "description"
        );
        ArrayList<PlayerTalentState.StyledTextView> description = new ArrayList<>(descriptionCount);
        for (int index = 0; index < descriptionCount; index++) description.add(decodeText(buffer));
        int effectCount = count(buffer.readVarInt(), PlayerTalentStateFactory.MAX_EFFECTS, "attribute effect");
        ArrayList<PlayerTalentState.AttributeEffectView> effects = new ArrayList<>(effectCount);
        for (int index = 0; index < effectCount; index++) {
            effects.add(new PlayerTalentState.AttributeEffectView(
                    buffer.readResourceLocation(), buffer.readDouble(),
                    buffer.readUtf(MAX_OPERATION), buffer.readBoolean(), buffer.readUtf(MAX_FORMULA)
            ));
        }
        return new PlayerTalentState.TalentView(
                id, name, rarity, rarityName, rarityColor, icon, defined, available,
                showGeneratedEffects, description, effects
        );
    }

    static int count(int value, int maximum, String type) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("Invalid " + type + " count: " + value);
        return value;
    }

    static void requireSize(int value, int maximum, String type) {
        if (value > maximum) throw new IllegalArgumentException(type + " count exceeds " + maximum);
    }

    static void encodeText(FriendlyByteBuf buffer, PlayerTalentState.StyledTextView text) {
        buffer.writeUtf(text.literal(), MAX_TEXT);
        buffer.writeUtf(text.translation(), MAX_TEXT);
        buffer.writeBoolean(text.color() != null);
        if (text.color() != null) buffer.writeInt(text.color());
        buffer.writeBoolean(text.bold());
        buffer.writeBoolean(text.italic());
    }

    static PlayerTalentState.StyledTextView decodeText(FriendlyByteBuf buffer) {
        String literal = buffer.readUtf(MAX_TEXT);
        String translation = buffer.readUtf(MAX_TEXT);
        Integer color = buffer.readBoolean() ? buffer.readInt() : null;
        return new PlayerTalentState.StyledTextView(
                literal, translation, color, buffer.readBoolean(), buffer.readBoolean()
        );
    }
}
