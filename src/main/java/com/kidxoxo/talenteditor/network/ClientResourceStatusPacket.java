package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.resource.ClientResourceRequirement;
import com.kidxoxo.talenteditor.resource.ClientResourceRequirementCatalog;
import com.kidxoxo.talenteditor.resource.ClientResourceStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ClientResourceStatusPacket(
        long revision,
        String contentHash,
        List<ClientResourceStatus> statuses
) {
    public ClientResourceStatusPacket {
        if (revision < 0L) throw new IllegalArgumentException("revision must not be negative");
        ClientResourceRequirementsPacket.validateHash(contentHash);
        statuses = List.copyOf(statuses);
        if (statuses.size() > ClientResourceRequirementCatalog.MAX_REQUIREMENTS) {
            throw new IllegalArgumentException("Too many client resource statuses");
        }
    }

    static void encode(ClientResourceStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.revision());
        buffer.writeUtf(packet.contentHash(), 64);
        buffer.writeVarInt(packet.statuses().size());
        packet.statuses().forEach(status -> {
            buffer.writeEnum(status.requirement().kind());
            buffer.writeResourceLocation(status.requirement().id());
            buffer.writeBoolean(status.available());
            buffer.writeUtf(status.sha256(), 64);
        });
    }

    static ClientResourceStatusPacket decode(FriendlyByteBuf buffer) {
        long revision = buffer.readVarLong();
        String contentHash = buffer.readUtf(64);
        int count = boundedCount(buffer.readVarInt());
        ArrayList<ClientResourceStatus> statuses = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ClientResourceRequirement requirement = new ClientResourceRequirement(
                    buffer.readEnum(ClientResourceRequirement.Kind.class),
                    buffer.readResourceLocation()
            );
            statuses.add(new ClientResourceStatus(
                    requirement, buffer.readBoolean(), buffer.readUtf(64)
            ));
        }
        return new ClientResourceStatusPacket(revision, contentHash, statuses);
    }

    static void handle(ClientResourceStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            var snapshot = TalentEditorMod.SNAPSHOTS.active();
            if (packet.revision() != snapshot.revision()
                    || !packet.contentHash().equals(snapshot.contentHash())) {
                context.setPacketHandled(true);
                return;
            }
            var catalog = ClientResourceRequirementCatalog.from(snapshot);
            TalentEditorMod.CLIENT_RESOURCES.accept(
                    player.getUUID(), packet.revision(), packet.contentHash(), packet.statuses(),
                    catalog.requirements()
            );
        }
        context.setPacketHandled(true);
    }

    public int availableCount() {
        return (int) statuses.stream().filter(ClientResourceStatus::available).count();
    }

    public int missingCount() {
        return statuses.size() - availableCount();
    }

    private static int boundedCount(int count) {
        if (count < 0 || count > ClientResourceRequirementCatalog.MAX_REQUIREMENTS) {
            throw new IllegalArgumentException("Invalid client resource status count: " + count);
        }
        return count;
    }
}
