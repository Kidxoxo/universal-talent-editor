package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedClientResourcePackTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void importsTextureIntoControlledPackAndAuditsHash() throws Exception {
        ManagedClientResourcePack pack = new ManagedClientResourcePack(temporaryDirectory);
        Path source = png("icon.png", Color.MAGENTA);
        ResourceLocation id = new ResourceLocation("example", "textures/talent_icons/1.png");

        ManagedClientResourcePack.ImportResult result = pack.importFile(
                source, id, ManagedClientResourcePack.ImportKind.TEXTURE, false
        );

        assertTrue(result.successful());
        assertEquals("assets/example/textures/talent_icons/1.png", result.asset());
        assertTrue(result.sha256().matches("[0-9A-F]{64}"));
        assertTrue(Files.isRegularFile(pack.root().resolve("pack.mcmeta")));
        assertTrue(Files.isRegularFile(pack.root().resolve(result.asset())));
        JsonObject manifest = json(pack.root().resolve(ManagedClientResourcePack.MANIFEST_NAME));
        assertEquals(1, manifest.get("schema").getAsInt());
        assertEquals(result.sha256(), manifest.getAsJsonArray("resources").get(0)
                .getAsJsonObject().get("sha256").getAsString());
        ManagedClientResourcePack.AuditResult audit = pack.audit();
        assertTrue(audit.valid());
        assertEquals(1, audit.total());
        assertEquals(0, audit.missing());
        assertEquals(0, audit.changed());
        assertEquals(result.sha256(), pack.verifiedHashes(
                ManagedClientResourcePack.ImportKind.TEXTURE
        ).get(id));
        assertEquals(java.util.List.of(id), pack.verifiedResources(
                ManagedClientResourcePack.ImportKind.TEXTURE));
    }

    @Test
    void convertsJpegSourceToCanonicalPngTexture() throws Exception {
        ManagedClientResourcePack pack = new ManagedClientResourcePack(temporaryDirectory);
        Path source = image("photo.jpg", "jpeg", BufferedImage.TYPE_INT_RGB, Color.ORANGE);
        ResourceLocation id = new ResourceLocation("example", "textures/gui/photo.png");

        ManagedClientResourcePack.ImportResult result = pack.importFile(
                source, id, ManagedClientResourcePack.ImportKind.TEXTURE, false
        );

        assertTrue(result.successful());
        Path imported = pack.root().resolve(result.asset());
        try (ImageInputStream input = ImageIO.createImageInputStream(imported.toFile())) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            try {
                assertEquals("png", reader.getFormatName().toLowerCase(java.util.Locale.ROOT));
            } finally {
                reader.dispose();
            }
        }
    }

    @Test
    void importsSoundAndGeneratesNamespacedSoundEvent() throws Exception {
        ManagedClientResourcePack pack = new ManagedClientResourcePack(temporaryDirectory);
        Path source = temporaryDirectory.resolve("acquire.ogg");
        Files.write(source, new byte[]{'O', 'g', 'g', 'S', 0, 2, 4, 8});
        ResourceLocation id = new ResourceLocation("example", "talent.acquire");

        ManagedClientResourcePack.ImportResult result = pack.importFile(
                source, id, ManagedClientResourcePack.ImportKind.SOUND, true
        );

        assertTrue(result.successful());
        assertEquals("assets/example/sounds/talent/acquire.ogg", result.asset());
        JsonObject sounds = json(pack.root().resolve("assets/example/sounds.json"));
        JsonObject choice = sounds.getAsJsonObject("talent.acquire")
                .getAsJsonArray("sounds").get(0).getAsJsonObject();
        assertEquals("example:talent/acquire", choice.get("name").getAsString());
        assertTrue(choice.get("stream").getAsBoolean());
    }

    @Test
    void rejectsMp3SourceWithoutCreatingManagedEntry() throws Exception {
        ManagedClientResourcePack pack = new ManagedClientResourcePack(temporaryDirectory);
        Path source = temporaryDirectory.resolve("acquire.mp3");
        Files.write(source, new byte[]{'I', 'D', '3', 1, 2, 3});

        ManagedClientResourcePack.ImportResult result = pack.importFile(
                source, new ResourceLocation("example", "talent.rejected"),
                ManagedClientResourcePack.ImportKind.SOUND, false
        );

        assertEquals(ManagedClientResourcePack.ImportCode.INVALID_CONTENT, result.code());
        assertFalse(Files.exists(pack.root().resolve(ManagedClientResourcePack.MANIFEST_NAME)));
    }

    @Test
    void rejectsInvalidContentUnsafeTargetsAndUnmanagedOverwrites() throws Exception {
        ManagedClientResourcePack pack = new ManagedClientResourcePack(temporaryDirectory);
        Path invalid = temporaryDirectory.resolve("fake.png");
        Files.writeString(invalid, "not a png", StandardCharsets.UTF_8);

        assertEquals(ManagedClientResourcePack.ImportCode.INVALID_CONTENT, pack.importFile(
                invalid, new ResourceLocation("example", "textures/fake.png"),
                ManagedClientResourcePack.ImportKind.TEXTURE, false
        ).code());
        assertEquals(ManagedClientResourcePack.ImportCode.INVALID_TARGET, pack.importFile(
                invalid, new ResourceLocation("example", "textures/../escape.png"),
                ManagedClientResourcePack.ImportKind.TEXTURE, false
        ).code());

        Path source = png("valid.png", Color.CYAN);
        Path unmanaged = pack.root().resolve("assets/example/textures/manual.png");
        Files.createDirectories(unmanaged.getParent());
        Files.writeString(unmanaged, "owned by user", StandardCharsets.UTF_8);
        ManagedClientResourcePack.ImportResult overwrite = pack.importFile(
                source, new ResourceLocation("example", "textures/manual.png"),
                ManagedClientResourcePack.ImportKind.TEXTURE, false
        );
        assertEquals(ManagedClientResourcePack.ImportCode.UNMANAGED_TARGET, overwrite.code());
        assertEquals("owned by user", Files.readString(unmanaged, StandardCharsets.UTF_8));
    }

    @Test
    void reimportUpdatesManifestAndAuditDetectsChangedFile() throws Exception {
        ManagedClientResourcePack pack = new ManagedClientResourcePack(temporaryDirectory);
        ResourceLocation id = new ResourceLocation("example", "textures/reimport.png");
        ManagedClientResourcePack.ImportResult first = pack.importFile(
                png("first.png", Color.RED), id, ManagedClientResourcePack.ImportKind.TEXTURE, false
        );
        ManagedClientResourcePack.ImportResult second = pack.importFile(
                png("second.png", Color.BLUE), id, ManagedClientResourcePack.ImportKind.TEXTURE, false
        );
        assertTrue(first.successful());
        assertTrue(second.successful());
        assertFalse(first.sha256().equals(second.sha256()));

        Files.writeString(pack.root().resolve(second.asset()), "changed", StandardCharsets.UTF_8);
        ManagedClientResourcePack.AuditResult audit = pack.audit();
        assertTrue(audit.valid());
        assertEquals(1, audit.changed());
        assertFalse(pack.verifiedHashes(ManagedClientResourcePack.ImportKind.TEXTURE).containsKey(id));
    }

    @Test
    void importedConfiguredItemIconGeneratesModelOverrideAndRejectsDuplicateModelData() throws Exception {
        ManagedClientResourcePack pack = new ManagedClientResourcePack(temporaryDirectory);
        ResourceLocation texture = new ResourceLocation("example", "textures/item/talent_shard.png");
        assertTrue(pack.importFile(
                png("shard.png", Color.GREEN), texture,
                ManagedClientResourcePack.ImportKind.TEXTURE, false
        ).successful());

        ManagedClientResourcePack.IconBindingResult bound = pack.bindConfiguredItemIcon(
                new ResourceLocation("example", "talent_shard"), 1001, texture
        );

        assertTrue(bound.successful());
        JsonObject itemModel = json(pack.root().resolve(
                "assets/example/models/item/talent_editor_configured/talent_shard.json"
        ));
        assertEquals("example:item/talent_shard",
                itemModel.getAsJsonObject("textures").get("layer0").getAsString());
        JsonObject carrier = json(pack.root().resolve(
                "assets/talent_editor/models/item/configured_item.json"
        ));
        assertEquals(1001, carrier.getAsJsonArray("overrides").get(0).getAsJsonObject()
                .getAsJsonObject("predicate").get("custom_model_data").getAsInt());
        assertFalse(pack.bindConfiguredItemIcon(
                new ResourceLocation("example", "another_item"), 1001, texture
        ).successful());

        ManagedClientResourcePack.IconBindingResult systemBound = pack.bindConfiguredItemIcon(
                new ResourceLocation("talent_editor", "talent_book"), 1001, texture
        );
        assertTrue(systemBound.successful());
        JsonObject bookModel = json(pack.root().resolve(
                "assets/talent_editor/models/item/talent_book.json"
        ));
        assertEquals("minecraft:item/writable_book",
                bookModel.getAsJsonObject("textures").get("layer0").getAsString());
        assertEquals(1001, bookModel.getAsJsonArray("overrides").get(0).getAsJsonObject()
                .getAsJsonObject("predicate").get("custom_model_data").getAsInt());
        assertEquals(1, json(pack.root().resolve(
                "assets/talent_editor/models/item/configured_item.json"
        )).getAsJsonArray("overrides").size());
    }

    private Path png(String name, Color color) throws Exception {
        return image(name, "png", BufferedImage.TYPE_INT_ARGB, color);
    }

    private Path image(String name, String format, int type, Color color) throws Exception {
        Path target = temporaryDirectory.resolve(name);
        BufferedImage image = new BufferedImage(2, 2, type);
        image.setRGB(0, 0, color.getRGB());
        image.setRGB(1, 1, color.darker().getRGB());
        assertTrue(ImageIO.write(image, format, target.toFile()));
        return target;
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
