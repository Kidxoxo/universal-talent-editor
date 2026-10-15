package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.editor.EditorSubmissionResult;
import com.kidxoxo.talenteditor.validation.ValidationIssue;
import com.kidxoxo.talenteditor.validation.ValidationSeverity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ValidationResultPacket(
        boolean accepted,
        boolean committed,
        boolean conflict,
        long activeRevision,
        String activeHash,
        List<ValidationIssue> issues,
        long requestId
) {
    private static final int MAX_ISSUES = 100;
    private static final int MAX_TEXT = 2_048;

    public ValidationResultPacket(boolean accepted, boolean committed, boolean conflict, long revision, String hash, List<ValidationIssue> issues) {
        this(accepted,committed,conflict,revision,hash,issues,0);
    }

    public ValidationResultPacket {
        issues = List.copyOf(issues);
    }

    static ValidationResultPacket from(EditorSubmissionResult result, long requestId) {
        List<ValidationIssue> issues = result.report().issues().stream().limit(MAX_ISSUES).toList();
        return new ValidationResultPacket(
                result.accepted(), result.committed(), result.conflict(),
                result.activeRevision(), result.activeHash(), issues, requestId
        );
    }

    static void encode(ValidationResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.requestId());
        buffer.writeBoolean(packet.accepted());
        buffer.writeBoolean(packet.committed());
        buffer.writeBoolean(packet.conflict());
        buffer.writeVarLong(packet.activeRevision());
        buffer.writeUtf(packet.activeHash(), 64);
        buffer.writeVarInt(packet.issues().size());
        for (ValidationIssue issue : packet.issues()) {
            buffer.writeEnum(issue.severity());
            buffer.writeUtf(issue.code(), MAX_TEXT);
            buffer.writeUtf(issue.source(), MAX_TEXT);
            buffer.writeUtf(issue.path(), MAX_TEXT);
            buffer.writeUtf(issue.message(), MAX_TEXT);
        }
    }

    static ValidationResultPacket decode(FriendlyByteBuf buffer) {
        long requestId = buffer.readVarLong();
        boolean accepted = buffer.readBoolean();
        boolean committed = buffer.readBoolean();
        boolean conflict = buffer.readBoolean();
        long revision = buffer.readVarLong();
        String hash = buffer.readUtf(64);
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ISSUES) {
            throw new IllegalArgumentException("Invalid validation issue count: " + count);
        }
        ArrayList<ValidationIssue> issues = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            issues.add(new ValidationIssue(
                    buffer.readEnum(ValidationSeverity.class),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT)
            ));
        }
        return new ValidationResultPacket(accepted, committed, conflict, revision, hash, issues, requestId);
    }

    static void handle(ValidationResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptValidation(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
