package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.OllamaAvailabilityService;
import com.cat2bug.quarkus.admin.service.OllamaModelService;
import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.AiAccount;
import com.cat2bug.quarkus.common.domain.SysAiModuleConfig;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.AiAccountService;
import com.cat2bug.quarkus.system.service.SysAiModuleConfigService;
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
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/system/ai")
@Produces(MediaType.APPLICATION_JSON)
public class AiResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String STATE_EMPTY = "EMPTY";
    private static final String STATE_COMPLETED = "COMPLETED";

    @Inject
    OllamaAvailabilityService ollamaAvailability;

    @Inject
    OllamaModelService ollamaModelService;

    @Inject
    AiAccountService aiAccountService;

    @Inject
    SysAiModuleConfigService sysAiModuleConfigService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions(value = {"system:ai:list", "system:case:add"},
            logical = RequiresPermissions.Logical.OR)
    public TableDataInfo list() {
        if (!ollamaAvailability.isAvailable()) {
            return emptyTable();
        }
        List<Map<String, Object>> downloaded = ollamaAvailability.listDownloadedModels();
        Set<String> downloadedNames = downloaded.stream()
                .map(row -> (String) row.get("name"))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Map<String, Object>> emptyPlaceholders = ollamaAvailability.defaultModelNames().stream()
                .filter(StringUtils::isNotEmpty)
                .filter(name -> !downloadedNames.contains(name))
                .map(name -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", name);
                    row.put("size", 0L);
                    row.put("state", STATE_EMPTY);
                    return row;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> merged = Stream.concat(downloaded.stream(), emptyPlaceholders.stream())
                .collect(Collectors.toCollection(() -> new TreeSetByName()))
                .stream()
                .collect(Collectors.toList());
        return new TableDataInfo(merged, merged.size());
    }

    @GET
    @Path("/project-model-options")
    @RequiresPermissions(value = {"system:ai:list", "system:case:add", "system:defect:add"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult projectModelOptions(@QueryParam("projectId") Long projectId) {
        List<Map<String, Object>> ollamaOpts = new ArrayList<>();
        if (ollamaAvailability.isAvailable()) {
            ollamaOpts = ollamaAvailability.listDownloadedModels().stream()
                    .filter(row -> STATE_COMPLETED.equals(row.get("state")))
                    .filter(row -> StringUtils.isNotEmpty((String) row.get("name")))
                    .map(row -> {
                        String name = (String) row.get("name");
                        Map<String, Object> option = new LinkedHashMap<>();
                        option.put("key", name);
                        option.put("label", name);
                        return option;
                    })
                    .collect(Collectors.toList());
        }
        AiAccount accQuery = new AiAccount();
        accQuery.setProjectId(projectId);
        List<Map<String, Object>> openaiOpts = aiAccountService.selectAiAccountList(accQuery).stream()
                .filter(a -> a != null && a.getAccountId() != null)
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    String key = String.valueOf(a.getAccountId());
                    row.put("key", key);
                    row.put("label", StringUtils.isNotBlank(a.getAccountName()) ? a.getAccountName() : key);
                    row.put("accountId", a.getAccountId());
                    return row;
                })
                .collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ollama", ollamaOpts);
        body.put("openai", openaiOpts);
        body.put("aiEnabled", ollamaAvailability.isAvailable());
        return AjaxResult.success(body);
    }

    @GET
    @Path("/model/list/default")
    @RequiresPermissions("system:ai:list")
    public TableDataInfo defaultModelList() {
        List<String> names = Stream.concat(
                        ollamaAvailability.defaultModelNames().stream(),
                        ollamaAvailability.configuredModels().stream())
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        return new TableDataInfo(names, names.size());
    }

    @POST
    @Path("/model")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:ai:add")
    public AjaxResult downloadModel(AiModuleRequest request) {
        String name = request != null ? request.getName() : null;
        return AjaxResult.success(ollamaModelService.pullModule(name, true));
    }

    @DELETE
    @Path("/model")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:ai:remove")
    public AjaxResult removeModel(AiModuleRequest request) {
        String name = request != null ? request.getName() : null;
        return ollamaModelService.removeModule(name) ? AjaxResult.success() : AjaxResult.error("删除模型失败");
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:ai:export")
    public Response export(@RestForm("projectId") Long projectId) {
        SysAiModuleConfig query = new SysAiModuleConfig();
        query.setProjectId(projectId);
        byte[] workbook = systemExcelExportService.exportAiModuleConfigs(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{aiId}")
    @RequiresPermissions("system:ai:query")
    public AjaxResult getInfo(@PathParam("aiId") Long aiId) {
        return AjaxResult.success(sysAiModuleConfigService.selectSysAiModuleConfigByAiId(aiId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:ai:add")
    public AjaxResult add(SysAiModuleConfig sysAiModuleConfig) {
        return toAjax(sysAiModuleConfigService.insertSysAiModuleConfig(sysAiModuleConfig));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:ai:edit")
    public AjaxResult edit(SysAiModuleConfig sysAiModuleConfig) {
        return toAjax(sysAiModuleConfigService.updateSysAiModuleConfig(sysAiModuleConfig));
    }

    @DELETE
    @Path("/{aiIds}")
    @RequiresPermissions("system:ai:remove")
    public AjaxResult removeConfig(@PathParam("aiIds") String aiIds) {
        Long[] ids = Arrays.stream(aiIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
        return toAjax(sysAiModuleConfigService.deleteSysAiModuleConfigByAiIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }

    private static TableDataInfo emptyTable() {
        return new TableDataInfo(List.of(), 0);
    }

    /**
     * 按模型名去重，与 Spring TreeSet(Comparator.comparing(AiModule::getName)) 行为一致。
     */
    private static final class TreeSetByName extends LinkedHashSet<Map<String, Object>> {
        @Override
        public boolean add(Map<String, Object> row) {
            String name = row == null ? null : (String) row.get("name");
            if (StringUtils.isEmpty(name)) {
                return false;
            }
            removeIf(existing -> name.equals(existing.get("name")));
            return super.add(row);
        }
    }

    public static class AiModuleRequest {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
