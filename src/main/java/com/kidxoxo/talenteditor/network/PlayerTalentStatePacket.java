package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public record PlayerTalentStatePacket(
        PlayerTalentState state,
        TalentBookLayoutDefinition layout,
        boolean openScreen
) {
    private static final int MAX_TALENTS = PlayerTalentStateFactory.MAX_TALENTS;
    private static final int MAX_DESCRIPTIONS = PlayerTalentStateFactory.MAX_DESCRIPTIONS;
    private static final int MAX_EFFECTS = PlayerTalentStateFactory.MAX_EFFECTS;
    private static final int MAX_TEXT = PlayerTalentStateFactory.MAX_TEXT_CHARACTERS;
    private static final int MAX_OPERATION = 32;
    private static final int MAX_LAYOUT_SLOTS = 256;
    private static final int MAX_LAYOUT_PAGES = 64;
    private static final int MAX_LAYOUT_TOTAL_PAGE_SLOTS = 4_096;
    private static final int MAX_HUD_RESOURCES = 32;

    public PlayerTalentStatePacket(PlayerTalentState state, boolean openScreen) {
        this(state, TalentBookLayoutDefinition.DEFAULT, openScreen);
    }

    static void encode(PlayerTalentStatePacket packet, FriendlyByteBuf buffer) {
        PlayerTalentState state = packet.state();
        requireSize(state.talents().size(), MAX_TALENTS, "talent");
        buffer.writeVarLong(state.definitionRevision());
        buffer.writeVarInt(state.drawTokens());
        buffer.writeBoolean(state.truncated());
        buffer.writeVarInt(state.talents().size());
        for (PlayerTalentState.TalentView talent : state.talents()) {
            TalentViewCodec.encode(buffer, talent);
        }
        buffer.writeBoolean(state.synthesesTruncated());
        requireSize(state.syntheses().size(), PlayerTalentStateFactory.MAX_SYNTHESES, "synthesis");
        buffer.writeVarInt(state.syntheses().size());
        for (PlayerTalentState.SynthesisView synthesis : state.syntheses()) {
            SynthesisViewCodec.encode(buffer, synthesis);
        }
        buffer.writeBoolean(state.countersTruncated());
        requireSize(state.counters().size(), PlayerTalentStateFactory.MAX_COUNTERS, "counter");
        buffer.writeVarInt(state.counters().size());
        for (PlayerTalentState.CounterProgressView counter : state.counters()) {
            encodeCounter(buffer, counter);
        }
        requireSize(state.hudResources().size(), MAX_HUD_RESOURCES, "HUD resource");
        buffer.writeVarInt(state.hudResources().size());
        for (PlayerTalentState.HudResourceView resource : state.hudResources()) {
            buffer.writeResourceLocation(resource.state());
            TalentViewCodec.encodeText(buffer, resource.name());
            buffer.writeDouble(resource.value());
            buffer.writeDouble(resource.maximum());
            buffer.writeVarInt(resource.color());
            buffer.writeVarInt(resource.order());
            buffer.writeBoolean(resource.showWhenFull());
        }
        buffer.writeDouble(state.render().alpha());
        buffer.writeDouble(state.render().scale());
        buffer.writeBoolean(state.render().hideNameplate());
        encodeLayout(buffer, packet.layout());
        buffer.writeBoolean(packet.openScreen());
    }

    static PlayerTalentStatePacket decode(FriendlyByteBuf buffer) {
        long revision = buffer.readVarLong();
        int drawTokens = buffer.readVarInt();
        if (drawTokens < 0) throw new IllegalArgumentException("Invalid draw token count: " + drawTokens);
        boolean truncated = buffer.readBoolean();
        int talentCount = count(buffer.readVarInt(), MAX_TALENTS, "talent");
        ArrayList<PlayerTalentState.TalentView> talents = new ArrayList<>(talentCount);
        for (int talentIndex = 0; talentIndex < talentCount; talentIndex++) {
            talents.add(TalentViewCodec.decode(buffer));
        }
        boolean synthesesTruncated = buffer.readBoolean();
        int synthesisCount = count(
                buffer.readVarInt(), PlayerTalentStateFactory.MAX_SYNTHESES, "synthesis"
        );
        ArrayList<PlayerTalentState.SynthesisView> syntheses = new ArrayList<>(synthesisCount);
        for (int index = 0; index < synthesisCount; index++) {
            syntheses.add(SynthesisViewCodec.decode(buffer));
        }
        boolean countersTruncated = buffer.readBoolean();
        int counterCount = count(buffer.readVarInt(), PlayerTalentStateFactory.MAX_COUNTERS, "counter");
        ArrayList<PlayerTalentState.CounterProgressView> counters = new ArrayList<>(counterCount);
        for (int index = 0; index < counterCount; index++) counters.add(decodeCounter(buffer));
        int hudCount = count(buffer.readVarInt(), MAX_HUD_RESOURCES, "HUD resource");
        ArrayList<PlayerTalentState.HudResourceView> hudResources = new ArrayList<>(hudCount);
        for (int index = 0; index < hudCount; index++) {
            hudResources.add(new PlayerTalentState.HudResourceView(
                    buffer.readResourceLocation(), TalentViewCodec.decodeText(buffer),
                    buffer.readDouble(), buffer.readDouble(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()
            ));
        }
        PlayerTalentState.RenderView render = new PlayerTalentState.RenderView(
                buffer.readDouble(), buffer.readDouble(), buffer.readBoolean()
        );
        TalentBookLayoutDefinition layout = decodeLayout(buffer);
        boolean openScreen = buffer.readBoolean();
        return new PlayerTalentStatePacket(new PlayerTalentState(
                revision, drawTokens, truncated, talents, synthesesTruncated, syntheses,
                countersTruncated, counters, hudResources, render
        ), layout, openScreen);
    }

    static void handle(PlayerTalentStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptPlayerTalentState(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }

    private static int count(int value, int maximum, String type) {
        return TalentViewCodec.count(value, maximum, type);
    }

    private static void requireSize(int value, int maximum, String type) {
        TalentViewCodec.requireSize(value, maximum, type);
    }

    private static void encodeCounter(FriendlyByteBuf buffer, PlayerTalentState.CounterProgressView counter) {
        if (counter.current() < 0L || counter.current() > com.kidxoxo.talenteditor.player.PlayerTalentData.MAX_COUNTER_VALUE
                || counter.maximum() < 0L
                || counter.maximum() > com.kidxoxo.talenteditor.player.PlayerTalentData.MAX_COUNTER_VALUE) {
            throw new IllegalArgumentException("Counter progress value exceeds bounds");
        }
        buffer.writeResourceLocation(counter.id());
        TalentViewCodec.encodeText(buffer, counter.name());
        buffer.writeUtf(counter.event(), 32);
        buffer.writeVarLong(counter.current());
        buffer.writeVarLong(counter.maximum());
        buffer.writeBoolean(counter.defined());
        buffer.writeBoolean(counter.active());
    }

    private static PlayerTalentState.CounterProgressView decodeCounter(FriendlyByteBuf buffer) {
        net.minecraft.resources.ResourceLocation id = buffer.readResourceLocation();
        PlayerTalentState.StyledTextView name = TalentViewCodec.decodeText(buffer);
        String event = buffer.readUtf(32);
        long current = buffer.readVarLong();
        long maximum = buffer.readVarLong();
        if (current < 0L || current > com.kidxoxo.talenteditor.player.PlayerTalentData.MAX_COUNTER_VALUE
                || maximum < 0L || maximum > com.kidxoxo.talenteditor.player.PlayerTalentData.MAX_COUNTER_VALUE) {
            throw new IllegalArgumentException("Invalid counter progress value");
        }
        return new PlayerTalentState.CounterProgressView(
                id, name, event, current, maximum, buffer.readBoolean(), buffer.readBoolean()
        );
    }

    private static void encodeLayout(FriendlyByteBuf buffer, TalentBookLayoutDefinition layout) {
        buffer.writeResourceLocation(layout.id());
        buffer.writeVarInt(layout.schema());
        buffer.writeBoolean(layout.enabled());
        buffer.writeVarInt(layout.priority());
        buffer.writeBoolean(layout.background().isPresent());
        layout.background().ifPresent(buffer::writeResourceLocation);
        buffer.writeVarInt(layout.panelPercent());
        buffer.writeVarInt(layout.columns());
        buffer.writeVarInt(layout.rows());
        buffer.writeVarInt(layout.slotWidth());
        buffer.writeVarInt(layout.slotHeight());
        buffer.writeVarInt(layout.iconSize());
        buffer.writeBoolean(layout.showGeneratedEffects());
        requireSize(layout.slots().size(), MAX_LAYOUT_SLOTS, "layout slot");
        buffer.writeVarInt(layout.slots().size());
        for (TalentBookLayoutDefinition.SlotDefinition slot : layout.slots()) {
            encodeLayoutSlot(buffer, slot);
        }
        requireSize(layout.pages().size(), MAX_LAYOUT_PAGES, "layout page");
        int totalPageSlots = layout.slots().size()
                + layout.pages().stream().mapToInt(page -> page.slots().size()).sum();
        requireSize(totalPageSlots, MAX_LAYOUT_TOTAL_PAGE_SLOTS, "layout configured page slot");
        buffer.writeVarInt(layout.pages().size());
        for (TalentBookLayoutDefinition.PageDefinition page : layout.pages()) {
            buffer.writeUtf(page.id(), 64);
            buffer.writeBoolean(page.background().isPresent());
            page.background().ifPresent(buffer::writeResourceLocation);
            requireSize(page.slots().size(), MAX_LAYOUT_SLOTS, "layout page slot");
            buffer.writeVarInt(page.slots().size());
            for (TalentBookLayoutDefinition.SlotDefinition slot : page.slots()) {
                encodeLayoutSlot(buffer, slot);
            }
        }
    }

    private static void encodeLayoutSlot(
            FriendlyByteBuf buffer,
            TalentBookLayoutDefinition.SlotDefinition slot
    ) {
        buffer.writeUtf(slot.id(), 64);
        buffer.writeVarInt(slot.x());
        buffer.writeVarInt(slot.y());
        buffer.writeVarInt(slot.width());
        buffer.writeVarInt(slot.height());
        buffer.writeVarInt(slot.iconSize());
        buffer.writeBoolean(slot.talent().isPresent());
        slot.talent().ifPresent(buffer::writeResourceLocation);
    }

    private static TalentBookLayoutDefinition decodeLayout(FriendlyByteBuf buffer) {
        net.minecraft.resources.ResourceLocation id = buffer.readResourceLocation();
        int schema = bounded(buffer.readVarInt(), 1, 2, "schema");
        boolean enabled = buffer.readBoolean();
        int priority = bounded(buffer.readVarInt(), -100_000, 100_000, "priority");
        java.util.Optional<net.minecraft.resources.ResourceLocation> background = buffer.readBoolean()
                ? java.util.Optional.of(buffer.readResourceLocation()) : java.util.Optional.empty();
        int panelPercent = bounded(buffer.readVarInt(), 25, 70, "panelPercent");
        int columns = bounded(buffer.readVarInt(), 1, 8, "columns");
        int rows = bounded(buffer.readVarInt(), 1, 10, "rows");
        int slotWidth = bounded(buffer.readVarInt(), 24, 128, "slotWidth");
        int slotHeight = bounded(buffer.readVarInt(), 22, 96, "slotHeight");
        int iconSize = bounded(buffer.readVarInt(), 8, 64, "iconSize");
        if (iconSize > slotHeight - 4) throw new IllegalArgumentException("Layout icon exceeds slot height");
        boolean showGeneratedEffects = buffer.readBoolean();
        int slotCount = count(buffer.readVarInt(), MAX_LAYOUT_SLOTS, "layout slot");
        java.util.ArrayList<TalentBookLayoutDefinition.SlotDefinition> slots = new java.util.ArrayList<>(slotCount);
        for (int index = 0; index < slotCount; index++) {
            var slot = decodeLayoutSlot(buffer, "root slot " + index);
            if (slot.talent().isPresent()) throw new IllegalArgumentException("Root slots cannot bind talents");
            slots.add(slot);
        }
        int pageCount = count(buffer.readVarInt(), MAX_LAYOUT_PAGES, "layout page");
        java.util.ArrayList<TalentBookLayoutDefinition.PageDefinition> pages = new java.util.ArrayList<>(pageCount);
        int totalPageSlots = slotCount;
        java.util.HashSet<net.minecraft.resources.ResourceLocation> fixedTalents = new java.util.HashSet<>();
        java.util.HashSet<String> pageIds = new java.util.HashSet<>();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            String pageId = buffer.readUtf(64);
            if (!pageId.matches("[a-z0-9_.-]{1,64}") || !pageIds.add(pageId)) {
                throw new IllegalArgumentException("Invalid or duplicate layout page id: " + pageId);
            }
            java.util.Optional<net.minecraft.resources.ResourceLocation> pageBackground = buffer.readBoolean()
                    ? java.util.Optional.of(buffer.readResourceLocation()) : java.util.Optional.empty();
            int pageSlotCount = count(buffer.readVarInt(), MAX_LAYOUT_SLOTS, "layout page slot");
            if (pageSlotCount == 0) throw new IllegalArgumentException("Layout page has no slots: " + pageId);
            totalPageSlots += pageSlotCount;
            if (totalPageSlots > MAX_LAYOUT_TOTAL_PAGE_SLOTS) {
                throw new IllegalArgumentException("Layout configured page slot count exceeds bounds");
            }
            java.util.ArrayList<TalentBookLayoutDefinition.SlotDefinition> pageSlots =
                    new java.util.ArrayList<>(pageSlotCount);
            for (int slotIndex = 0; slotIndex < pageSlotCount; slotIndex++) {
                var slot = decodeLayoutSlot(buffer, "page " + pageId + " slot " + slotIndex);
                if (slot.talent().isPresent() && !fixedTalents.add(slot.talent().orElseThrow())) {
                    throw new IllegalArgumentException("Duplicate fixed talent in layout pages");
                }
                pageSlots.add(slot);
            }
            pages.add(new TalentBookLayoutDefinition.PageDefinition(pageId, pageBackground, pageSlots));
        }
        return new TalentBookLayoutDefinition(
                id, schema, enabled, priority, background, panelPercent, columns, rows, slotWidth, slotHeight,
                iconSize, showGeneratedEffects, slots, pages
        );
    }

    private static TalentBookLayoutDefinition.SlotDefinition decodeLayoutSlot(
            FriendlyByteBuf buffer,
            String label
    ) {
        String slotId = buffer.readUtf(64);
        int x = bounded(buffer.readVarInt(), 0, 2_048, "slot x");
        int y = bounded(buffer.readVarInt(), 0, 2_048, "slot y");
        int width = bounded(buffer.readVarInt(), 24, 256, "slot width");
        int height = bounded(buffer.readVarInt(), 22, 128, "slot height");
        int slotIcon = bounded(buffer.readVarInt(), 8, 64, "slot icon size");
        java.util.Optional<net.minecraft.resources.ResourceLocation> talent = buffer.readBoolean()
                ? java.util.Optional.of(buffer.readResourceLocation()) : java.util.Optional.empty();
        if (!slotId.matches("[a-z0-9_.-]{1,64}") || slotIcon > height - 4
                || x + width > 2_048 || y + height > 2_048) {
            throw new IllegalArgumentException("Invalid layout " + label);
        }
        return new TalentBookLayoutDefinition.SlotDefinition(slotId, x, y, width, height, slotIcon, talent);
    }

    private static int bounded(int value, int minimum, int maximum, String field) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Invalid layout " + field + ": " + value);
        }
        return value;
    }
}
