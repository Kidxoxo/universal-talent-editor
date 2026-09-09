package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.resource.ClientResourceRequirement;
import com.kidxoxo.talenteditor.resource.ClientResourceRequirementCatalog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ClientResourceRequirementsPacket(
        long revision,
        String contentHash,
        boolean truncated,
        List<ClientResourceRequirement> requirements
) {
    public ClientResourceRequirementsPacket {
        if (revision < 0L) throw new IllegalArgumentException("revision must not be negative");
        validateHash(contentHash);
        requirements = List.copyOf(requirements);
        if (requirements.size() > ClientResourceRequirementCatalog.MAX_REQUIREMENTS) {
            throw new IllegalArgumentException("Too many client resource requirements");
        }
    }

    static void encode(ClientResourceRequirementsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.revision());
        buffer.writeUtf(packet.contentHash(), 64);
        buffer.writeBoolean(packet.truncated());
        buffer.writeVarInt(packet.requirements().size());
        packet.requirements().forEach(requirement -> {
            buffer.writeEnum(requirement.kind());
            buffer.writeResourceLocation(requirement.id());
        });
    }

    static ClientResourceRequirementsPacket decode(FriendlyByteBuf buffer) {
        long revision = buffer.readVarLong();
        String contentHash = buffer.readUtf(64);
        boolean truncated = buffer.readBoolean();
        int count = boundedCount(buffer.readVarInt());
        ArrayList<ClientResourceRequirement> requirements = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            requirements.add(new ClientResourceRequirement(
                    buffer.readEnum(ClientResourceRequirement.Kind.class),
                    buffer.readResourceLocation()
            ));
        }
        return new ClientResourceRequirementsPacket(revision, contentHash, truncated, requirements);
    }

    static void handle(
            ClientResourceRequirementsPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptResourceRequirements(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }

    static void validateHash(String hash) {
        if (hash == null || !hash.matches("[0-9A-Fa-f]{64}")) {
            throw new IllegalArgumentException("Invalid content hash");
        }
    }

    private static int boundedCount(int count) {
        if (count < 0 || count > ClientResourceRequirementCatalog.MAX_REQUIREMENTS) {
            throw new IllegalArgumentException("Invalid client resource requirement count: " + count);
        }
        return count;
    }
}
