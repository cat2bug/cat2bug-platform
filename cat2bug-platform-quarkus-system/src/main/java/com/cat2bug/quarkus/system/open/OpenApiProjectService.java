package com.cat2bug.quarkus.system.open;

import com.cat2bug.quarkus.common.domain.SysProject;
import com.cat2bug.quarkus.common.domain.open.ApiMember;
import com.cat2bug.quarkus.system.mapper.OpenApiProjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class OpenApiProjectService {

    @Inject
    OpenApiProjectMapper openApiProjectMapper;

    public Map<String, Object> buildProjectInfo(Long projectId) {
        SysProject project = openApiProjectMapper.selectProjectById(projectId);
        Map<String, Object> result = new LinkedHashMap<>();
        if (project == null) {
            return result;
        }
        result.put("projectName", project.getProjectName());
        result.put("projectIntroduce", project.getProjectIntroduce());
        result.put("createTime", project.getCreateTime());

        ApiMember creator = openApiProjectMapper.selectCreatorByProjectId(projectId);
        if (creator != null) {
            Map<String, Object> creatorInfo = new LinkedHashMap<>();
            creatorInfo.put("memberAccount", creator.getMemberAccount());
            creatorInfo.put("memberName", creator.getMemberName());
            creatorInfo.put("phoneNumber", creator.getPhoneNumber());
            creatorInfo.put("email", creator.getEmail());
            result.put("creator", creatorInfo);
        }
        result.put("statistics", openApiProjectMapper.getProjectStatistics(projectId));
        return result;
    }
}
