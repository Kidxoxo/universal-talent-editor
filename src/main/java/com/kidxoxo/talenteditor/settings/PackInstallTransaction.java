package com.kidxoxo.talenteditor.settings;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

/** Records a target immediately after backup, before installation can fail. */
final class PackInstallTransaction {
    interface Files {
        void prepare(Path target) throws IOException;
        Path backup(Path target) throws IOException;
        void install(Path source, Path target) throws IOException;
        void restore(Path target, Path backup) throws IOException;
    }

    private final Files files;
    private final ArrayList<Replacement> replacements = new ArrayList<>();

    PackInstallTransaction(Files files) { this.files = files; }

    Path replace(Path source, Path target) throws IOException {
        files.prepare(target);
        Path backup = files.backup(target);
        replacements.add(new Replacement(target, backup));
        files.install(source, target);
        return backup;
    }

    boolean rollback(Throwable failure) {
        boolean restored = true;
        for (int index = replacements.size() - 1; index >= 0; index--) {
            Replacement replacement = replacements.get(index);
            try {
                files.restore(replacement.target(), replacement.backup());
                replacements.remove(index);
            } catch (IOException | RuntimeException restoreError) {
                failure.addSuppressed(restoreError);
                restored = false;
            }
        }
        return restored;
    }

    private record Replacement(Path target, Path backup) { }
}
