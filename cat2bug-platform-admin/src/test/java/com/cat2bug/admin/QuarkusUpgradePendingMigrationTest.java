package com.cat2bug.admin;

import com.cat2bug.admin.setup.InstallEffectiveDataSource;
import com.cat2bug.admin.setup.SetupMigrationService;
import com.cat2bug.common.constant.FlywayConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@TestProfile(QuarkusUpgradePendingMigrationTest.NoBootstrapRestartProfile.class)
class QuarkusUpgradePendingMigrationTest {

    public static final class NoBootstrapRestartProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("cat2bug.bootstrap.mode", "false");
        }
    }

    private static final Path INSTALL_CONFIG = Path.of(
            "target/quarkus-test-install/nonexistent-application-install.yml");

    @Inject
    SetupMigrationService setupMigrationService;

    @Inject
    InstallEffectiveDataSource effectiveDataSource;

    @BeforeEach
    void writeCompletedInstallConfig() throws Exception {
        Files.createDirectories(INSTALL_CONFIG.getParent());
        Files.writeString(INSTALL_CONFIG, """
                cat2bug:
                  install:
                    completed: true
                """);
    }

    @AfterEach
    void restoreMigrationHistory() throws Exception {
        setupMigrationService.migrate("mysql", effectiveDataSource.get());
        setupMigrationService.invalidatePendingMigrationCache();
        if (Files.exists(INSTALL_CONFIG)) {
            Files.delete(INSTALL_CONFIG);
        }
    }

    @Test
    void upgradeStatusReportsPendingWhenLastMigrationRemoved() throws Exception {
        assumeTrue(deleteLatestSuccessfulMigration() != null,
                "Flyway history table has no successful migration row");
        setupMigrationService.invalidatePendingMigrationCache();

        List<String> pending = setupMigrationService.listPendingMigrations("mysql");
        assertFalse(pending.isEmpty(), "expected pending migrations after deleting latest history row");

        given()
                .when().get("/upgrade/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.upgradeRequired", equalTo(true))
                .body("data.pendingMigrations.size()", org.hamcrest.Matchers.greaterThan(0));
    }

    private Integer deleteLatestSuccessfulMigration() throws Exception {
        try (Connection connection = effectiveDataSource.get().getConnection()) {
            Integer rank = null;
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT MAX(installed_rank) FROM " + FlywayConstants.SCHEMA_HISTORY_TABLE + " WHERE success = TRUE")) {
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        rank = rs.getInt(1);
                        if (rs.wasNull()) {
                            return null;
                        }
                    }
                }
            }
            if (rank == null) {
                return null;
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM " + FlywayConstants.SCHEMA_HISTORY_TABLE + " WHERE installed_rank = ?")) {
                delete.setInt(1, rank);
                assumeTrue(delete.executeUpdate() == 1, "failed to delete latest migration history row");
            }
            return rank;
        }
    }
}
