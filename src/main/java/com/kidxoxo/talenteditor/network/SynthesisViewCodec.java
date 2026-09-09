package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;

final class SynthesisViewCodec {
    private static final long MAX_VALUE = 1_000_000_000_000L;

    private SynthesisViewCodec() {
    }

    static void encode(FriendlyByteBuf buffer, PlayerTalentState.SynthesisView synthesis) {
        TalentViewCodec.requireSize(
                synthesis.costs().size(), PlayerTalentStateFactory.MAX_SYNTHESIS_COSTS, "synthesis cost"
        );
        TalentViewCodec.encode(buffer, synthesis.target());
        buffer.writeBoolean(synthesis.conditionMet());
        buffer.writeBoolean(synthesis.ready());
        buffer.writeVarInt(synthesis.costs().size());
        for (PlayerTalentState.SynthesisCostView cost : synthesis.costs()) {
            if (cost.required() < 0L || cost.required() > MAX_VALUE
                    || cost.current() < 0L || cost.current() > MAX_VALUE) {
                throw new IllegalArgumentException("Synthesis cost value exceeds bounds");
            }
            buffer.writeEnum(cost.type());
            buffer.writeResourceLocation(cost.id());
            buffer.writeVarLong(cost.required());
            buffer.writeVarLong(cost.current());
        }
    }

    static PlayerTalentState.SynthesisView decode(FriendlyByteBuf buffer) {
        PlayerTalentState.TalentView target = TalentViewCodec.decode(buffer);
        boolean conditionMet = buffer.readBoolean();
        boolean ready = buffer.readBoolean();
        int count = TalentViewCodec.count(
                buffer.readVarInt(), PlayerTalentStateFactory.MAX_SYNTHESIS_COSTS, "synthesis cost"
        );
        ArrayList<PlayerTalentState.SynthesisCostView> costs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            PlayerTalentState.SynthesisCostType type = buffer.readEnum(PlayerTalentState.SynthesisCostType.class);
            net.minecraft.resources.ResourceLocation id = buffer.readResourceLocation();
            long required = buffer.readVarLong();
            long current = buffer.readVarLong();
            if (required < 0L || required > MAX_VALUE || current < 0L || current > MAX_VALUE) {
                throw new IllegalArgumentException("Invalid synthesis cost value");
            }
            costs.add(new PlayerTalentState.SynthesisCostView(type, id, required, current));
        }
        return new PlayerTalentState.SynthesisView(target, conditionMet, ready, costs);
    }
}
