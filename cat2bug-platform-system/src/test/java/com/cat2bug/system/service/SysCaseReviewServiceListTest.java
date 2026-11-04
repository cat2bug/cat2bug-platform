package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysCaseReview;
import com.cat2bug.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SysCaseReviewServiceListTest {

    @Test
    void normalizeListStatusAcceptsOpenAndCompleted() {
        assertEquals("open", SysCaseReviewService.normalizeListStatus("open"));
        assertEquals("completed", SysCaseReviewService.normalizeListStatus(" completed "));
        assertNull(SysCaseReviewService.normalizeListStatus(null));
        assertNull(SysCaseReviewService.normalizeListStatus("  "));
    }

    @Test
    void normalizeListStatusRejectsUnknown() {
        assertThrows(ApiException.class, () -> SysCaseReviewService.normalizeListStatus("closed"));
    }

    @Test
    void buildListQueryTrimsFilters() {
        SysCaseReviewService service = new SysCaseReviewService();
        SysCaseReview query = service.buildListQuery(9L, "open", "2026-01-01", "2026-01-31", " tester ");
        assertEquals(9L, query.getProjectId());
        assertEquals("open", query.getStatus());
        assertEquals("2026-01-01", query.getCreateTimeBegin());
        assertEquals("2026-01-31", query.getCreateTimeEnd());
        assertEquals("tester", query.getCreateBy());
    }
}
