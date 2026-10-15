package com.kidxoxo.talenteditor.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PackInstallTransactionTest {
    @TempDir Path temporary;

    @TestFactory List<DynamicTest> restoresBothTargetsAtEveryInstallFailure() {
        return List.of("prepare-server", "backup-server", "install-server",
                "prepare-client", "backup-client", "install-client").stream()
                .map(fault -> DynamicTest.dynamicTest(fault, () -> {
                    Fixture f = new Fixture(temporary.resolve(fault), fault, "");
                    IOException error = assertThrows(IOException.class, f::install);
                    assertTrue(f.transaction.rollback(error));
                    assertEquals("old-server", Files.readString(f.server));
                    assertEquals("old-client", Files.readString(f.client));
                    assertEquals(fault, error.getMessage());
                })).toList();
    }

    @Test void removesNewTargetIfNoOldPackExisted() throws Exception {
        Fixture f = new Fixture(temporary, "install-client", "");
        Files.delete(f.server);
        IOException error = assertThrows(IOException.class, f::install);
        assertTrue(f.transaction.rollback(error));
        assertFalse(Files.exists(f.server));
        assertEquals("old-client", Files.readString(f.client));
    }

    @Test void restoreFailureKeepsBackupAndStillRestoresOtherTarget() throws Exception {
        Fixture f = new Fixture(temporary, "install-client", "restore-client");
        IOException error = assertThrows(IOException.class, f::install);
        assertFalse(f.transaction.rollback(error));
        assertEquals("install-client", error.getMessage());
        assertEquals("restore-client", error.getSuppressed()[0].getMessage());
        assertEquals("old-client", Files.readString(f.client.resolveSibling("client.bak")));
        assertEquals("old-server", Files.readString(f.server));
        assertEquals(List.of("restore-client", "restore-server"), f.restores);
        f.restoreFault = "";
        assertTrue(f.transaction.rollback(error));
        assertEquals("old-client", Files.readString(f.client));
    }

    @Test void successfulInstallRetainsBothBackupsForReloadRollback() throws Exception {
        Fixture f = new Fixture(temporary, "", "");
        f.install();
        assertEquals("new-server", Files.readString(f.server));
        assertEquals("new-client", Files.readString(f.client));
        assertTrue(f.transaction.rollback(new IOException("reload")));
        assertEquals("old-server", Files.readString(f.server));
        assertEquals("old-client", Files.readString(f.client));
    }

    private static final class Fixture implements PackInstallTransaction.Files {
        final Path server, client, serverSource, clientSource;
        final String fault;
        String restoreFault;
        final List<String> restores = new ArrayList<>();
        final PackInstallTransaction transaction = new PackInstallTransaction(this);
        Fixture(Path root, String fault, String restoreFault) throws IOException {
            Files.createDirectories(root);
            server=root.resolve("server");client=root.resolve("client");
            serverSource=root.resolve("server-source");clientSource=root.resolve("client-source");
            Files.writeString(server,"old-server");Files.writeString(client,"old-client");
            Files.writeString(serverSource,"new-server");Files.writeString(clientSource,"new-client");
            this.fault=fault;this.restoreFault=restoreFault;
        }
        void install() throws IOException {transaction.replace(serverSource,server);transaction.replace(clientSource,client);}
        void fail(String phase,Path target) throws IOException {
            String point=phase+"-"+target.getFileName();
            if(point.equals(fault)||point.equals(restoreFault))throw new IOException(point);
        }
        public void prepare(Path target) throws IOException {fail("prepare",target);}
        public Path backup(Path target) throws IOException {
            fail("backup",target);if(!Files.exists(target))return null;
            return Files.move(target,target.resolveSibling(target.getFileName()+".bak"));
        }
        public void install(Path source,Path target) throws IOException {fail("install",target);Files.move(source,target);}
        public void restore(Path target,Path backup) throws IOException {
            restores.add("restore-"+target.getFileName());fail("restore",target);
            Files.deleteIfExists(target);if(backup!=null)Files.move(backup,target);
        }
    }
}
