package com.cat2bug.admin.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfileRemoteFileTest {

    @Test
    void buildsTheSameProfilePathOnTheConfiguredHost() {
        assertEquals(
                "https://cloud.cat2bug.com/profile/upload/2026/09/12/a.png",
                ProfileRemoteFile.uri("https://cloud.cat2bug.com/", "upload/2026/09/12/a.png").toString());
    }

    @Test
    void staysLocalWhenNoRemoteBaseIsConfigured() {
        assertNull(ProfileRemoteFile.uri(" ", "upload/a.png"));
        assertNull(ProfileRemoteFile.uri("https://cloud.cat2bug.com", "../secret"));
    }

    @Test
    void encodesSpacesAndSpecialCharsInPathSegments() {
        assertEquals(
                "https://cloud.cat2bug.com/profile/upload/2026/07/12/0%20%281%29_20260712131849A005.jpeg",
                ProfileRemoteFile.uri(
                                "https://cloud.cat2bug.com/",
                                "upload/2026/07/12/0 (1)_20260712131849A005.jpeg")
                        .toString());
    }
}
