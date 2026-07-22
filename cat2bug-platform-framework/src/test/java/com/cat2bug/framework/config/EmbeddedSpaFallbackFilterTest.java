package com.cat2bug.framework.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedSpaFallbackFilterTest
{
    @Test
    void acceptsHtml_detectsBrowserNavigation()
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        assertTrue(EmbeddedSpaFallbackFilter.acceptsHtml(request));
    }

    @Test
    void acceptsHtml_rejectsJsonApiCalls()
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/user/list");
        request.addHeader("Accept", "application/json, text/plain, */*");
        assertFalse(EmbeddedSpaFallbackFilter.acceptsHtml(request));
    }

    @Test
    void isExcluded_skipsStaticAndApiPaths()
    {
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/static/js/app.js"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/captchaImage"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/html/ie.html"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/git/info/refs"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/login"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/system/user"));
    }

    @Test
    void isExcluded_skipsNamespacedGitSmartHttp()
    {
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/acme/demo/code/my-repo.git"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/acme/demo/code/my-repo.git/info/refs"));
        assertTrue(EmbeddedSpaFallbackFilter.isExcluded("/acme/demo/code/my-repo.git/git-upload-pack"));
        assertTrue(EmbeddedSpaFallbackFilter.isNamespacedGitSmartHttp("/t1/p1/code/slug.git/HEAD"));
    }

    @Test
    void isExcluded_allowsNamespacedCodeSpaPaths()
    {
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/acme/demo/code/my-repo"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/acme/demo/code/my-repo/branches"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/acme/demo/defect"));
        assertFalse(EmbeddedSpaFallbackFilter.isNamespacedGitSmartHttp("/acme/demo/code/my-repo"));
        assertFalse(EmbeddedSpaFallbackFilter.isNamespacedGitSmartHttp("/acme/demo/code/my-repo.gitx"));
        assertFalse(EmbeddedSpaFallbackFilter.isNamespacedGitSmartHttp("/code/my-repo.git"));
    }

    @Test
    void hasStaticExtension_detectsAssetPaths()
    {
        assertTrue(EmbeddedSpaFallbackFilter.hasStaticExtension("/static/js/app.js"));
        assertTrue(EmbeddedSpaFallbackFilter.hasStaticExtension("/index.html"));
        assertFalse(EmbeddedSpaFallbackFilter.hasStaticExtension("/login"));
    }
}
