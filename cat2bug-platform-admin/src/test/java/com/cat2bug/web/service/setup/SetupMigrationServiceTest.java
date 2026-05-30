package com.cat2bug.web.service.setup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupMigrationServiceTest
{
    @Test
    void containsChecksumConflict_detectsFlywayValidationMessage()
    {
        String message = """
                Validate failed: Migrations have failed validation
                Migration checksum mismatch for migration version 0.5.1
                -> Applied to database : 1393167113
                -> Resolved locally    : -1548861318
                """;
        assertTrue(SetupMigrationService.containsChecksumConflict(message));
        assertTrue(SetupMigrationService.isChecksumConflict(new RuntimeException(message)));
    }

    @Test
    void containsChecksumConflict_ignoresUnrelatedErrors()
    {
        assertFalse(SetupMigrationService.containsChecksumConflict("Connection refused"));
    }

    @Test
    void containsFailedMigration_detectsFlywayFailedEntryMessage()
    {
        assertTrue(SetupMigrationService.containsFailedMigration(
                "Schema `cat2bug_platform` contains a failed migration to version 0.6.2.1"));
        assertTrue(SetupMigrationService.isRepairableMigrationError(
                new RuntimeException("Migrations have failed validation")));
    }
}
