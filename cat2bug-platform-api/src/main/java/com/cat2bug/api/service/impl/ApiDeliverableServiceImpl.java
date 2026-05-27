package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiDeliverable;
import com.cat2bug.api.mapper.ApiDeliverableMapper;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiDeliverableService;
import com.cat2bug.api.support.ApiDeliverablePathMatch;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.service.ISysModuleService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 01:09
 * @Version: 1.0.0
 */
@Service
public class ApiDeliverableServiceImpl implements IApiDeliverableService {

    @Resource
    private ApiService apiService;

    @Resource
    private ApiDeliverableMapper apiDeliverableMapper;

    @Resource
    private ISysModuleService sysModuleService;

    @Override
    public List<ApiDeliverable> selectApiDeliverableList(ApiDeliverable apiDeliverable) {
        Long projectId = this.apiService.getProjectId();
        Map<Long, ApiDeliverable> allPathApiDeliverableMap = this.apiDeliverableMapper.selectApiDeliverablePathList(projectId).
                stream().collect(Collectors.toMap(ApiDeliverable::getDeliverableId, d->d));
        return apiDeliverableMapper.selectApiDeliverableList(projectId, apiDeliverable).stream().map(d->{
            // 设置交付物全路径
            if(allPathApiDeliverableMap.containsKey(d.getDeliverableId())) {
                d.setDeliverablePath(allPathApiDeliverableMap.get(d.getDeliverableId()).getDeliverablePath());
            }
            return d;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ApiDeliverable> selectApiDeliverablePathList(Long projectId) {
        return this.apiDeliverableMapper.selectApiDeliverablePathList(projectId);
    }

    @Override
    public List<ApiDeliverable> selectApiDeliverableTreeFlat(Long projectId) {
        return this.apiDeliverableMapper.selectApiDeliverableTreeFlat(projectId);
    }

    @Override
    public Optional<Long> resolveToModuleId(Long projectId, String deliverablePathOrName) {
        if (StringUtils.isBlank(deliverablePathOrName)) {
            return Optional.empty();
        }
        List<ApiDeliverable> pathList = this.apiDeliverableMapper.selectApiDeliverablePathList(projectId);
        return ApiDeliverablePathMatch.find(pathList, deliverablePathOrName).map(ApiDeliverable::getDeliverableId);
    }

    @Override
    public long resolveParentModulePid(Long projectId, String parentDeliverablePathOrName) {
        if (StringUtils.isBlank(parentDeliverablePathOrName)) {
            return 0L;
        }
        return resolveToModuleId(projectId, parentDeliverablePathOrName).orElse(-1L);
    }

    @Override
    public Optional<ApiDeliverable> buildApiViewByPathOrName(Long projectId, String deliverablePathOrName) {
        Optional<Long> moduleIdOpt = resolveToModuleId(projectId, deliverablePathOrName);
        if (!moduleIdOpt.isPresent()) {
            return Optional.empty();
        }
        Long moduleId = moduleIdOpt.get();
        SysModule module = this.sysModuleService.selectSysModuleByModuleId(moduleId);
        if (module == null || !projectId.equals(module.getProjectId())) {
            return Optional.empty();
        }
        List<ApiDeliverable> pathList = this.apiDeliverableMapper.selectApiDeliverablePathList(projectId);
        String path = ApiDeliverablePathMatch.find(pathList, deliverablePathOrName)
                .map(ApiDeliverable::getDeliverablePath)
                .orElse(module.getModuleName());
        ApiDeliverable out = new ApiDeliverable();
        out.setDeliverableName(module.getModuleName());
        out.setDeliverablePath(path);
        out.setRemark(module.getRemark());
        out.setChildrenCount(this.apiDeliverableMapper.countModuleChildren(projectId, moduleId));
        return Optional.of(out);
    }
}
