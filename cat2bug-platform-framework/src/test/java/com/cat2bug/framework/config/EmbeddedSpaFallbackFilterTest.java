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
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/login"));
        assertFalse(EmbeddedSpaFallbackFilter.isExcluded("/system/user"));
    }

    @Test
    void hasStaticExtension_detectsAssetPaths()
    {
        assertTrue(EmbeddedSpaFallbackFilter.hasStaticExtension("/static/js/app.js"));
        assertTrue(EmbeddedSpaFallbackFilter.hasStaticExtension("/index.html"));
        assertFalse(EmbeddedSpaFallbackFilter.hasStaticExtension("/login"));
    }
}
