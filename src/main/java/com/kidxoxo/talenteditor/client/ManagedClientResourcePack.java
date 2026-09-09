package com.kidxoxo.talenteditor.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Owns one generated, client-local resource pack and never writes outside its fixed root. */
final class ManagedClientResourcePack {
    static final String DIRECTORY_NAME = "talent_editor_generated";
    static final String PREFERRED_PACK_ID = "file/" + DIRECTORY_NAME;
    static final String MANIFEST_NAME = "talent_editor.manifest.json";
    private static final String ITEM_ICON_MANIFEST = "talent_editor.item_icons.json";
    private static final int MANIFEST_SCHEMA = 1;
    private static final int RESOURCE_PACK_FORMAT = 15;
    private static final long MAX_TEXTURE_BYTES = 16L * 1024L * 1024L;
    private static final long MAX_SOUND_BYTES = 64L * 1024L * 1024L;
    private static final int MAX_TEXTURE_EDGE = 8_192;
    private static final long MAX_TEXTURE_PIXELS = 67_108_864L;
    private static final byte[] OGG_HEADER = {'O', 'g', 'g', 'S'};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path root;

    ManagedClientResourcePack(Path gameDirectory) {
        this.root = gameDirectory.toAbsolutePath().normalize()
                .resolve("resourcepacks").resolve(DIRECTORY_NAME).normalize();
    }

    Path root() {
        return root;
    }

