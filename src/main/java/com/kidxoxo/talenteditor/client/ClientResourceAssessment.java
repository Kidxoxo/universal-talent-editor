package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.ClientResourceRequirementsPacket;
import com.kidxoxo.talenteditor.network.ClientResourceStatusPacket;
import com.kidxoxo.talenteditor.resource.ClientResourceRequirement;
import com.kidxoxo.talenteditor.resource.ClientResourceStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Set;

/** Assesses the resources Minecraft has actually loaded after pack resolution. */
final class ClientResourceAssessment {
    private ClientResourceAssessment() {
    }

    static ClientResourceStatusPacket capture(
            ResourceManager resources,
            Set<ResourceLocation> availableSounds,
            ClientResourceRequirementsPacket packet
    ) {
        return assess(packet, requirement -> {
            if (requirement.kind() == ClientResourceRequirement.Kind.SOUND) {
                boolean available = availableSounds.contains(requirement.id());
                // A sound event may resolve to several weighted/streamed files, so there is no single
                // honest loaded-content hash to report. Presence is still useful and remains bounded.
                return new Observation(available, "");
            }
            var resource = resources.getResource(requirement.id());
            if (resource.isEmpty()) return new Observation(false, "");
            try (InputStream input = resource.orElseThrow().open()) {
                return new Observation(true, sha256(input));
            } catch (IOException | SecurityException exception) {
                return new Observation(false, "");
            }
        });
    }

    static ClientResourceStatusPacket assess(
            ClientResourceRequirementsPacket packet,
            Probe probe
    ) {
        ArrayList<ClientResourceStatus> statuses = new ArrayList<>(packet.requirements().size());
        packet.requirements().forEach(requirement -> {
            Observation observation;
            try {
                observation = probe.inspect(requirement);
            } catch (RuntimeException exception) {
                observation = new Observation(false, "");
            }
            statuses.add(new ClientResourceStatus(
                    requirement, observation.available(), observation.sha256()
            ));
        });
        return new ClientResourceStatusPacket(packet.revision(), packet.contentHash(), statuses);
    }

    private static String sha256(InputStream input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16_384];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().withUpperCase().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    @FunctionalInterface
    interface Probe {
        Observation inspect(ClientResourceRequirement requirement);
    }

    record Observation(boolean available, String sha256) {
        Observation {
            sha256 = sha256 == null ? "" : sha256;
            if (!available) sha256 = "";
        }
    }
}
