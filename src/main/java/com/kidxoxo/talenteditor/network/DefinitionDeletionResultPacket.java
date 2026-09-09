package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import com.kidxoxo.talenteditor.domain.DefinitionTarget;
import com.kidxoxo.talenteditor.editor.DefinitionDeletionResult;
import com.kidxoxo.talenteditor.editor.DefinitionDeletionStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record DefinitionDeletionResultPacket(
        DefinitionDeletionStatus status,
        DefinitionKind kind,
        ResourceLocation id,
        long activeRevision,
        List<DefinitionReference> blockingReferences
) {
    private static final int MAX_REFERENCES = 100;
    private static final int MAX_USAGE = 64;
    private static final int MAX_PATH = 512;

    public DefinitionDeletionResultPacket {
        blockingReferences = List.copyOf(blockingReferences);
    }

    static DefinitionDeletionResultPacket from(DefinitionDeletionResult result) {
        return new DefinitionDeletionResultPacket(
                result.status(), result.kind(), result.id(), result.activeRevision(),
                result.blockingReferences().stream().limit(MAX_REFERENCES).toList()
        );
    }

    static void encode(DefinitionDeletionResultPacket packet, FriendlyByteBuf buffer) {
        if (packet.blockingReferences().size() > MAX_REFERENCES) {
            throw new IllegalArgumentException("Too many blocking references");
        }
        buffer.writeEnum(packet.status());
        buffer.writeEnum(packet.kind());
        buffer.writeResourceLocation(packet.id());
        buffer.writeVarLong(packet.activeRevision());
        buffer.writeVarInt(packet.blockingReferences().size());
        for (DefinitionReference reference : packet.blockingReferences()) {
            buffer.writeEnum(reference.target().kind());
            buffer.writeResourceLocation(reference.target().id());
            buffer.writeEnum(reference.sourceKind());
            buffer.writeResourceLocation(reference.sourceId());
            buffer.writeUtf(reference.usage(), MAX_USAGE);
            buffer.writeUtf(reference.path(), MAX_PATH);
        }
    }

    static DefinitionDeletionResultPacket decode(FriendlyByteBuf buffer) {
        DefinitionDeletionStatus status = buffer.readEnum(DefinitionDeletionStatus.class);
        DefinitionKind kind = buffer.readEnum(DefinitionKind.class);
        ResourceLocation id = buffer.readResourceLocation();
        long revision = buffer.readVarLong();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_REFERENCES) {
            throw new IllegalArgumentException("Invalid blocking reference count: " + count);
        }
        ArrayList<DefinitionReference> references = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            DefinitionKind targetKind = buffer.readEnum(DefinitionKind.class);
            ResourceLocation targetId = buffer.readResourceLocation();
            references.add(new DefinitionReference(
                    new DefinitionTarget(targetKind, targetId),
                    buffer.readEnum(DefinitionKind.class), buffer.readResourceLocation(),
                    buffer.readUtf(MAX_USAGE), buffer.readUtf(MAX_PATH)
            ));
        }
        return new DefinitionDeletionResultPacket(status, kind, id, revision, references);
    }

    static void handle(
            DefinitionDeletionResultPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptDeletionResult(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
