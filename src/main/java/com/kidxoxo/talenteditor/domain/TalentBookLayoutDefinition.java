package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.List;

/**
 * Server-authored presentation settings for the player talent book.
 *
 * <p>The server selects one enabled layout and sends it with the player state, so dedicated-server
 * clients render the same page geometry without reading server data-pack files directly.</p>
 */
public record TalentBookLayoutDefinition(
        ResourceLocation id,
        int schema,
        boolean enabled,
        int priority,
        Optional<ResourceLocation> background,
        int panelPercent,
        int columns,
        int rows,
        int slotWidth,
        int slotHeight,
        int iconSize,
        boolean showGeneratedEffects,
        List<SlotDefinition> slots,
        List<PageDefinition> pages
) {
    public static final ResourceLocation DEFAULT_ID = new ResourceLocation("talent_editor", "default");
    public static final TalentBookLayoutDefinition DEFAULT = new TalentBookLayoutDefinition(
            DEFAULT_ID, 2, true, 0, Optional.empty(), 60, 2, 3, 120, 56, 48, true,
            List.of(), List.of()
    );

    public TalentBookLayoutDefinition {
        background = background == null ? Optional.empty() : background;
        slots = slots == null ? List.of() : List.copyOf(slots);
        pages = pages == null ? List.of() : List.copyOf(pages);
    }

    public TalentBookLayoutDefinition(
            ResourceLocation id,
            int schema,
            boolean enabled,
            int priority,
            Optional<ResourceLocation> background,
            int panelPercent,
            int columns,
            int rows,
            int slotWidth,
            int slotHeight,
            int iconSize,
            boolean showGeneratedEffects
    ) {
        this(id, schema, enabled, priority, background, panelPercent, columns, rows,
                slotWidth, slotHeight, iconSize, showGeneratedEffects, List.of(), List.of());
    }

    public TalentBookLayoutDefinition(
            ResourceLocation id,
            int schema,
            boolean enabled,
            int priority,
            Optional<ResourceLocation> background,
            int panelPercent,
            int columns,
            int rows,
            int slotWidth,
            int slotHeight,
            int iconSize,
            boolean showGeneratedEffects,
            List<SlotDefinition> slots
    ) {
        this(id, schema, enabled, priority, background, panelPercent, columns, rows,
                slotWidth, slotHeight, iconSize, showGeneratedEffects, slots, List.of());
    }

    public int pageCapacity() {
        return slots.isEmpty() ? Math.multiplyExact(columns, rows) : slots.size();
    }

    public List<SlotDefinition> slotsForPage(int pageIndex) {
        return pageIndex >= 0 && pageIndex < pages.size() ? pages.get(pageIndex).slots() : slots;
    }

    public Optional<ResourceLocation> backgroundForPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            Optional<ResourceLocation> pageBackground = pages.get(pageIndex).background();
            if (pageBackground.isPresent()) return pageBackground;
        }
        return background;
    }

    public record SlotDefinition(
            String id,
            int x,
            int y,
            int width,
            int height,
            int iconSize,
            Optional<ResourceLocation> talent
    ) {
        public SlotDefinition {
            talent = talent == null ? Optional.empty() : talent;
        }

        public SlotDefinition(String id, int x, int y, int width, int height, int iconSize) {
            this(id, x, y, width, height, iconSize, Optional.empty());
        }
    }

    public record PageDefinition(
            String id,
            Optional<ResourceLocation> background,
            List<SlotDefinition> slots
    ) {
        public PageDefinition {
            background = background == null ? Optional.empty() : background;
            slots = slots == null ? List.of() : List.copyOf(slots);
        }
    }
}
