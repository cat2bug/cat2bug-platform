package com.cat2bug.system.service.impl.report.parse;

import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.service.ISysCaseService;
import com.cat2bug.system.service.ISysPlanService;
import com.cat2bug.system.service.ISysProjectService;
import com.cat2bug.system.service.IReportParseService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Report DSL materialization: inline tokens in tables, styled images, plan variables.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportDescriptionMaterializeTest {

    @Mock
    private ISysProjectService sysProjectService;
    @Mock
    private ISysCaseService sysCaseService;
    @Mock
    private ISysPlanService sysPlanService;

    @InjectMocks
    private SysProjectImageParseImpl imageParse;
    @InjectMocks
    private SysProjectVariableParseImpl projectVariableParse;
    @InjectMocks
    private SysCaseVariableParseImpl caseVariableParse;
    @InjectMocks
    private SysPlanVariableParseImpl planVariableParse;

    private List<IReportParseService> parsers;

    @Before
    public void setUp() {
        SysProject project = new SysProject();
        project.setProjectId(58L);
        project.setProjectName("Cat2Bug-Platform");
        project.setProjectIcon("3");
        when(sysProjectService.selectSysProjectByProjectId(eq(58L))).thenReturn(project);

        when(sysCaseService.totalByProjectId(eq(58L))).thenReturn(12L);

        SysPlan plan = new SysPlan();
        plan.setPlanId("p1");
        plan.setPlanNumber(1L);
        plan.setPlanName("Sprint-1");
        plan.setPassCount(5);
        plan.setDefectCount(1);
        when(sysPlanService.selectSysPlanList(any(SysPlan.class))).thenReturn(Collections.singletonList(plan));

        parsers = Arrays.asList(
                imageParse,
                projectVariableParse,
                caseVariableParse,
                planVariableParse
        );
    }

    private String materialize(String input, Map<String, Object> params) {
        String content = input;
        Map<String, Object> safe = params == null ? Collections.emptyMap() : params;
        for (IReportParseService parser : parsers) {
            if (parser.isHandle(content)) {
                content = parser.parse(58L, content, safe);
            }
        }
        return content;
    }

    @Test
    public void materialize_inlineImageAndVariableInTableCell() {
        String input = "|col|\n|-|\n|$image[\"style\":\"width:100px;height:100px;\"]{api.project.projectIcon} ${api.project.projectName}|";
        String out = materialize(input, Collections.emptyMap());
        assertFalse(out.contains("$image"));
        assertFalse(out.contains("${"));
        assertTrue(out.contains("<img"));
        assertTrue(out.contains("width:100px;height:100px;"));
        assertTrue(out.contains("Cat2Bug-Platform"));
        assertTrue(out.contains("/static/img/project_icon3.svg"));
    }

    @Test
    public void materialize_styledImagePreservesStyle() {
        String input = "$image[\"style\":\"width:200px;height:200px;\"]{api.project.projectIcon}";
        String out = imageParse.parse(58L, input, Collections.emptyMap());
        assertTrue(out.contains("<img"));
        assertTrue(out.contains("style=\"width:200px;height:200px;\""));
        assertFalse(out.contains("$image"));
        assertFalse(out.contains("$s"));
    }

    @Test
    public void materialize_planVariableByPlanNumber() {
        String input = "通过数: ${api.plan.passCount[1]} 缺陷: ${api.plan.defectCount[1]}";
        String out = planVariableParse.parse(58L, input, Collections.emptyMap());
        assertEquals("通过数: 5 缺陷: 1", out);
    }

    @Test
    public void materialize_caseTotal() {
        String input = "用例总数 ${api.case.total}";
        String out = caseVariableParse.parse(58L, input, Collections.emptyMap());
        assertEquals("用例总数 12", out);
    }

    @Test
    public void containsReportDsl_detectsTokens() {
        assertTrue(ReportParseUtils.containsReportDsl("x $image{api.project.projectIcon} y"));
        assertTrue(ReportParseUtils.containsReportDsl("${api.project.projectName}"));
        assertFalse(ReportParseUtils.containsReportDsl("<img src=\"/a.png\" />"));
    }

    @Test
    public void materialize_withBaseApiPrefixForUploadPath() {
        SysProject project = new SysProject();
        project.setProjectId(58L);
        project.setProjectIcon("/profile/upload/icon.png");
        when(sysProjectService.selectSysProjectByProjectId(eq(58L))).thenReturn(project);
        Map<String, Object> params = new HashMap<>();
        params.put("vueAppBaseApi", "http://localhost:2020");
        String out = imageParse.parse(58L, "$image{api.project.projectIcon}", params);
        assertTrue(out.contains("src=\"http://localhost:2020/profile/upload/icon.png\""));
    }

    @Test
    public void materialize_blankProjectIconFallsBackToDefaultInsteadOfWiping() {
        SysProject project = new SysProject();
        project.setProjectId(58L);
        project.setProjectIcon(null);
        when(sysProjectService.selectSysProjectByProjectId(eq(58L))).thenReturn(project);
        String out = imageParse.parse(58L,
                "$image[\"style\":\"width:100px;height:100px;\"]{api.project.projectIcon}",
                Collections.emptyMap());
        assertTrue(out.contains("<img"));
        assertTrue(out.contains("project_icon1.svg"));
        assertFalse(out.contains("$image"));
    }

    @Test
    public void materialize_preservesHashedStaticIconPath() {
        SysProject project = new SysProject();
        project.setProjectId(58L);
        project.setProjectIcon("/static/img/project_icon1.204904ae.svg");
        when(sysProjectService.selectSysProjectByProjectId(eq(58L))).thenReturn(project);
        String out = imageParse.parse(58L, "$image{api.project.projectIcon}", Collections.emptyMap());
        assertTrue(out.contains("src=\"/static/img/project_icon1.204904ae.svg\""));
    }

    @Test
    public void materialize_prefixesStaticIconWithVueAppBaseApi() {
        SysProject project = new SysProject();
        project.setProjectId(58L);
        project.setProjectIcon("/static/img/project_icon1.204904ae.svg");
        when(sysProjectService.selectSysProjectByProjectId(eq(58L))).thenReturn(project);
        Map<String, Object> params = new HashMap<>();
        params.put("vueAppBaseApi", "/dev-api");
        String out = imageParse.parse(58L, "$image{api.project.projectIcon}", params);
        assertTrue(out.contains("src=\"/dev-api/static/img/project_icon1.204904ae.svg\""));
    }
}
