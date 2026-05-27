package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiMember;
import com.cat2bug.api.mapper.ApiProjectApiMapper;
import com.cat2bug.api.service.IApiProjectService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.system.domain.SysProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * API项目信息Controller
 *
 * @author yuzhantao
 * @date 2026-04-09
 */
@RestController
@RequestMapping("/api/project")
public class ApiProjectController extends BaseController {

    private static final String AUTH_TOKEN_HEADER_NAME = "CAT2BUG-API-KEY";

    @Autowired
    private IApiProjectService apiProjectService;

    @Autowired
    private ApiProjectApiMapper apiProjectApiMapper;

    /**
     * 获取项目信息（包含统计信息）
     */
    @GetMapping
    public AjaxResult getInfo(HttpServletRequest request) {
        // 从请求头获取 API KEY
        String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);
        if (apiKey == null || apiKey.isEmpty()) {
            return AjaxResult.error("API KEY 不能为空");
        }

        // 根据 API KEY 获取项目 ID
        Long projectId = apiProjectApiMapper.selectProjectIdByApiId(apiKey);
        if (projectId == null) {
            return AjaxResult.error("无效的 API KEY");
        }

        // 获取项目信息
        SysProject project = apiProjectService.selectProjectById(projectId);
        if (project == null) {
            return AjaxResult.error("项目不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("projectName", project.getProjectName());
        result.put("projectIntroduce", project.getProjectIntroduce());
        result.put("createTime", project.getCreateTime());

        // 获取创建成员信息
        ApiMember creator = apiProjectService.selectCreatorByProjectId(projectId);
        if (creator != null) {
            Map<String, Object> creatorInfo = new HashMap<>();
            creatorInfo.put("memberAccount", creator.getMemberAccount());
            creatorInfo.put("memberName", creator.getMemberName());
            creatorInfo.put("phoneNumber", creator.getPhoneNumber());
            creatorInfo.put("email", creator.getEmail());
            result.put("creator", creatorInfo);
        }

        // 获取统计信息
        Map<String, Object> statistics = apiProjectService.getProjectStatistics(projectId);
        result.put("statistics", statistics);

        return AjaxResult.success(result);
    }
}
