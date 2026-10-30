package com.cat2bug.system.service;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDefectPlanBaseline;
import com.cat2bug.common.domain.SysDefectPlanBaselineItem;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysDefectPlanBaselineMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;

@ApplicationScoped
public class SysDefectPlanBaselineService {

    @Inject
    SysDefectPlanBaselineMapper baselineMapper;

    @Inject
    AuthContext authContext;

    public SysDefectPlanBaseline getActive(Long projectId) {
        requireProjectId(projectId);
        SysDefectPlanBaseline baseline = baselineMapper.selectByProjectId(projectId);
        if (baseline == null) {
            return null;
        }
        baseline.setItems(baselineMapper.selectItemsByBaselineId(baseline.getBaselineId()));
        return baseline;
    }

    public SysDefectPlanBaseline getActiveForDefectIds(Long projectId, List<Long> defectIds) {
        requireProjectId(projectId);
        SysDefectPlanBaseline baseline = baselineMapper.selectByProjectId(projectId);
        if (baseline == null) {
            return null;
        }
        if (defectIds == null || defectIds.isEmpty()) {
            baseline.setItems(List.of());
            return baseline;
        }
        baseline.setItems(baselineMapper.selectItemsByBaselineAndDefectIds(baseline.getBaselineId(), defectIds));
        return baseline;
    }

    @Transactional
    public SysDefectPlanBaseline freeze(Long projectId) {
        LoginUser loginUser = requireLoginUser();
        requireProjectId(projectId);
        List<SysDefectPlanBaselineItem> plans = baselineMapper.selectScheduledPlansByProjectId(projectId);
        Date now = new Date();
        SysDefectPlanBaseline existing = baselineMapper.selectByProjectId(projectId);
        if (existing != null) {
            baselineMapper.deleteItemsByBaselineId(existing.getBaselineId());
            existing.setFrozenBy(loginUser.getUsername());
            existing.setFrozenTime(now);
            baselineMapper.updateBaseline(existing);
            for (SysDefectPlanBaselineItem p : plans) {
                SysDefectPlanBaselineItem item = new SysDefectPlanBaselineItem();
                item.setBaselineId(existing.getBaselineId());
                item.setDefectId(p.getDefectId());
                item.setPlanStartTime(p.getPlanStartTime());
                item.setPlanEndTime(p.getPlanEndTime());
                baselineMapper.insertItem(item);
            }
            existing.setItems(baselineMapper.selectItemsByBaselineId(existing.getBaselineId()));
            return existing;
        }
        SysDefectPlanBaseline row = new SysDefectPlanBaseline();
        row.setProjectId(projectId);
        row.setFrozenBy(loginUser.getUsername());
        row.setFrozenTime(now);
        baselineMapper.insertBaseline(row);
        for (SysDefectPlanBaselineItem p : plans) {
            SysDefectPlanBaselineItem item = new SysDefectPlanBaselineItem();
            item.setBaselineId(row.getBaselineId());
            item.setDefectId(p.getDefectId());
            item.setPlanStartTime(p.getPlanStartTime());
            item.setPlanEndTime(p.getPlanEndTime());
            baselineMapper.insertItem(item);
        }
        row.setItems(baselineMapper.selectItemsByBaselineId(row.getBaselineId()));
        return row;
    }

    @Transactional
    public void clear(Long projectId) {
        requireLoginUser();
        requireProjectId(projectId);
        SysDefectPlanBaseline existing = baselineMapper.selectByProjectId(projectId);
        if (existing == null) {
            return;
        }
        baselineMapper.deleteItemsByBaselineId(existing.getBaselineId());
        baselineMapper.deleteBaselineByProjectId(projectId);
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
