package com.cat2bug.quarkus.framework.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PageOrderTest {

    @Test
    void buildConvertsColumnAndDirectionLikeSpring() {
        assertEquals("project_num asc", PageOrder.build("projectNum", "ascending"));
        assertEquals("update_time desc", PageOrder.build("updateTime", "descending"));
        assertEquals("defect_name asc", PageOrder.build("defectName", null));
        assertEquals("case_num desc", PageOrder.build(" caseNum ", "desc"));
        assertNull(PageOrder.build(null, "ascending"));
        assertNull(PageOrder.build(" ", "ascending"));
        assertNull(PageOrder.build("custom_level", "ascending"));
    }

    @Test
    void toUnderScoreCaseMatchesSpring() {
        assertEquals("user_name", PageOrder.toUnderScoreCase("userName"));
        assertEquals("update_by_name", PageOrder.toUnderScoreCase("updateByName"));
        assertEquals("abc", PageOrder.toUnderScoreCase("abc"));
    }

    @Test
    void validationRejectsUnsafeOrTooLongInput() {
        assertNull(PageOrder.validationError(null));
        assertNull(PageOrder.validationError("d.defect_id desc, project_num asc"));
        assertEquals(PageOrder.INVALID_MESSAGE, PageOrder.validationError("defect_id'; drop table x"));
        assertEquals(PageOrder.INVALID_MESSAGE, PageOrder.validationError("sleep(1) asc"));
        assertEquals(PageOrder.TOO_LONG_MESSAGE, PageOrder.validationError("a".repeat(501)));
    }

    @Test
    void aliasesReplaceOnlyMatchingColumn() {
        String aliases = "create_member=d.create_by_id,update_by_name=u.nick_name";
        assertEquals("d.create_by_id desc", PageOrder.applyAliases("create_member desc", aliases));
        assertEquals("u.nick_name asc", PageOrder.applyAliases("update_by_name asc", aliases));
        assertEquals("create_member_name asc", PageOrder.applyAliases("create_member_name asc", aliases));
        assertNull(PageOrder.applyAliases(null, aliases));
    }
}
