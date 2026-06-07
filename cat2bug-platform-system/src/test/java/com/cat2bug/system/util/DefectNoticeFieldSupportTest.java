package com.cat2bug.system.util;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.system.domain.SysProjectDefectFieldColumnLayout;
import com.cat2bug.system.mapper.SysCaseMapper;
import com.cat2bug.system.service.ISysProjectDefectFieldService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefectNoticeFieldSupportTest {

    @Mock
    private ISysProjectDefectFieldService projectDefectFieldService;

    @Mock
    private SysCaseMapper sysCaseMapper;

    @InjectMocks
    private DefectNoticeFieldSupport support;

    private SysDefect defect;
    private SysProjectDefectFieldColumnLayout layout;

    @Before
    public void setUp() {
        defect = new SysDefect();
        defect.setProjectId(1L);
        defect.setDefectType(SysDefectTypeEnum.BUG);
        defect.setDefectState(SysDefectStateEnum.REJECTED);
        defect.setDefectLevel("middle");
        defect.setModuleName("模块A");
        defect.setRejectCount(2);

        layout = new SysProjectDefectFieldColumnLayout();
        layout.setEnabledBuiltinFieldKeys(Arrays.asList(
                "defectType", "defectLevel", "defectState", "moduleId", "moduleVersion"));
        layout.setOrderedEnabledFieldKeys(Arrays.asList(
                "moduleVersion", "defectState", "defectType", "defectLevel", "moduleId"));
        layout.setCustomFields(Collections.emptyList());

        when(projectDefectFieldService.selectColumnLayoutByProjectId(1L)).thenReturn(layout);
        when(sysCaseMapper.selectSysCaseByCaseId(anyLong())).thenReturn(null);
    }

    @Test
    public void buildFieldLines_respectsConfiguredOrderAndSkipsDescribe() {
        List<DefectNoticeFieldSupport.FieldLine> lines = support.buildFieldLines(defect);
        List<String> labels = lines.stream()
                .map(DefectNoticeFieldSupport.FieldLine::getLabel)
                .collect(Collectors.toList());

        int moduleVersionIdx = labels.indexOf(MessageUtils.message("defect.builtin-field.moduleVersion"));
        int defectStateIdx = labels.indexOf(MessageUtils.message("defect.builtin-field.defectState"));
        int defectTypeIdx = labels.indexOf(MessageUtils.message("defect.builtin-field.defectType"));

        assertTrue(moduleVersionIdx >= 0);
        assertTrue(defectStateIdx > moduleVersionIdx);
        assertTrue(defectTypeIdx > defectStateIdx);
        assertTrue(labels.stream().noneMatch(l -> MessageUtils.message("defect.builtin-field.defectDescribe").equals(l)));
    }

    @Test
    public void buildFieldLines_appendsLifeTimeAndRejectExtras() {
        List<DefectNoticeFieldSupport.FieldLine> lines = support.buildFieldLines(defect);
        DefectNoticeFieldSupport.FieldLine last = lines.get(lines.size() - 1);
        DefectNoticeFieldSupport.FieldLine secondLast = lines.get(lines.size() - 2);

        assertEquals("2", last.getValue());
        assertEquals(MessageUtils.message("reject"), last.getLabel());
        assertEquals(MessageUtils.message("defect.life-time"), secondLast.getLabel());
        assertTrue(secondLast.getValue() != null);
    }
}
