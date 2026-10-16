package com.cat2bug.admin.resource;

import com.cat2bug.common.domain.SysDefect;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefectListQueryTest {

    @Test
    void bindsIndexedHandleByAndNestedParamsTheWayTheDefectPageSendsThem() {
        MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
        query.add("projectId", "277");
        query.add("handleBy[0]", "228");
        query.add("nameVersionKeyword", "登录");
        query.add("params[delFlag]", "2");
        query.add("params[defectStates]", "0,1");
        query.add("moduleId", "0");
        query.add("pageNum", "1");
        query.add("pageSize", "10");

        query.add("createByIds[0]", "228");

        SysDefect defect = DefectListQuery.bind(query);

        assertEquals(277L, defect.getProjectId());
        assertEquals(List.of(228L), defect.getHandleBy());
        assertEquals(List.of(228L), defect.getParams().get("createByIds"));
        assertEquals("登录", defect.getNameVersionKeyword());
        assertEquals(0L, defect.getModuleId());
        assertEquals(List.of(0, 1), defect.getParams().get("defectStates"));
        assertEquals("2", defect.getParams().get("delFlag"));
        assertEquals(1, DefectListQuery.pageNum(query));
        assertEquals(10, DefectListQuery.pageSize(query));
    }

    @Test
    void keepsSingleHandleByIdAndIgnoresBlankFilters() {
        MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
        query.add("handleById", "88");
        query.add("defectName", "  ");
        query.add("projectNum", "12");

        SysDefect defect = DefectListQuery.bind(query);
        Map<String, Object> params = defect.getParams();

        assertNull(defect.getDefectName());
        assertNull(defect.getHandleBy());
        assertEquals(88L, params.get("handleById"));
        assertEquals(12L, params.get("projectNum"));
    }
}