    ImportResult importFile(Path source, ResourceLocation resource, ImportKind kind, boolean stream) {
        if (source == null || resource == null || kind == null) {
            return ImportResult.failure(ImportCode.INVALID_TARGET, "");
        }
        String asset = assetPath(resource, kind);
        if (asset == null) return ImportResult.failure(ImportCode.INVALID_TARGET, resource.toString());
        Path normalizedSource = source.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedSource)) {
            return ImportResult.failure(ImportCode.SOURCE_MISSING, normalizedSource.toString());
        }

        long limit = kind == ImportKind.TEXTURE ? MAX_TEXTURE_BYTES : MAX_SOUND_BYTES;
        try {
            if (Files.size(normalizedSource) > limit) {
                return ImportResult.failure(ImportCode.SOURCE_TOO_LARGE, Long.toString(limit));
            }
            List<ManifestEntry> entries = readManifest();
            ManifestEntry owner = entries.stream().filter(entry -> entry.asset().equals(asset)).findFirst().orElse(null);
            if (owner != null && (!owner.resource().equals(resource.toString()) || owner.kind() != kind)) {
                return ImportResult.failure(ImportCode.TARGET_CONFLICT, owner.resource());
            }
            Path target = controlledTarget(asset);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && owner == null) {
                return ImportResult.failure(ImportCode.UNMANAGED_TARGET, asset);
            }
            JsonObject sounds = kind == ImportKind.SOUND ? readSounds(resource.getNamespace()) : null;
            boolean resourceManaged = entries.stream().anyMatch(entry -> entry.resource().equals(resource.toString()));
            if (kind == ImportKind.SOUND && sounds.has(resource.getPath()) && !resourceManaged) {
                return ImportResult.failure(ImportCode.UNMANAGED_TARGET,
                        "assets/" + resource.getNamespace() + "/sounds.json#" + resource.getPath());
            }
            ensureDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), ".talent-import-", ".tmp");
            try {
                Preparation preparation = prepareSource(normalizedSource, temporary, kind);
                if (!preparation.successful()) {
                    return ImportResult.failure(preparation.code(), preparation.detail());
                }
                long bytes = Files.size(temporary);
                if (bytes > limit) return ImportResult.failure(ImportCode.SOURCE_TOO_LARGE, Long.toString(limit));
                if (!validContent(temporary, kind)) {
                    return ImportResult.failure(ImportCode.INVALID_CONTENT, kind.name().toLowerCase());
                }
                String hash = sha256(temporary);
                ensurePackMetadata();
                if (kind == ImportKind.SOUND) {
                    String soundName = resource.getNamespace() + ":" + soundAssetPath(resource);
                    JsonObject definition = new JsonObject();
                    JsonArray choices = new JsonArray();
                    JsonObject choice = new JsonObject();
                    choice.addProperty("name", soundName);
                    if (stream) choice.addProperty("stream", true);
                    choices.add(choice);
                    definition.add("sounds", choices);
                    sounds.add(resource.getPath(), definition);
                    writeJson(controlledTarget("assets/" + resource.getNamespace() + "/sounds.json"), sounds);
                }
                ManifestEntry updated = new ManifestEntry(
                        kind, resource.toString(), asset, bytes, hash, kind == ImportKind.SOUND && stream
                );
                ArrayList<ManifestEntry> next = new ArrayList<>(entries);
                next.removeIf(entry -> entry.resource().equals(updated.resource()) || entry.asset().equals(asset));
                next.add(updated);
                writeManifest(next);
                // Move the content last: if this final step fails, the manifest owns the target and a retry
                // can repair it instead of treating a partially imported file as unmanaged user data.
                moveReplacing(temporary, target);
                return new ImportResult(ImportCode.IMPORTED, resource, asset, bytes, hash, "");
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (InvalidManifestException exception) {
            return ImportResult.failure(ImportCode.MANIFEST_INVALID, exception.getMessage());
        } catch (IOException | SecurityException exception) {
            return ImportResult.failure(ImportCode.IO_ERROR, safeMessage(exception));
        }
    }

    AuditResult audit() {
        try {
            List<ManifestEntry> entries = readManifest();
            int missing = 0;
            int changed = 0;
            for (ManifestEntry entry : entries) {
                Path target = controlledTarget(entry.asset());
                if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                    missing++;
                } else if (Files.size(target) != entry.bytes() || !sha256(target).equals(entry.sha256())) {
                    changed++;
                }
            }
            return new AuditResult(true, entries.size(), missing, changed, "");
        } catch (InvalidManifestException exception) {
            return new AuditResult(false, 0, 0, 0, exception.getMessage());
        } catch (IOException | SecurityException exception) {
            return new AuditResult(false, 0, 0, 0, safeMessage(exception));
        }
    }

    /** Returns only manifest entries whose current file still matches its recorded hash and size. */
    Map<ResourceLocation, String> verifiedHashes(ImportKind kind) {
        LinkedHashMap<ResourceLocation, String> verified = new LinkedHashMap<>();
        try {
            for (ManifestEntry entry : readManifest()) {
                if (entry.kind() != kind) continue;
                Path target = controlledTarget(entry.asset());
                if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                        || Files.size(target) != entry.bytes()
                        || !sha256(target).equals(entry.sha256())) continue;
                ResourceLocation id = ResourceLocation.tryParse(entry.resource());
                if (id != null) verified.put(id, entry.sha256());
            }
        } catch (InvalidManifestException | IOException | SecurityException ignored) {
            return Map.of();
        }
        return Map.copyOf(verified);
    }

    List<ResourceLocation> verifiedResources(ImportKind kind) {
        return verifiedHashes(kind).keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    /** Lightweight picker index: validates manifest ownership and file presence without rehashing large audio. */
    List<ResourceLocation> managedResources(ImportKind kind) {
        ArrayList<ResourceLocation> resources = new ArrayList<>();
        try {
            for (ManifestEntry entry : readManifest()) {
                if (entry.kind() != kind) continue;
                Path target = controlledTarget(entry.asset());
                ResourceLocation id = ResourceLocation.tryParse(entry.resource());
                if (id != null && Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) resources.add(id);
            }
        } catch (InvalidManifestException | IOException | SecurityException ignored) {
            return List.of();
        }
        return resources.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    IconBindingResult bindConfiguredItemIcon(
            ResourceLocation definitionId,
            int customModelData,
            ResourceLocation texture
    ) {
        if (definitionId == null || texture == null || customModelData <= 0
                || customModelData > 16_777_215 || assetPath(texture, ImportKind.TEXTURE) == null) {
            return IconBindingResult.failure("invalid item ID, CustomModelData, or texture ID");
        }
        try {
            ensurePackMetadata();
            ArrayList<IconBinding> bindings = new ArrayList<>(readIconBindings());
            IconBinding conflict = bindings.stream()
                    .filter(value -> value.customModelData() == customModelData
                            && carrierGroup(value.definition()).equals(carrierGroup(definitionId.toString()))
                            && !value.definition().equals(definitionId.toString()))
                    .findFirst().orElse(null);
            if (conflict != null) {
                return IconBindingResult.failure("CustomModelData " + customModelData
                        + " is already owned by " + conflict.definition());
            }
            String modelId = definitionId.getNamespace() + ":item/talent_editor_configured/"
                    + definitionId.getPath();
            IconBinding next = new IconBinding(
                    definitionId.toString(), customModelData, modelId, texture.toString()
            );
            bindings.removeIf(value -> value.definition().equals(definitionId.toString()));
            bindings.add(next);
            bindings.sort(Comparator.comparingInt(IconBinding::customModelData)
                    .thenComparing(IconBinding::definition));
            writeConfiguredItemModel(next);
            writeConfiguredItemCarrierModel(bindings);
            writeSystemItemModel(bindings, "talent_editor:talent_book", "minecraft:item/writable_book");
            writeSystemItemModel(bindings, "talent_editor:talent_point", "minecraft:item/nether_star");
            writeIconBindings(bindings);
            return new IconBindingResult(true, modelId, "");
        } catch (InvalidManifestException | IOException | SecurityException exception) {
            return IconBindingResult.failure(safeMessage(exception));
        }
    }

    private List<IconBinding> readIconBindings() throws IOException, InvalidManifestException {
        Path file = controlledTarget(ITEM_ICON_MANIFEST);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return List.of();
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (!root.has("schema") || root.get("schema").getAsInt() != 1
                    || !root.has("bindings") || !root.get("bindings").isJsonArray()) {
                throw new InvalidManifestException("unsupported item icon manifest");
            }
            ArrayList<IconBinding> result = new ArrayList<>();
            HashSet<String> definitions = new HashSet<>();
            HashSet<String> models = new HashSet<>();
            for (JsonElement element : root.getAsJsonArray("bindings")) {
                JsonObject value = element.getAsJsonObject();
                String definition = value.get("definition").getAsString();
                int modelData = value.get("customModelData").getAsInt();
                String model = value.get("model").getAsString();
                String texture = value.get("texture").getAsString();
                ResourceLocation definitionId = ResourceLocation.tryParse(definition);
                ResourceLocation modelId = ResourceLocation.tryParse(model);
                ResourceLocation textureId = ResourceLocation.tryParse(texture);
                if (definitionId == null || modelId == null || textureId == null
                        || modelData <= 0 || modelData > 16_777_215
                        || !models.add(carrierGroup(definition) + ":" + modelData)
                        || !definitions.add(definition)
                        || assetPath(textureId, ImportKind.TEXTURE) == null) {
                    throw new InvalidManifestException("invalid item icon binding");
                }
                result.add(new IconBinding(definition, modelData, model, texture));
            }
            return List.copyOf(result);
        } catch (InvalidManifestException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InvalidManifestException("invalid item icon manifest JSON");
        }
    }

    private void writeIconBindings(List<IconBinding> bindings) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 1);
        JsonArray values = new JsonArray();
        bindings.forEach(binding -> {
            JsonObject value = new JsonObject();
            value.addProperty("definition", binding.definition());
            value.addProperty("customModelData", binding.customModelData());
            value.addProperty("model", binding.model());
            value.addProperty("texture", binding.texture());
            values.add(value);
        });
        root.add("bindings", values);
        writeJson(controlledTarget(ITEM_ICON_MANIFEST), root);
    }

    private void writeConfiguredItemModel(IconBinding binding) throws IOException {
        ResourceLocation model = ResourceLocation.tryParse(binding.model());
        ResourceLocation texture = ResourceLocation.tryParse(binding.texture());
        if (model == null || texture == null) throw new IOException("invalid item icon model binding");
        String modelPath = "assets/" + model.getNamespace() + "/models/" + model.getPath() + ".json";
        String texturePath = texture.getPath().substring("textures/".length(),
                texture.getPath().length() - ".png".length());
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texture.getNamespace() + ":" + texturePath);
        json.add("textures", textures);
        writeJson(controlledTarget(modelPath), json);
    }

    private void writeConfiguredItemCarrierModel(List<IconBinding> bindings) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "minecraft:item/paper");
        root.add("textures", textures);
        JsonArray overrides = new JsonArray();
        bindings.stream().filter(binding -> !isSystemDefinition(binding.definition())).forEach(binding -> {
            JsonObject override = new JsonObject();
            JsonObject predicate = new JsonObject();
            predicate.addProperty("custom_model_data", binding.customModelData());
            override.add("predicate", predicate);
            override.addProperty("model", binding.model());
            overrides.add(override);
        });
        root.add("overrides", overrides);
        writeJson(controlledTarget("assets/talent_editor/models/item/configured_item.json"), root);
    }

    private void writeSystemItemModel(
            List<IconBinding> bindings,
            String definition,
            String defaultTexture
    ) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", defaultTexture);
        root.add("textures", textures);
        JsonArray overrides = new JsonArray();
        bindings.stream().filter(binding -> binding.definition().equals(definition)).forEach(binding -> {
            JsonObject override = new JsonObject();
            JsonObject predicate = new JsonObject();
            predicate.addProperty("custom_model_data", binding.customModelData());
            override.add("predicate", predicate);
            override.addProperty("model", binding.model());
            overrides.add(override);
        });
        root.add("overrides", overrides);
        String path = definition.substring(definition.indexOf(':') + 1);
        writeJson(controlledTarget("assets/talent_editor/models/item/" + path + ".json"), root);
    }

    private static boolean isSystemDefinition(String definition) {
        return definition.equals("talent_editor:talent_book")
                || definition.equals("talent_editor:talent_point");
    }

    private static String carrierGroup(String definition) {
        return isSystemDefinition(definition) ? definition : "talent_editor:configured_item";
    }

    private void ensurePackMetadata() throws IOException {
        ensureDirectories(root);
        JsonObject pack = new JsonObject();
        JsonObject details = new JsonObject();
        details.addProperty("pack_format", RESOURCE_PACK_FORMAT);
        details.addProperty("description", "Universal Talent Editor managed client resources");
        pack.add("pack", details);
        writeJson(controlledTarget("pack.mcmeta"), pack);
    }

    private List<ManifestEntry> readManifest() throws IOException, InvalidManifestException {
        Path manifest = controlledTarget(MANIFEST_NAME);
        if (!Files.exists(manifest, LinkOption.NOFOLLOW_LINKS)) return List.of();
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(manifest, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new InvalidManifestException("root is not an object");
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("schema") || root.get("schema").getAsInt() != MANIFEST_SCHEMA
                    || !root.has("resources") || !root.get("resources").isJsonArray()) {
                throw new InvalidManifestException("unsupported schema");
            }
            ArrayList<ManifestEntry> entries = new ArrayList<>();
            HashSet<String> resources = new HashSet<>();
            HashSet<String> assets = new HashSet<>();
            for (JsonElement element : root.getAsJsonArray("resources")) {
                JsonObject item = element.getAsJsonObject();
                ImportKind kind = ImportKind.valueOf(item.get("kind").getAsString().toUpperCase());
                ResourceLocation id = ResourceLocation.tryParse(item.get("resource").getAsString());
                String asset = item.get("asset").getAsString();
                long bytes = item.get("bytes").getAsLong();
                String hash = item.get("sha256").getAsString();
                boolean stream = item.has("stream") && item.get("stream").getAsBoolean();
                if (id == null || !asset.equals(assetPath(id, kind)) || bytes < 0
                        || !hash.matches("[0-9A-F]{64}") || !resources.add(id.toString())
                        || !assets.add(asset)) {
                    throw new InvalidManifestException("invalid resource entry");
                }
                controlledTarget(asset);
                entries.add(new ManifestEntry(kind, id.toString(), asset, bytes, hash, stream));
            }
            return List.copyOf(entries);
        } catch (InvalidManifestException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InvalidManifestException("invalid JSON or resource entry");
        }
    }

    private void writeManifest(List<ManifestEntry> entries) throws IOException {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("schema", MANIFEST_SCHEMA);
        manifest.addProperty("pack", PREFERRED_PACK_ID);
        JsonArray resources = new JsonArray();
        entries.stream().sorted(Comparator.comparing(ManifestEntry::asset)).forEach(entry -> {
            JsonObject item = new JsonObject();
            item.addProperty("kind", entry.kind().name().toLowerCase());
            item.addProperty("resource", entry.resource());
            item.addProperty("asset", entry.asset());
            item.addProperty("bytes", entry.bytes());
            item.addProperty("sha256", entry.sha256());
            if (entry.kind() == ImportKind.SOUND) item.addProperty("stream", entry.stream());
            resources.add(item);
        });
        manifest.add("resources", resources);
        writeJson(controlledTarget(MANIFEST_NAME), manifest);
    }

    private JsonObject readSounds(String namespace) throws IOException, InvalidManifestException {
        Path file = controlledTarget("assets/" + namespace + "/sounds.json");
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return new JsonObject();
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new InvalidManifestException("sounds.json is not an object");
            return parsed.getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new InvalidManifestException("invalid sounds.json");
        }
    }

    private void writeJson(Path target, JsonObject value) throws IOException {
        ensureDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".talent-json-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(value) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
            moveReplacing(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Path controlledTarget(String relative) throws IOException {
        String normalizedRelative = relative.replace('\\', '/');
        if (!safeSegments(normalizedRelative)) throw new IOException("unsafe target path");
        Path target = root.resolve(normalizedRelative).normalize();
        if (!target.startsWith(root)) throw new IOException("target escaped resource pack");
        return target;
    }

    private void ensureDirectories(Path directory) throws IOException {
        if (!directory.toAbsolutePath().normalize().startsWith(root)) {
            throw new IOException("directory escaped resource pack");
        }
        Path container = root.getParent();
        if (Files.exists(container, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(container) || !Files.isDirectory(container, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("resourcepacks directory is not a safe directory");
            }
        } else {
            Files.createDirectories(container);
        }
        Path cursor = root;
        if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)
                && (Files.isSymbolicLink(cursor) || !Files.isDirectory(cursor, LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException("resource pack root is a symbolic link");
        }
        if (!Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(cursor);
        Path relative = root.relativize(directory.toAbsolutePath().normalize());
        for (Path part : relative) {
            cursor = cursor.resolve(part);
            if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(cursor) || !Files.isDirectory(cursor, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("unsafe resource pack directory");
                }
            } else {
                Files.createDirectory(cursor);
            }
        }
    }

    private static String assetPath(ResourceLocation resource, ImportKind kind) {
        if (!safeSegments(resource.getPath())) return null;
        if (kind == ImportKind.TEXTURE) {
            if (!resource.getPath().startsWith("textures/") || !resource.getPath().endsWith(".png")) return null;
            return "assets/" + resource.getNamespace() + "/" + resource.getPath();
        }
        if (resource.getPath().endsWith(".ogg")) return null;
        return "assets/" + resource.getNamespace() + "/sounds/" + soundAssetPath(resource) + ".ogg";
    }

    private static String soundAssetPath(ResourceLocation resource) {
        return resource.getPath().replace('.', '/');
    }

    private static boolean safeSegments(String path) {
        if (path.isBlank() || path.startsWith("/") || path.endsWith("/")) return false;
        for (String part : path.split("/", -1)) {
            if (part.isBlank() || part.equals(".") || part.equals("..")) return false;
        }
        return true;
    }

    private static boolean validContent(Path file, ImportKind kind) throws IOException {
        if (kind == ImportKind.SOUND) {
            try (InputStream input = Files.newInputStream(file)) {
                return java.util.Arrays.equals(input.readNBytes(OGG_HEADER.length), OGG_HEADER);
            }
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) return false;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return false;
            ImageReader reader = readers.next();
            try {
                if (!reader.getFormatName().equalsIgnoreCase("png")) return false;
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_TEXTURE_EDGE || height > MAX_TEXTURE_EDGE
                        || (long) width * height > MAX_TEXTURE_PIXELS) return false;
                return reader.read(0) != null;
            } finally {
                reader.dispose();
            }
        }
    }

    private Preparation prepareSource(Path source, Path target, ImportKind kind) throws IOException {
        if (kind == ImportKind.TEXTURE) return prepareTexture(source, target);
        try (InputStream input = Files.newInputStream(source)) {
            if (java.util.Arrays.equals(input.readNBytes(OGG_HEADER.length), OGG_HEADER)) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return Preparation.success();
            }
        }
        return Preparation.failure(ImportCode.INVALID_CONTENT, "expected OGG Vorbis source");
    }

    private static Preparation prepareTexture(Path source, Path target) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(source.toFile())) {
            if (input == null) return Preparation.failure(ImportCode.INVALID_CONTENT, "texture");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return Preparation.failure(ImportCode.INVALID_CONTENT, "texture");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) {
                    return Preparation.failure(ImportCode.INVALID_CONTENT, "expected PNG, JPG, or JPEG source");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_TEXTURE_EDGE || height > MAX_TEXTURE_EDGE
                        || (long) width * height > MAX_TEXTURE_PIXELS) {
                    return Preparation.failure(ImportCode.INVALID_CONTENT, "texture dimensions");
                }
                BufferedImage image = reader.read(0);
                if (image == null || !ImageIO.write(image, "png", target.toFile())) {
                    return Preparation.failure(ImportCode.INVALID_CONTENT, "PNG conversion");
                }
                return Preparation.success();
            } finally {
                reader.dispose();
            }
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[16_384];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().withUpperCase().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message;
    }

    enum ImportKind {
        TEXTURE,
        SOUND
    }

    enum ImportCode {
        IMPORTED,
        SOURCE_MISSING,
        SOURCE_TOO_LARGE,
        INVALID_CONTENT,
        INVALID_TARGET,
        UNMANAGED_TARGET,
        TARGET_CONFLICT,
        MANIFEST_INVALID,
        IO_ERROR
    }

    record ImportResult(
            ImportCode code,
            ResourceLocation resource,
            String asset,
            long bytes,
            String sha256,
            String detail
    ) {
        static ImportResult failure(ImportCode code, String detail) {
            return new ImportResult(code, null, "", 0L, "", detail == null ? "" : detail);
        }

        boolean successful() {
            return code == ImportCode.IMPORTED;
        }
    }

    record AuditResult(boolean valid, int total, int missing, int changed, String detail) {
    }

    record IconBindingResult(boolean successful, String model, String detail) {
        static IconBindingResult failure(String detail) {
            return new IconBindingResult(false, "", detail == null ? "" : detail);
        }
    }

    private record IconBinding(String definition, int customModelData, String model, String texture) {
    }

    private record ManifestEntry(
            ImportKind kind,
            String resource,
            String asset,
            long bytes,
            String sha256,
            boolean stream
    ) {
    }

    private record Preparation(boolean successful, ImportCode code, String detail) {
        private static Preparation success() {
            return new Preparation(true, ImportCode.IMPORTED, "");
        }

        private static Preparation failure(ImportCode code, String detail) {
            return new Preparation(false, code, detail == null ? "" : detail);
        }
    }

    private static final class InvalidManifestException extends RuntimeException {
        private InvalidManifestException(String message) {
            super(message);
        }
    }
}
