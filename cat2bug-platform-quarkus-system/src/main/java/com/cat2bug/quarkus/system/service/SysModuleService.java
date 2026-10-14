package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.SysModule;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.system.mapper.SysModuleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SysModuleService {

    @Inject
    SysModuleMapper sysModuleMapper;

    public List<SysModule> selectSysModuleList(SysModule query) {
        return sysModuleMapper.selectSysModuleList(query);
    }

    public List<SysModule> selectSysModulePathList(Long projectId) {
        if (projectId == null) {
            throw new ApiException("项目不能为空");
        }
        return sysModuleMapper.selectSysModulePathList(projectId);
    }

    public SysModule selectSysModuleByModuleId(Long moduleId) {
        return sysModuleMapper.selectSysModuleByModuleId(moduleId);
    }

    public List<SysModule> selectModuleTree(SysModule query) {
        if (query.getModulePid() == null || query.getModulePid() == 0) {
            return selectSysModuleList(query);
        }
        SysModule findParentModule = query;
        List<SysModule> parentModule = new LinkedList<>();
        while (findParentModule != null) {
            findParentModule = sysModuleMapper.selectSysModuleByModuleId(findParentModule.getModulePid());
            if (findParentModule != null) {
                parentModule.add(0, findParentModule);
            }
        }
        List<SysModule> treeModule = null;
        List<SysModule> prevLayerModuleList = null;
        for (SysModule module : parentModule) {
            SysModule params = new SysModule();
            params.setModulePid(module.getModulePid());
            params.setProjectId(query.getProjectId());
            List<SysModule> children = sysModuleMapper.selectSysModuleList(params);
            if (treeModule == null) {
                treeModule = children;
            } else if (prevLayerModuleList != null && children != null && !children.isEmpty()) {
                Long parentModuleId = children.get(0).getModulePid();
                Optional<SysModule> prevParentModule = prevLayerModuleList.stream()
                        .filter(prevModule -> prevModule.getModuleId().equals(parentModuleId))
                        .findFirst();
                if (prevParentModule.isPresent()) {
                    prevParentModule.get().setChildren(children);
                    prevParentModule.get().setChildrenCount(children.size());
                }
            }
            prevLayerModuleList = children;
        }
        return treeModule != null ? treeModule : List.of();
    }

    public List<SysModule> insertSysModule(SysModule sysModule) {
        List<SysModule> ret = new ArrayList<>();
        if (sysModule.getBatchModuleNames() != null && !sysModule.getBatchModuleNames().isEmpty()) {
            for (String name : sysModule.getBatchModuleNames()) {
                SysModule module = new SysModule();
                module.setModuleName(name);
                module.setModulePid(sysModule.getModulePid());
                module.setProjectId(sysModule.getProjectId());
                if (sysModuleMapper.insertSysModule(module) <= 0) {
                    throw new ApiException("新增模块失败");
                }
                ret.add(module);
            }
        } else {
            if (sysModuleMapper.insertSysModule(sysModule) <= 0) {
                throw new ApiException("新增模块失败");
            }
            ret.add(sysModule);
        }
        return ret;
    }

    public int updateSysModule(SysModule sysModule) {
        if (sysModule.getModulePid() != null && sysModule.getModulePid().equals(sysModule.getModuleId())) {
            throw new ApiException("父模块不能是自身");
        }
        return sysModuleMapper.updateSysModule(sysModule);
    }

    public int deleteSysModuleByModuleIds(Long[] moduleIds) {
        return sysModuleMapper.deleteSysModuleByModuleIds(moduleIds);
    }
}
