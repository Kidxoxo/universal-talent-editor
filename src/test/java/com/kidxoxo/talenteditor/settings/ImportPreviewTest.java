package com.kidxoxo.talenteditor.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.zip.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class ImportPreviewTest {
    @TempDir Path game;
    private Path archive(String... names)throws IOException {
        Path path=game.resolve("talent_editor_exports/preview.zip");Files.createDirectories(path.getParent());
        try(var zip=new ZipOutputStream(Files.newOutputStream(path))){for(String name:names){zip.putNextEntry(new ZipEntry(name));zip.write("{}".getBytes());zip.closeEntry();}}
        return path;
    }
    @Test void previewCountsDefinitionsAndPinsExactArchive()throws Exception {
        var path=archive("datapacks/talent_editor_generated/data/test/talent_editor/talents/one.json","datapacks/talent_editor_generated/data/test/talent_editor/layouts/one.json","resourcepacks/talent_editor_generated/assets/test/textures/a.png");
        var preview=EditorSettingsService.inspectImportArchive(game,path.toString());
        assertEquals(1,preview.getAsJsonObject("counts").get("talents").getAsInt());assertEquals(1,preview.getAsJsonObject("counts").get("layouts").getAsInt());assertEquals(1,preview.get("resources").getAsInt());
        assertEquals(path,EditorSettingsService.resolveConfirmedArchive(game,preview.toString()));
        archive("datapacks/talent_editor_generated/data/test/talent_editor/talents/two.json");
        assertThrows(IOException.class,()->EditorSettingsService.resolveConfirmedArchive(game,preview.toString()));
    }
    @Test void previewRejectsUnsafeArchivePaths()throws Exception {
        var path=archive("../escape.json");assertThrows(IOException.class,()->EditorSettingsService.inspectImportArchive(game,path.toString()));
    }
    @Test void malformedConfirmationIsRejected() {
        assertThrows(IOException.class,()->EditorSettingsService.resolveConfirmedArchive(game,"{\"path\":\"missing\"}"));
    }
}
