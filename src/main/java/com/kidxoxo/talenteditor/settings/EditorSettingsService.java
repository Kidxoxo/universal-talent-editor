package com.kidxoxo.talenteditor.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.editor.EditorPackStorage;
import com.kidxoxo.talenteditor.editor.DefinitionService;
import com.kidxoxo.talenteditor.compile.CompilationResult;
import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.kidxoxo.talenteditor.forge.ForgeCompilationEnvironment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Saves world behavior, exports complete editor packs, and owns the game-wide new-world template. */
public final class EditorSettingsService {
    static final String SETTINGS_FILE = "talent_editor.settings.json";
    static final String GLOBAL_DIRECTORY = "talent_editor/global_template";
    static final String EXPORT_DIRECTORY = "talent_editor_exports";
    static final String IMPORT_STAGING_DIRECTORY = ".talent_editor_import_staging";
    private static final long MAX_ARCHIVE_BYTES = 512L * 1024L * 1024L;
    private static final long MAX_ENTRY_BYTES = 64L * 1024L * 1024L;
    private static final int MAX_ARCHIVE_ENTRIES = 50_000;
    private static final int SCHEMA = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter EXPORT_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final EditorPackStorage storage;
    private volatile EditorRuntimeSettings current = EditorRuntimeSettings.DEFAULT;

    public EditorSettingsService(EditorPackStorage storage) {
        this.storage = storage;
    }

    public EditorRuntimeSettings current() {
        return current;
    }

    public boolean automaticSynthesis() {
        return current.automaticSynthesis();
    }

    public EditorRuntimeSettings.SynthesisSoundAudience synthesisSoundAudience() {
        return current.synthesisSoundAudience();
    }

    public EditorRuntimeSettings.SynthesisMessageAudience synthesisMessageAudience() {
        return current.synthesisMessageAudience();
    }

    public EditorRuntimeSettings.SoundOverlapPolicy soundOverlapPolicy() {
        return current.soundOverlapPolicy();
    }

    /** Called once for each server. Returns true when a game-wide template was installed into the world. */
    public synchronized boolean initializeServer(MinecraftServer server) {
        boolean installed = false;
        try {
            installed = installTemplateIfWorldHasNoEditorPack(
                    globalPackRoot(), storage.datapacksRoot(server).resolve(EditorPackStorage.PACK_FOLDER)
            );
            current = readSettings(storage.datapacksRoot(server).resolve(EditorPackStorage.PACK_FOLDER));
        } catch (IOException | RuntimeException exception) {
            current = EditorRuntimeSettings.DEFAULT;
            com.kidxoxo.talenteditor.TalentEditorMod.LOGGER.error(
                    "Could not initialize talent editor settings; using defaults", exception
            );
        }
        return installed;
    }

    public synchronized void clearServer() {
        current = EditorRuntimeSettings.DEFAULT;
    }

    public synchronized EditorRuntimeSettings update(
            MinecraftServer server,
            EditorRuntimeSettings settings
    ) throws IOException {
        Path packRoot = storage.ensurePackExists(server);
        writeSettings(packRoot, settings);
        current = settings;
        return settings;
    }

