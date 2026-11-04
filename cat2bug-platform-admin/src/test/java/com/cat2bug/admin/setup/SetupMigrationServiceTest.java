package com.cat2bug.admin.setup;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupMigrationServiceTest {

    @Test
    void containsChecksumConflict_detectsFlywayValidationMessage() {
        String message = """
                Validate failed: Migrations have failed validation
                Migration checksum mismatch for migration version 0.5.1
                -> Applied to database : 1393167113
                -> Resolved locally    : -1548861318
                """;
        assertTrue(SetupMigrationService.containsChecksumConflict(message));
    }

    @Test
    void containsChecksumConflict_ignoresUnrelatedErrors() {
        assertFalse(SetupMigrationService.containsChecksumConflict("Connection refused"));
    }

    @Test
    void containsFailedMigration_detectsFlywayFailedEntryMessage() {
        assertTrue(SetupMigrationService.containsFailedMigration(
                "Schema `cat2bug_platform` contains a failed migration to version 0.6.2.1"));
    }

    @Test
    void isRepairableMigrationError_detectsFailedValidation() {
        assertTrue(SetupMigrationService.isRepairableMigrationError(
                new RuntimeException("Migrations have failed validation")));
    }

    @Test
    void resolveMigrationLocations_prefersLocalFilesystemMysqlDir() {
        String[] locations = SetupMigrationService.resolveMigrationLocations(
                SetupMigrationService.MYSQL_LOCATION);
        assertTrue(locations.length >= 1);
        assertTrue(locations[0].startsWith("filesystem:") || locations[0].equals(SetupMigrationService.MYSQL_LOCATION));
        if (locations[0].startsWith("filesystem:")) {
            Path dir = Path.of(locations[0].substring("filesystem:".length()));
            assertTrue(Files.isDirectory(dir), "resolved dir should exist: " + dir);
        }
    }
}
