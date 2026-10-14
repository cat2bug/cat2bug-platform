package com.cat2bug.quarkus.system.open;

import com.cat2bug.quarkus.common.domain.SysModule;
import com.cat2bug.quarkus.common.domain.open.ApiDeliverable;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.system.mapper.OpenApiDeliverableMapper;
import com.cat2bug.quarkus.system.service.SysModuleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class OpenApiDeliverableService {

    @Inject
    OpenApiDeliverableMapper openApiDeliverableMapper;

    @Inject
    SysModuleService sysModuleService;

    public List<ApiDeliverable> selectApiDeliverableList(Long projectId, ApiDeliverable apiDeliverable) {
        Map<Long, ApiDeliverable> allPathApiDeliverableMap = openApiDeliverableMapper
                .selectApiDeliverablePathList(projectId)
                .stream()
                .collect(Collectors.toMap(ApiDeliverable::getDeliverableId, d -> d));
        return openApiDeliverableMapper.selectApiDeliverableList(projectId, apiDeliverable).stream()
                .map(d -> {
                    if (allPathApiDeliverableMap.containsKey(d.getDeliverableId())) {
                        d.setDeliverablePath(allPathApiDeliverableMap.get(d.getDeliverableId()).getDeliverablePath());
                    }
                    return d;
                })
                .collect(Collectors.toList());
    }

    public List<ApiDeliverable> selectApiDeliverablePathList(Long projectId) {
        return openApiDeliverableMapper.selectApiDeliverablePathList(projectId);
    }

    public List<ApiDeliverable> selectApiDeliverableTreeFlat(Long projectId) {
        return openApiDeliverableMapper.selectApiDeliverableTreeFlat(projectId);
    }

    public Optional<Long> resolveToModuleId(Long projectId, String deliverablePathOrName) {
        if (StringUtils.isBlank(deliverablePathOrName)) {
            return Optional.empty();
        }
        List<ApiDeliverable> pathList = openApiDeliverableMapper.selectApiDeliverablePathList(projectId);
        return ApiDeliverablePathMatch.find(pathList, deliverablePathOrName)
                .map(ApiDeliverable::getDeliverableId);
    }

    public long resolveParentModulePid(Long projectId, String parentDeliverablePathOrName) {
        if (StringUtils.isBlank(parentDeliverablePathOrName)) {
            return 0L;
        }
        return resolveToModuleId(projectId, parentDeliverablePathOrName).orElse(-1L);
    }

    public Optional<ApiDeliverable> buildApiViewByPathOrName(Long projectId, String deliverablePathOrName) {
        Optional<Long> moduleIdOpt = resolveToModuleId(projectId, deliverablePathOrName);
        if (moduleIdOpt.isEmpty()) {
            return Optional.empty();
        }
        Long moduleId = moduleIdOpt.get();
        SysModule module = sysModuleService.selectSysModuleByModuleId(moduleId);
        if (module == null || !projectId.equals(module.getProjectId())) {
            return Optional.empty();
        }
        List<ApiDeliverable> pathList = openApiDeliverableMapper.selectApiDeliverablePathList(projectId);
        String path = ApiDeliverablePathMatch.find(pathList, deliverablePathOrName)
                .map(ApiDeliverable::getDeliverablePath)
                .orElse(module.getModuleName());
        ApiDeliverable out = new ApiDeliverable();
        out.setDeliverableName(module.getModuleName());
        out.setDeliverablePath(path);
        out.setRemark(module.getRemark());
        out.setChildrenCount(openApiDeliverableMapper.countModuleChildren(projectId, moduleId));
        return Optional.of(out);
    }
}