    public synchronized Path exportCurrent(MinecraftServer server) throws IOException {
        Path gameRoot = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path packRoot = storage.ensurePackExists(server);
        writeSettings(packRoot, current);
        Path exportRoot = gameRoot.resolve(EXPORT_DIRECTORY).normalize();
        requireInside(gameRoot, exportRoot, false);
        Files.createDirectories(exportRoot);
        String world = safeFileName(server.getWorldData().getLevelName());
        Path target = uniquePath(exportRoot, world + "-talent-settings-"
                + EXPORT_TIME.format(LocalDateTime.now()), ".zip");
        Path resolved = exportRoot.resolve(".resolved-" + UUID.randomUUID()).normalize();
        try {
            writeResolvedPack(resolved, com.kidxoxo.talenteditor.TalentEditorMod.DEFINITIONS.activeRaw(), current);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(
                    target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
            ), StandardCharsets.UTF_8)) {
                zipDirectory(resolved, "datapacks/" + EditorPackStorage.PACK_FOLDER + "/", zip);
                Path clientPack = gameRoot.resolve("resourcepacks")
                        .resolve(EditorPackStorage.PACK_FOLDER).normalize();
                if (Files.isDirectory(clientPack, LinkOption.NOFOLLOW_LINKS)
                        && !Files.isSymbolicLink(clientPack)) {
                    zipDirectory(clientPack,
                            "resourcepacks/" + EditorPackStorage.PACK_FOLDER + "/", zip);
                }
            }
        } catch (IOException exception) {
            Files.deleteIfExists(target);
            throw exception;
        } finally {
            if (Files.exists(resolved, LinkOption.NOFOLLOW_LINKS)) deleteTree(resolved, exportRoot);
        }
        return target;
    }

    public synchronized Path installGlobalTemplate(MinecraftServer server) throws IOException {
        Path source = storage.ensurePackExists(server);
        writeSettings(source, current);
        Path templateParent = globalPackRoot().getParent();
        Files.createDirectories(templateParent);
        Path staging = templateParent.resolve("." + EditorPackStorage.PACK_FOLDER
                + "." + UUID.randomUUID() + ".tmp").normalize();
        requireInside(templateParent, staging, false);
        Path target = globalPackRoot();
        Path backup = null;
        try {
            writeResolvedPack(staging, com.kidxoxo.talenteditor.TalentEditorMod.DEFINITIONS.activeRaw(), current);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                backup = uniquePath(templateParent,
                        EditorPackStorage.PACK_FOLDER + "-backup-" + EXPORT_TIME.format(LocalDateTime.now()), "");
                move(target, backup);
            }
            try {
                move(staging, target);
            } catch (IOException exception) {
                if (backup != null && !Files.exists(target, LinkOption.NOFOLLOW_LINKS)) move(backup, target);
                throw exception;
            }
            return target;
        } finally {
            if (Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) deleteTree(staging, templateParent);
        }
    }

    /**
     * Safely stages and installs one of this game directory's own exports. The caller must reload data packs and
     * then either complete or roll back the returned transaction.
     */
    public synchronized ImportInstallation beginImport(MinecraftServer server, String input) throws IOException {
        Path gameRoot = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path archive = resolveImportArchive(gameRoot, input);
        Path stagingParent = gameRoot.resolve(IMPORT_STAGING_DIRECTORY).normalize();
        requireInside(gameRoot, stagingParent, false);
        Files.createDirectories(stagingParent);
        Path staging = stagingParent.resolve(UUID.randomUUID().toString()).normalize();
        Files.createDirectory(staging);

        Path serverSource = staging.resolve("datapacks").resolve(EditorPackStorage.PACK_FOLDER).normalize();
        Path clientSource = staging.resolve("resourcepacks").resolve(EditorPackStorage.PACK_FOLDER).normalize();
        Path serverTarget = storage.datapacksRoot(server).resolve(EditorPackStorage.PACK_FOLDER).normalize();
        Path clientTarget = gameRoot.resolve("resourcepacks").resolve(EditorPackStorage.PACK_FOLDER).normalize();
        Path serverBackup = null;
        Path clientBackup = null;
        EditorRuntimeSettings previous = current;
        boolean clientInstalled = false;
        try {
            extractImportArchive(archive, staging);
            RawDefinitionSet imported = readImportedDefinitions(serverSource);
            CompilationResult compilation = com.kidxoxo.talenteditor.TalentEditorMod.COMPILER.compile(
                    imported, ForgeCompilationEnvironment.capture()
            );
            if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) {
                String detail = compilation.report().issues().stream()
                        .filter(issue -> issue.severity()
                                == com.kidxoxo.talenteditor.validation.ValidationSeverity.ERROR)
                        .findFirst()
                        .map(issue -> issue.code() + " " + issue.path() + ": " + issue.message())
                        .orElse("Imported definitions did not produce a runtime snapshot");
                throw new IOException("Imported talent definitions are invalid: " + detail);
            }
            EditorRuntimeSettings importedSettings = readSettings(serverSource);
            validateClientPack(clientSource);

            Files.createDirectories(serverTarget.getParent());
            serverBackup = moveExistingToBackup(serverTarget);
            try {
                move(serverSource, serverTarget);
            } catch (IOException exception) {
                restoreBackup(serverTarget, serverBackup);
                throw exception;
            }

            if (Files.isDirectory(clientSource, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectories(clientTarget.getParent());
                clientBackup = moveExistingToBackup(clientTarget);
                try {
                    move(clientSource, clientTarget);
                    clientInstalled = true;
                } catch (IOException exception) {
                    restoreBackup(clientTarget, clientBackup);
                    restoreBackup(serverTarget, serverBackup);
                    throw exception;
                }
            }
            current = importedSettings;
            return new ImportInstallation(
                    archive, staging, serverTarget, serverBackup, clientTarget, clientBackup,
                    clientInstalled, previous
            );
        } catch (IOException | RuntimeException exception) {
            current = previous;
            if (Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) deleteTree(staging, stagingParent);
            if (exception instanceof IOException ioException) throw ioException;
            throw new IOException("Could not import talent editor archive", exception);
        }
    }

    public synchronized String completeImport(ImportInstallation installation) throws IOException {
        cleanupImportStaging(installation);
        return installation.archive().toString();
    }

    public synchronized void rollbackImport(ImportInstallation installation) throws IOException {
        IOException failure = null;
        try {
            if (installation.clientPackInstalled()) {
                restoreBackup(installation.clientTarget(), installation.clientBackup());
            }
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            restoreBackup(installation.serverTarget(), installation.serverBackup());
        } catch (IOException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        current = installation.previousSettings();
        try {
            cleanupImportStaging(installation);
        } catch (IOException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }

    public boolean globalTemplateAvailable() {
        Path root = globalPackRoot();
        return Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(root)
                && Files.isRegularFile(root.resolve("pack.mcmeta"), LinkOption.NOFOLLOW_LINKS);
    }

    Path globalPackRoot() {
        return FMLPaths.CONFIGDIR.get().toAbsolutePath().normalize()
                .resolve(GLOBAL_DIRECTORY).resolve(EditorPackStorage.PACK_FOLDER).normalize();
    }

    static EditorRuntimeSettings readSettings(Path packRoot) throws IOException {
        Path file = packRoot.resolve(SETTINGS_FILE).normalize();
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return EditorRuntimeSettings.DEFAULT;
        if (Files.isSymbolicLink(file) || Files.size(file) > 64 * 1024L) {
            throw new IOException("Unsafe or oversized editor settings file");
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (!root.has("schema") || root.get("schema").getAsInt() != SCHEMA) {
                throw new IOException("Unsupported editor settings schema");
            }
            return new EditorRuntimeSettings(
                    !root.has("automaticSynthesis") || root.get("automaticSynthesis").getAsBoolean(),
                    root.has("synthesisSoundAudience")
                            ? EditorRuntimeSettings.SynthesisSoundAudience.parse(
                            root.get("synthesisSoundAudience").getAsString())
                            : EditorRuntimeSettings.SynthesisSoundAudience.PERSONAL,
                    root.has("synthesisMessageAudience")
                            ? EditorRuntimeSettings.SynthesisMessageAudience.parse(
                            root.get("synthesisMessageAudience").getAsString())
                            : EditorRuntimeSettings.SynthesisMessageAudience.PERSONAL,
                    root.has("soundOverlapPolicy")
                            ? EditorRuntimeSettings.SoundOverlapPolicy.parse(
                            root.get("soundOverlapPolicy").getAsString())
                            : EditorRuntimeSettings.SoundOverlapPolicy.IMMEDIATE
            );
        } catch (RuntimeException exception) {
            throw new IOException("Invalid editor settings JSON", exception);
        }
    }

    static void writeSettings(Path packRoot, EditorRuntimeSettings settings) throws IOException {
        if (!Files.isDirectory(packRoot, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(packRoot)) {
            throw new IOException("Editor pack root is not a safe directory");
        }
        JsonObject root = new JsonObject();
        root.addProperty("schema", SCHEMA);
        root.addProperty("automaticSynthesis", settings.automaticSynthesis());
        root.addProperty("synthesisSoundAudience", settings.synthesisSoundAudience().serializedName());
        root.addProperty("synthesisMessageAudience", settings.synthesisMessageAudience().serializedName());
        root.addProperty("soundOverlapPolicy", settings.soundOverlapPolicy().serializedName());
        writeAtomically(packRoot.resolve(SETTINGS_FILE), GSON.toJson(root) + System.lineSeparator());
    }

    static boolean installTemplateIfWorldHasNoEditorPack(Path template, Path worldPack) throws IOException {
        if (Files.exists(worldPack, LinkOption.NOFOLLOW_LINKS)) return false;
        if (!Files.isDirectory(template, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(template)
                || !Files.isRegularFile(template.resolve("pack.mcmeta"), LinkOption.NOFOLLOW_LINKS)) return false;
        Path datapacksRoot = worldPack.getParent().toAbsolutePath().normalize();
        Files.createDirectories(datapacksRoot);
        Path staging = datapacksRoot.resolve("." + EditorPackStorage.PACK_FOLDER
                + "." + UUID.randomUUID() + ".tmp").normalize();
        copyDirectory(template, staging);
        try {
            move(staging, worldPack);
        } finally {
            if (Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) deleteTree(staging, datapacksRoot);
        }
        return true;
    }

    static void extractImportArchive(Path archive, Path staging) throws IOException {
        if (!Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(archive)) {
            throw new IOException("Import archive is not a safe regular file");
        }
        if (Files.size(archive) > MAX_ARCHIVE_BYTES) throw new IOException("Import archive exceeds 512 MiB");
        Path normalizedStaging = staging.toAbsolutePath().normalize();
        Set<String> destinations = new HashSet<>();
        long totalBytes = 0L;
        int entries = 0;
        byte[] buffer = new byte[16 * 1024];
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ARCHIVE_ENTRIES) throw new IOException("Import archive has too many entries");
                String name = entry.getName();
                validateArchiveEntryName(name, entry.isDirectory());
                Path destination = normalizedStaging.resolve(name).normalize();
                requireInside(normalizedStaging, destination, false);
                String destinationKey = destination.toString().toLowerCase(Locale.ROOT);
                if (!destinations.add(destinationKey)) throw new IOException("Duplicate archive entry: " + name);
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    zip.closeEntry();
                    continue;
                }
                Files.createDirectories(destination.getParent());
                long entryBytes = 0L;
                try (var output = Files.newOutputStream(
                        destination, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
                )) {
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        entryBytes += read;
                        totalBytes += read;
                        if (entryBytes > MAX_ENTRY_BYTES || totalBytes > MAX_ARCHIVE_BYTES) {
                            throw new IOException("Import archive expands beyond its safety limit");
                        }
                        output.write(buffer, 0, read);
                    }
                }
                zip.closeEntry();
            }
        }
        Path serverPack = normalizedStaging.resolve("datapacks").resolve(EditorPackStorage.PACK_FOLDER);
        if (!Files.isRegularFile(serverPack.resolve("pack.mcmeta"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Archive does not contain the exported talent editor data pack");
        }
    }

    static RawDefinitionSet readImportedDefinitions(Path serverPack) throws IOException {
        if (!Files.isDirectory(serverPack, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(serverPack)) {
            throw new IOException("Imported data pack is missing or unsafe");
        }
        JsonObject metadata = readJsonObject(serverPack.resolve("pack.mcmeta"), 64 * 1024, "pack metadata");
        if (!metadata.has("pack") || !metadata.get("pack").isJsonObject()) {
            throw new IOException("Imported pack.mcmeta has no pack object");
        }
        readSettings(serverPack);
        LinkedHashMap<ResourceLocation, JsonObject> rarities = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, JsonObject> pools = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, JsonObject> counters = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, JsonObject> layouts = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, JsonObject> items = new LinkedHashMap<>();
        loadImportedDirectory(serverPack, "rarities", rarities);
        loadImportedDirectory(serverPack, "talents", talents);
        loadImportedDirectory(serverPack, "pools", pools);
        loadImportedDirectory(serverPack, "counters", counters);
        loadImportedDirectory(serverPack, "layouts", layouts);
        loadImportedDirectory(serverPack, "items", items);
        return new RawDefinitionSet(
                rarities, talents, pools, counters, layouts, items,
                sourceMap(rarities), sourceMap(talents), sourceMap(pools), sourceMap(counters),
                sourceMap(layouts), sourceMap(items), List.of()
        );
    }

    static void writeResolvedPack(
            Path packRoot,
            RawDefinitionSet definitions,
            EditorRuntimeSettings settings
    ) throws IOException {
        if (Files.exists(packRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Resolved pack target already exists");
        }
        Files.createDirectories(packRoot);
        JsonObject description = new JsonObject();
        description.addProperty("pack_format", 15);
        description.addProperty("description", "Universal Talent Editor resolved settings snapshot");
        JsonObject metadata = new JsonObject();
        metadata.add("pack", description);
        writeAtomically(packRoot.resolve("pack.mcmeta"), GSON.toJson(metadata) + System.lineSeparator());
        writeSettings(packRoot, settings);
        writeDefinitions(packRoot, "rarities", definitions.rarities());
        writeDefinitions(packRoot, "talents", definitions.talents());
        writeDefinitions(packRoot, "pools", definitions.pools());
        writeDefinitions(packRoot, "counters", definitions.counters());
        writeDefinitions(packRoot, "layouts", definitions.layouts());
        writeDefinitions(packRoot, "items", definitions.configuredItems());
    }

    private static void writeDefinitions(
            Path packRoot,
            String directory,
            Map<ResourceLocation, JsonObject> definitions
    ) throws IOException {
        for (Map.Entry<ResourceLocation, JsonObject> entry : definitions.entrySet()) {
            ResourceLocation id = entry.getKey();
            Path root = packRoot.resolve("data").resolve(id.getNamespace())
                    .resolve("talent_editor").resolve(directory).normalize();
            Path target = root.resolve(id.getPath() + ".json").normalize();
            requireInside(packRoot, target, false);
            writeAtomically(target, GSON.toJson(entry.getValue()) + System.lineSeparator());
        }
    }

    private static void zipDirectory(Path sourceRoot, String prefix, ZipOutputStream zip) throws IOException {
        Path normalized = sourceRoot.toAbsolutePath().normalize();
        try (var stream = Files.walk(normalized)) {
            for (Path source : stream.sorted().toList()) {
                if (source.equals(normalized)) continue;
                if (Files.isSymbolicLink(source)) throw new IOException("Symbolic links cannot be exported");
                String relative = normalized.relativize(source).toString().replace('\\', '/');
                if (relative.startsWith(".talent_editor_history/")
                        || relative.startsWith(".talent_editor_backups/")
                        || relative.startsWith(".talent_editor_recovery/")) continue;
                String name = prefix + relative + (Files.isDirectory(source) ? "/" : "");
                ZipEntry entry = new ZipEntry(name);
                zip.putNextEntry(entry);
                if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                    try (InputStream input = Files.newInputStream(source)) {
                        input.transferTo(zip);
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private static Path resolveImportArchive(Path gameRoot, String input) throws IOException {
        Path exportRoot = gameRoot.resolve(EXPORT_DIRECTORY).normalize();
        requireInside(gameRoot, exportRoot, false);
        if (!Files.isDirectory(exportRoot, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(exportRoot)) {
            throw new IOException("No talent_editor_exports directory exists in this game directory");
        }
        String value = input == null ? "" : input.strip();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        Path candidate;
        if (value.isBlank()) {
            try (var stream = Files.list(exportRoot)) {
                candidate = stream
                        .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                        .max(Comparator.comparingLong(EditorSettingsService::lastModified))
                        .orElseThrow(() -> new IOException("No exported ZIP was found"));
            }
        } else {
            try {
                candidate = Path.of(value);
            } catch (RuntimeException exception) {
                throw new IOException("Invalid import archive path", exception);
            }
            if (!candidate.isAbsolute()) candidate = exportRoot.resolve(candidate);
        }
        candidate = candidate.toAbsolutePath().normalize();
        requireInside(exportRoot, candidate, false);
        if (!candidate.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new IOException("Import file must use the .zip extension");
        }
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(candidate)) {
            throw new IOException("Import ZIP does not exist or is unsafe");
        }
        Path realRoot = exportRoot.toRealPath(LinkOption.NOFOLLOW_LINKS);
        Path realCandidate = candidate.toRealPath(LinkOption.NOFOLLOW_LINKS);
        requireInside(realRoot, realCandidate, false);
        return realCandidate;
    }

    private static void validateArchiveEntryName(String name, boolean directory) throws IOException {
        if (name == null || name.isBlank() || name.indexOf('\0') >= 0 || name.indexOf('\\') >= 0
                || name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
            throw new IOException("Unsafe archive entry name");
        }
        String trimmed = directory && name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
        for (String segment : trimmed.split("/", -1)) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Unsafe archive entry: " + name);
            }
        }
        String serverPrefix = "datapacks/" + EditorPackStorage.PACK_FOLDER + "/";
        String clientPrefix = "resourcepacks/" + EditorPackStorage.PACK_FOLDER + "/";
        if (name.startsWith(serverPrefix)) {
            String relative = name.substring(serverPrefix.length());
            if (relative.isBlank() && directory) return;
            if (relative.equals("pack.mcmeta") || relative.equals(SETTINGS_FILE)) return;
            if (validDefinitionArchivePath(relative, directory)) return;
            throw new IOException("Unsupported file in imported data pack: " + name);
        }
        if (name.startsWith(clientPrefix)) {
            String relative = name.substring(clientPrefix.length());
            if (relative.isBlank() && directory) return;
            if (relative.equals("pack.mcmeta") || relative.equals("pack.png")
                    || relative.equals("talent_editor.manifest.json")
                    || relative.equals("talent_editor.item_icons.json")
                    || relative.startsWith("assets/") && (directory || isClientResourceFile(relative))) return;
            throw new IOException("Unsupported file in imported client resource pack: " + name);
        }
        throw new IOException("Archive entry is outside the exported editor packs: " + name);
    }

    private static boolean validDefinitionArchivePath(String relative, boolean directory) {
        String value = directory && relative.endsWith("/")
                ? relative.substring(0, relative.length() - 1) : relative;
        String[] segments = value.split("/", -1);
        if (segments.length == 1) return directory && segments[0].equals("data");
        if (!segments[0].equals("data")) return false;
        if (segments.length == 2) return directory && ResourceLocation.isValidNamespace(segments[1]);
        if (!ResourceLocation.isValidNamespace(segments[1]) || !segments[2].equals("talent_editor")) return false;
        if (segments.length == 3) return directory;
        Set<String> kinds = Set.of("rarities", "talents", "pools", "counters", "layouts", "items");
        if (!kinds.contains(segments[3])) return false;
        if (segments.length == 4) return directory;
        if (directory) return true;
        return value.endsWith(".json") && value.length() > ".json".length();
    }

    private static boolean isClientResourceFile(String relative) {
        String lower = relative.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".ogg") || lower.endsWith(".json") || lower.endsWith(".mcmeta");
    }

    private static void loadImportedDirectory(
            Path serverPack, String kind, Map<ResourceLocation, JsonObject> output
    ) throws IOException {
        Path data = serverPack.resolve("data");
        if (!Files.isDirectory(data, LinkOption.NOFOLLOW_LINKS)) return;
        try (var namespaces = Files.list(data)) {
            for (Path namespaceRoot : namespaces.toList()) {
                if (!Files.isDirectory(namespaceRoot, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(namespaceRoot)) {
                    throw new IOException("Unsafe namespace entry in imported data pack");
                }
                String namespace = namespaceRoot.getFileName().toString();
                Path root = namespaceRoot.resolve("talent_editor").resolve(kind);
                if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) continue;
                try (var files = Files.walk(root)) {
                    for (Path file : files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).toList()) {
                        if (Files.isSymbolicLink(file)) throw new IOException("Imported definitions cannot be symlinks");
                        String relative = root.relativize(file).toString().replace('\\', '/');
                        if (!relative.endsWith(".json")) {
                            throw new IOException("Imported definition is not JSON: " + relative);
                        }
                        String definitionPath = relative.substring(0, relative.length() - 5);
                        ResourceLocation id = ResourceLocation.tryBuild(namespace, definitionPath);
                        if (id == null) throw new IOException("Invalid imported definition id: "
                                + namespace + ":" + definitionPath);
                        if (output.putIfAbsent(id, readJsonObject(
                                file, DefinitionService.MAX_JSON_CHARACTERS, "definition " + id
                        )) != null) {
                            throw new IOException("Duplicate imported definition: " + id);
                        }
                    }
                }
            }
        }
    }

    private static JsonObject readJsonObject(Path file, int maxCharacters, String description) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)
                || Files.size(file) > MAX_ENTRY_BYTES) {
            throw new IOException("Missing, unsafe, or oversized " + description);
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        if (text.length() > maxCharacters) throw new IOException("Oversized " + description);
        try {
            var parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) throw new IOException(description + " must be a JSON object");
            return parsed.getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid JSON in " + description, exception);
        }
    }

    private static Map<ResourceLocation, String> sourceMap(Map<ResourceLocation, JsonObject> definitions) {
        LinkedHashMap<ResourceLocation, String> result = new LinkedHashMap<>();
        definitions.keySet().forEach(id -> result.put(id, EditorPackStorage.PACK_FOLDER));
        return result;
    }

    private static void validateClientPack(Path clientPack) throws IOException {
        if (!Files.exists(clientPack, LinkOption.NOFOLLOW_LINKS)) return;
        if (!Files.isDirectory(clientPack, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(clientPack)) {
            throw new IOException("Imported client resource pack is unsafe");
        }
        JsonObject metadata = readJsonObject(clientPack.resolve("pack.mcmeta"), 64 * 1024, "client pack metadata");
        if (!metadata.has("pack") || !metadata.get("pack").isJsonObject()) {
            throw new IOException("Imported client pack.mcmeta has no pack object");
        }
    }

    private static Path moveExistingToBackup(Path target) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return null;
        if (Files.isSymbolicLink(target)) throw new IOException("Managed pack target cannot be a symbolic link");
        Path parent = target.getParent().toAbsolutePath().normalize();
        Path backup = uniquePath(parent, EditorPackStorage.PACK_FOLDER + "-backup-"
                + EXPORT_TIME.format(LocalDateTime.now()), "");
        move(target, backup);
        return backup;
    }

    private static void restoreBackup(Path target, Path backup) throws IOException {
        Path parent = target.getParent().toAbsolutePath().normalize();
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) deleteTree(target, parent);
        if (backup != null && Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) move(backup, target);
    }

    private static void cleanupImportStaging(ImportInstallation installation) throws IOException {
        Path staging = installation.staging().toAbsolutePath().normalize();
        if (Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) deleteTree(staging, staging.getParent());
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }

    private static void copyDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        if (Files.exists(targetRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Copy target already exists");
        }
        Path source = sourceRoot.toAbsolutePath().normalize();
        Path target = targetRoot.toAbsolutePath().normalize();
        Files.createDirectory(target);
        try (var stream = Files.walk(source)) {
            for (Path value : stream.sorted().toList()) {
                if (value.equals(source)) continue;
                if (Files.isSymbolicLink(value)) throw new IOException("Symbolic links cannot be copied");
                Path relative = source.relativize(value);
                if (relative.getNameCount() > 0
                        && relative.getName(0).toString().startsWith(".talent_editor_")) continue;
                Path destination = target.resolve(relative).normalize();
                requireInside(target, destination, false);
                if (Files.isDirectory(value, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(destination);
                else if (Files.isRegularFile(value, LinkOption.NOFOLLOW_LINKS)) {
                    Files.copy(value, destination, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        } catch (IOException exception) {
            deleteTree(target, target.getParent());
            throw exception;
        }
    }

    private static void deleteTree(Path root, Path allowedParent) throws IOException {
        requireInside(allowedParent.toAbsolutePath().normalize(), root.toAbsolutePath().normalize(), false);
        try (var stream = Files.walk(root)) {
            for (Path value : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(value);
        }
    }

    private static void writeAtomically(Path target, String text) throws IOException {
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = parent.resolve("." + target.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target);
        }
    }

    private static Path uniquePath(Path directory, String base, String suffix) {
        Path candidate = directory.resolve(base + suffix);
        int counter = 2;
        while (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            candidate = directory.resolve(base + "-" + counter++ + suffix);
        }
        return candidate;
    }

    private static String safeFileName(String value) {
        String safe = value == null ? "world" : value.strip().replaceAll("[^\\p{L}\\p{N}._-]+", "_");
        return safe.isBlank() ? "world" : safe.substring(0, Math.min(80, safe.length()));
    }

    private static void requireInside(Path root, Path target, boolean allowRoot) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(normalizedRoot) || !allowRoot && normalizedTarget.equals(normalizedRoot)) {
            throw new IOException("Path escaped its managed directory");
        }
    }

    public enum ManagementAction {
        UPDATE_SETTINGS,
        EXPORT,
        INSTALL_GLOBAL,
        IMPORT
    }

    public record ImportInstallation(
            Path archive,
            Path staging,
            Path serverTarget,
            Path serverBackup,
            Path clientTarget,
            Path clientBackup,
            boolean clientPackInstalled,
            EditorRuntimeSettings previousSettings
    ) {
    }

    public record ManagementResult(ManagementAction action, boolean successful, String detail) {
        public ManagementResult {
            detail = detail == null ? "" : detail;
        }
    }
}
