package com.cat2bug.admin.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudPushSupportTest {

    @Test
    void partIdsAndLegacySelection() {
        assertEquals("doc-12", CloudPushSupport.docPartId(12));
        assertEquals("git-app", CloudPushSupport.gitPartId("app"));
        assertTrue(CloudPushSupport.isLegacySelection(null, null));
        assertFalse(CloudPushSupport.isLegacySelection(List.of(), null));
        assertFalse(CloudPushSupport.isLegacySelection(null, List.of()));
    }

    @Test
    void zipEstimateWeightsJsonMoreThanFiles() {
        assertEquals(0L, CloudPushSupport.estimateZipBytes(0, 0));
        assertEquals(1_000_000L, CloudPushSupport.estimateZipBytes(0, 1_000_000));
        long jsonHeavy = CloudPushSupport.estimateZipBytes(10_000_000, 0);
        assertTrue(jsonHeavy < 2_500_000L);
        assertTrue(jsonHeavy >= 64L);
        assertEquals(Math.round(1_000_000 * CloudPushSupport.ZIP_JSON_RATIO) + 500_000L,
                CloudPushSupport.estimateZipBytes(1_000_000, 500_000));
    }

    @Test
    void sizeGuardAndRetryFilter() {
        assertFalse(CloudPushSupport.wouldExceed(100, 50, 150));
        assertTrue(CloudPushSupport.wouldExceed(100, 51, 150));
        assertFalse(CloudPushSupport.wouldExceed(150, 0, 150));
        assertTrue(CloudPushSupport.wouldExceed(151, 0, 150));
        assertFalse(CloudPushSupport.shouldUpload("OK", false));
        assertFalse(CloudPushSupport.shouldUpload("OK", true));
        assertTrue(CloudPushSupport.shouldUpload("FAILED", true));
        assertTrue(CloudPushSupport.shouldUpload("FAILED", false));
        assertFalse(CloudPushSupport.shouldUpload("PENDING", true));
        assertTrue(CloudPushSupport.shouldUpload("PENDING", false));
        assertFalse(CloudPushSupport.shouldUpload("PACKING", true));
        assertFalse(CloudPushSupport.shouldUpload("UPLOADING", true));
        assertFalse(CloudPushSupport.shouldUpload(null, true));
        assertFalse(CloudPushSupport.shouldUpload(null, false));
        assertEquals(4, CloudPushSupport.MAX_UPLOAD_ATTEMPTS);
        assertEquals(1000L, CloudPushSupport.backoffMillis(1));
        assertEquals(2000L, CloudPushSupport.backoffMillis(2));
        assertEquals(4000L, CloudPushSupport.backoffMillis(3));
    }

    @Test
    void readsSessionIdFromAjaxBody() {
        assertEquals("sid-1", CloudPushMultipartClient.readSessionId(
                "{\"code\":200,\"data\":{\"sessionId\":\"sid-1\"}}", "fallback"));
        assertEquals("push-9", CloudPushMultipartClient.readSessionId(
                "{\"code\":200,\"data\":{\"projectPushId\":\"push-9\"}}", "fallback"));
        assertEquals("fallback", CloudPushMultipartClient.readSessionId("not-json", "fallback"));
        assertEquals("HTTP 413：超过上限", CloudPushMultipartClient.errorDetail(413, "{\"msg\":\"超过上限\"}"));
    }
}
