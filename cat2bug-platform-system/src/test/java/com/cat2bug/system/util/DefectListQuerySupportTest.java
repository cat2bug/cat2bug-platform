package com.cat2bug.system.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefectListQuerySupportTest {

    @Test
    public void isCustomFieldOrderColumn_detectsCustomPrefix() {
        assertTrue(DefectListQuerySupport.isCustomFieldOrderColumn("custom_bool"));
        assertTrue(DefectListQuerySupport.isCustomFieldOrderColumn("custom_my_field"));
        assertFalse(DefectListQuerySupport.isCustomFieldOrderColumn("projectNum"));
        assertFalse(DefectListQuerySupport.isCustomFieldOrderColumn(null));
        assertFalse(DefectListQuerySupport.isCustomFieldOrderColumn(""));
    }
}
