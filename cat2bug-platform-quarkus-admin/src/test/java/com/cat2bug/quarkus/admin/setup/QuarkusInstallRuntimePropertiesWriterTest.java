package com.cat2bug.quarkus.admin.setup;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuarkusInstallRuntimePropertiesWriterTest {

    @Test
    void relativeH2FileUrlBecomesAbsolute() {
        String expectedPath = Path.of("data/cat2bug_platform").toAbsolutePath().normalize().toString().replace('\\', '/');
        assertEquals("jdbc:h2:file:" + expectedPath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;",
                QuarkusInstallRuntimePropertiesWriter.absolutizeH2FileUrl(
                        "jdbc:h2:file:./data/cat2bug_platform;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"));
    }

    @Test
    void absoluteAndNonFileUrlsAreUnchanged() {
        String abs = "jdbc:h2:file:/var/lib/cat2bug/db;MODE=MySQL;";
        assertEquals(abs, QuarkusInstallRuntimePropertiesWriter.absolutizeH2FileUrl(abs));
        String home = "jdbc:h2:file:~/cat2bug;MODE=MySQL;";
        assertEquals(home, QuarkusInstallRuntimePropertiesWriter.absolutizeH2FileUrl(home));
        String mysql = "jdbc:mysql://127.0.0.1:3306/cat2bug";
        assertEquals(mysql, QuarkusInstallRuntimePropertiesWriter.absolutizeH2FileUrl(mysql));
        assertEquals(null, QuarkusInstallRuntimePropertiesWriter.absolutizeH2FileUrl(null));
    }
}
