package com.cat2bug.admin.resource.open;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysCaseStep;
import com.cat2bug.common.domain.open.ApiCase;
import com.cat2bug.common.domain.open.ApiCaseStep;
import com.cat2bug.common.domain.open.ApiCaseWriteRequest;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.open.OpenApiCaseService;
import com.cat2bug.system.open.OpenApiContextService;
import com.cat2bug.system.open.OpenApiDeliverableService;
import com.cat2bug.system.service.SysCaseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;

@Path("/api/case")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiCaseResource {

    @Inject
    OpenApiContextService openApiContextService;

    @Inject
    OpenApiCaseService openApiCaseService;

    @Inject
    OpenApiDeliverableService openApiDeliverableService;

    @Inject
    SysCaseService sysCaseService;

    @GET
    public TableDataInfo list(@QueryParam("caseName") String caseName,
                              @QueryParam("deliverableName") String deliverableName,
                              @QueryParam("caseNum") Long caseNum) {
        Long projectId = openApiContextService.requireProjectId();
        ApiCase filter = new ApiCase();
        filter.setCaseName(caseName);
        filter.setDeliverableName(deliverableName);
        filter.setCaseNum(caseNum);
        List<ApiCase> rows = openApiCaseService.selectApiCaseList(projectId, filter);
        return new TableDataInfo(rows, rows.size());
    }

    @GET
    @Path("/{caseNum}")
    public AjaxResult getInfo(@PathParam("caseNum") Long caseNum) {
        Long projectId = openApiContextService.requireProjectId();
        ApiCase apiCase = openApiCaseService.selectApiCaseByCaseNum(projectId, caseNum);
        if (apiCase == null) {
            return AjaxResult.error("用例不存在或无权限访问");
        }
        return AjaxResult.success(apiCase);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult add(ApiCaseWriteRequest body) {
        if (body == null || StringUtils.isBlank(body.getCaseName())
                || StringUtils.isBlank(body.getCaseExpect()) || body.getCaseLevel() == null) {
            return AjaxResult.error("caseName、caseExpect、caseLevel 为必填");
        }
        Long projectId = openApiContextService.requireProjectId();
        SysCase sysCase = new SysCase();
        sysCase.setCaseName(body.getCaseName());
        sysCase.setCaseExpect(body.getCaseExpect());
        sysCase.setCaseStep(toSysCaseSteps(body.getCaseStep()));
        sysCase.setCaseLevel(body.getCaseLevel());
        sysCase.setCasePreconditions(body.getCasePreconditions());
        sysCase.setCaseData(body.getCaseData());
        sysCase.setRemark(body.getRemark());
        if (StringUtils.isNotBlank(body.getDeliverableName())) {
            Long moduleId = openApiDeliverableService.resolveToModuleId(projectId, body.getDeliverableName())
                    .orElse(null);
            if (moduleId == null) {
                return AjaxResult.error("交付物不存在: " + body.getDeliverableName());
            }
            sysCase.setModuleId(moduleId);
        }
        sysCase.setProjectId(projectId);
        SysCase result = sysCaseService.insertSysCase(sysCase);
        ApiCase view = openApiCaseService.selectApiCaseByCaseNum(projectId, result.getCaseNum());
        return AjaxResult.success(view);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult edit(ApiCaseWriteRequest body) {
        if (body == null || body.getCaseNum() == null) {
            return AjaxResult.error("caseNum 为必填（用例编号）");
        }
        Long projectId = openApiContextService.requireProjectId();
        SysCase existing = sysCaseService.selectSysCaseByCaseNum(projectId, body.getCaseNum());
        if (existing == null) {
            return AjaxResult.error("用例不存在或无权限访问");
        }
        if (body.getCaseName() != null) {
            existing.setCaseName(body.getCaseName());
        }
        if (body.getCaseExpect() != null) {
            existing.setCaseExpect(body.getCaseExpect());
        }
        if (body.getCaseStep() != null) {
            existing.setCaseStep(toSysCaseSteps(body.getCaseStep()));
        }
        if (body.getCaseLevel() != null) {
            existing.setCaseLevel(body.getCaseLevel());
        }
        if (body.getCasePreconditions() != null) {
            existing.setCasePreconditions(body.getCasePreconditions());
        }
        if (body.getCaseData() != null) {
            existing.setCaseData(body.getCaseData());
        }
        if (body.getRemark() != null) {
            existing.setRemark(body.getRemark());
        }
        if (body.getDeliverableName() != null) {
            if (StringUtils.isBlank(body.getDeliverableName())) {
                existing.setModuleId(null);
            } else {
                Long moduleId = openApiDeliverableService.resolveToModuleId(projectId, body.getDeliverableName())
                        .orElse(null);
                if (moduleId == null) {
                    return AjaxResult.error("交付物不存在: " + body.getDeliverableName());
                }
                existing.setModuleId(moduleId);
            }
        }
        sysCaseService.updateSysCase(existing);
        return AjaxResult.success(openApiCaseService.selectApiCaseByCaseNum(projectId, existing.getCaseNum()));
    }

    @DELETE
    @Path("/{caseNum}")
    public AjaxResult remove(@PathParam("caseNum") Long caseNum) {
        Long projectId = openApiContextService.requireProjectId();
        int rows = sysCaseService.deleteSysCaseByCaseNum(projectId, caseNum);
        if (rows == 0) {
            return AjaxResult.error("用例不存在或无权限访问");
        }
        return AjaxResult.success();
    }

    private static List<SysCaseStep> toSysCaseSteps(List<ApiCaseStep> steps) {
        if (steps == null) {
            return null;
        }
        return steps.stream().map(s -> {
            SysCaseStep t = new SysCaseStep();
            t.setStepDescribe(s.getStepDescribe());
            t.setStepExpect(s.getStepExpect());
            return t;
        }).collect(Collectors.toList());
    }
}
