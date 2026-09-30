package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.editor.EditorPackStorage;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.ZipFile;

/** One-time migration through the same draft and storage layers used by the in-game editor. */
public final class ConfiguredItemWorldMigration {
    private static final List<String> ITEMS = List.of(
            "fallen_feather", "gugu_friend", "gugu_sword", "night_feather", "stone_mask"
    );

    private ConfiguredItemWorldMigration() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("source, staged datapacks root, and optional item name required");
        }
        Path source = Path.of(args[0]);
        Path datapacksRoot = Path.of(args[1]);
        EditorPackStorage storage = new EditorPackStorage();
        List<String> names = args.length == 3 ? List.of(args[2]) : ITEMS;
        for (String name : names) {
                if (!ITEMS.contains(name)) throw new IllegalArgumentException("Unknown item: " + name);
                String original;
                if (Files.isDirectory(source)) {
                    original = Files.readString(source.resolve(name + ".json"), StandardCharsets.UTF_8);
                } else {
                    String entry = "data/ripples_talents/talent_editor/items/" + name + ".json";
                    try (ZipFile zip = new ZipFile(source.toFile())) {
                        var sourceEntry = zip.getEntry(entry);
                        if (sourceEntry == null) throw new IllegalStateException("Missing " + entry);
                        original = new String(zip.getInputStream(sourceEntry).readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                ConfiguredItemDraft draft = ConfiguredItemDraft.parse(original);
                if (name.equals("gugu_friend") && args.length == 3) {
                    mergeFriendSlots(draft);
                }
                draft.replaceTriggers(draft.triggers());
                List<String> requiredMods = Arrays.stream(draft.requiredMods().split(","))
                        .map(String::strip).filter(value -> !value.isEmpty()).toList();
                boolean sword = name.equals("gugu_sword");
                JsonObject definition = JsonParser.parseString(draft.toJson(
                        draft.name(), draft.descriptionEditorText(), draft.enabled(), draft.customModelData(),
                        draft.glint(), !sword, sword, draft.curiosSlots(), requiredMods
                )).getAsJsonObject();
                ResourceLocation id = new ResourceLocation("ripples_talents", name);
                Path output = storage.writeConfiguredItem(datapacksRoot, id, definition);
                System.out.println(id + " -> " + output);
        }
    }

    private static void mergeFriendSlots(ConfiguredItemDraft draft) {
        var originals = draft.attributeModifiers();
        if (originals.size() != 9) throw new IllegalStateException("Expected nine friend modifiers");
        LinkedHashMap<String, ArrayList<ConfiguredItemDraft.AttributeModifierDraft>> groups =
                new LinkedHashMap<>();
        for (var modifier : originals) {
            String identity = modifier.attribute() + "|" + modifier.amount() + "|" + modifier.operation();
            groups.computeIfAbsent(identity, ignored -> new ArrayList<>()).add(modifier);
        }
        if (groups.size() != 3) throw new IllegalStateException("Expected three friend attributes");
        ArrayList<ConfiguredItemDraft.AttributeModifierDraft> merged = new ArrayList<>();
        for (var group : groups.values()) {
            LinkedHashSet<String> slots = new LinkedHashSet<>();
            group.forEach(modifier -> slots.addAll(modifier.slots()));
            if (group.size() != 3 || !slots.equals(new LinkedHashSet<>(
                    List.of("curios", "mainhand", "offhand")))) {
                throw new IllegalStateException("Unexpected friend slot group");
            }
            var first = group.get(0);
            String key = first.modifierKey().replaceFirst("_curios$", "");
            merged.add(first.withValues(ResourceLocation.parse(first.attribute()), first.amount(),
                    first.operation(), List.copyOf(slots), key));
        }
        draft.replaceAttributeModifiers(merged);
    }
}
