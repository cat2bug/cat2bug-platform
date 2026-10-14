package com.cat2bug.admin.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedSpaFallbackFilterTest {

    @Test
    void shouldServeSpa_servesPostOnlySpaPathsWithoutHtmlAccept() {
        assertTrue(EmbeddedSpaFallbackFilter.shouldServeSpa(null, "/login"));
        assertTrue(EmbeddedSpaFallbackFilter.shouldServeSpa(null, "/register"));
        assertFalse(EmbeddedSpaFallbackFilter.shouldServeSpa(null, "/system/user/list"));
    }

    @Test
    void isExcluded_skipsApiAndAssetPaths() {
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/static/js/app.js"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/captchaImage"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/system/user/list"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/api/project/list"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/q/health"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/login"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/register"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/setup"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/git/p/1/demo.git/info/refs"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/acme/proj/code/demo.git/git-upload-pack"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/acme/proj/code/demo"));
    }

    @Test
    void hasStaticExtension_detectsAssetPaths() {
        assertTrue(EmbeddedSpaFallbackFilter.hasStaticExtension("/static/js/app.js"));
        assertTrue(EmbeddedSpaFallbackFilter.hasStaticExtension("/index.html"));
        assertFalse(EmbeddedSpaFallbackFilter.hasStaticExtension("/login"));
    }
}
