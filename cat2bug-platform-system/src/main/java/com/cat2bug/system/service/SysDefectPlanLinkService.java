package com.cat2bug.system.service;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysDefectPlanLink;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysDefectPlanLinkMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

@ApplicationScoped
public class SysDefectPlanLinkService {

    public static final String LINK_TYPE_FS = "FS";

    @Inject
    SysDefectPlanLinkMapper planLinkMapper;

    @Inject
    SysDefectService sysDefectService;

    @Inject
    AuthContext authContext;

    public List<SysDefectPlanLink> listByProject(Long projectId) {
        requireProjectId(projectId);
        return planLinkMapper.selectByProjectId(projectId);
    }

    public List<SysDefectPlanLink> listByProjectAndDefectIds(Long projectId, List<Long> defectIds) {
        requireProjectId(projectId);
        if (defectIds == null || defectIds.isEmpty()) {
            return List.of();
        }
        return planLinkMapper.selectByProjectAndDefectIds(projectId, defectIds);
    }

    @Transactional
    public SysDefectPlanLink create(SysDefectPlanLink body) {
        LoginUser loginUser = requireLoginUser();
        if (body == null) {
            throw new ApiException(400, "依赖参数无效");
        }
        Long fromId = body.getFromDefectId();
        Long toId = body.getToDefectId();
        if (fromId == null || toId == null) {
            throw new ApiException(400, "前置与后继缺陷不能为空");
        }
        if (Objects.equals(fromId, toId)) {
            throw new ApiException(400, "不能依赖自身");
        }
        String linkType = body.getLinkType() == null || body.getLinkType().isBlank()
                ? LINK_TYPE_FS
                : body.getLinkType().trim().toUpperCase();
        if (!LINK_TYPE_FS.equals(linkType)) {
            throw new ApiException(400, "当前仅支持 FS 依赖");
        }
        SysDefect from = sysDefectService.selectSysDefectByDefectId(fromId);
        SysDefect to = sysDefectService.selectSysDefectByDefectId(toId);
        if (from.getProjectId() == null || !from.getProjectId().equals(to.getProjectId())) {
            throw new ApiException(400, "依赖两端须属于同一项目");
        }
        Long projectId = from.getProjectId();
        if (body.getProjectId() != null && !body.getProjectId().equals(projectId)) {
            throw new ApiException(400, "项目与缺陷不匹配");
        }
        List<SysDefectPlanLink> existing = planLinkMapper.selectByProjectId(projectId);
        for (SysDefectPlanLink e : existing) {
            if (Objects.equals(e.getFromDefectId(), fromId)
                    && Objects.equals(e.getToDefectId(), toId)
                    && LINK_TYPE_FS.equals(e.getLinkType())) {
                throw new ApiException(400, "依赖已存在");
            }
        }
        if (wouldCreateCycle(existing, fromId, toId)) {
            throw new ApiException(400, "添加该依赖会形成环");
        }
        SysDefectPlanLink row = new SysDefectPlanLink();
        row.setProjectId(projectId);
        row.setFromDefectId(fromId);
        row.setToDefectId(toId);
        row.setLinkType(LINK_TYPE_FS);
        row.setCreateBy(loginUser.getUsername());
        row.setCreateTime(new Date());
        planLinkMapper.insert(row);
        return row;
    }

    @Transactional
    public void delete(Long linkId) {
        requireLoginUser();
        if (linkId == null) {
            throw new ApiException(400, "依赖ID无效");
        }
        SysDefectPlanLink existing = planLinkMapper.selectByLinkId(linkId);
        if (existing == null) {
            throw new ApiException(404, "依赖不存在");
        }
        /* 触达权限：能查到缺陷即可删（与编辑同权限由 Resource 注解约束） */
        sysDefectService.selectSysDefectByDefectId(existing.getFromDefectId());
        planLinkMapper.deleteByLinkId(linkId);
    }

    /** 若已存在 B→…→A，再加 A→B 则成环。 */
    static boolean wouldCreateCycle(List<SysDefectPlanLink> existing, Long fromId, Long toId) {
        Map<Long, List<Long>> adj = new HashMap<>();
        for (SysDefectPlanLink e : existing) {
            if (e == null || e.getFromDefectId() == null || e.getToDefectId() == null) continue;
            adj.computeIfAbsent(e.getFromDefectId(), k -> new ArrayList<>()).add(e.getToDefectId());
        }
        /* 从 to 出发能否走到 from */
        Set<Long> seen = new HashSet<>();
        Queue<Long> q = new ArrayDeque<>();
        q.add(toId);
        seen.add(toId);
        while (!q.isEmpty()) {
            Long cur = q.poll();
            if (Objects.equals(cur, fromId)) {
                return true;
            }
            List<Long> next = adj.get(cur);
            if (next == null) continue;
            for (Long n : next) {
                if (n != null && seen.add(n)) {
                    q.add(n);
                }
            }
        }
        return false;
    }

    private void requireProjectId(Long projectId) {
        if (projectId == null) {
            throw new ApiException(400, "项目ID不能为空");
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
