package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysTeam;
import com.cat2bug.common.domain.SysTeamPlan;
import com.cat2bug.common.domain.TeamQuotaInfo;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysTeamMapper;
import com.cat2bug.system.mapper.SysTeamPlanMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class SysTeamPlanService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final long DEFAULT_MAX_PROJECT_BYTES = 1_073_741_824L;

    @Inject
    SysTeamPlanMapper sysTeamPlanMapper;

    @Inject
    SysTeamMapper sysTeamMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    public List<SysTeamPlan> listAll() {
        return sysTeamPlanMapper.selectAll();
    }

    public SysTeamPlan getById(Long planId) {
        SysTeamPlan plan = sysTeamPlanMapper.selectByPlanId(planId);
        if (plan == null) {
            throw new ApiException(404, "团队套餐不存在");
        }
        return plan;
    }

    /** 解析团队有效套餐：显式绑定 → 系统默认 → 内置默认值。 */
    public SysTeamPlan resolvePlanForTeam(Long teamId) {
        if (teamId == null) {
            throw new ApiException("团队不能为空");
        }
        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(teamId);
        if (team == null) {
            throw new ApiException(404, "团队不存在");
        }
        if (team.getPlanId() != null) {
            SysTeamPlan bound = sysTeamPlanMapper.selectByPlanId(team.getPlanId());
            if (bound != null) {
                return bound;
            }
        }
        SysTeamPlan def = sysTeamPlanMapper.selectSystemDefault();
        if (def != null) {
            return def;
        }
        return builtinOrdinaryPlan();
    }

    public TeamQuotaInfo getQuotaInfo(Long teamId) {
        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(teamId);
        if (team == null) {
            throw new ApiException(404, "团队不存在");
        }
        SysTeamPlan plan = resolvePlanForTeam(teamId);
        int members = team.getMemberCount();
        int projects = sysProjectMapper.countActiveByTeamId(teamId);
        TeamQuotaInfo info = new TeamQuotaInfo();
        info.setTeamId(teamId);
        info.setPlanId(plan.getPlanId());
        info.setPlanName(plan.getPlanName());
        info.setMaxMembers(nz(plan.getMaxMembers(), 10));
        info.setMaxProjects(nz(plan.getMaxProjects(), 20));
        info.setMaxProjectBytes(plan.getMaxProjectBytes() != null ? plan.getMaxProjectBytes() : DEFAULT_MAX_PROJECT_BYTES);
        info.setMemberCount(members);
        info.setProjectCount(projects);
        info.setMemberRemaining(Math.max(0, info.getMaxMembers() - members));
        info.setProjectRemaining(Math.max(0, info.getMaxProjects() - projects));
        return info;
    }

    public void assertCanAddMembers(Long teamId, int addCount) {
        TeamQuotaInfo q = getQuotaInfo(teamId);
        if (q.getMemberCount() + addCount > q.getMaxMembers()) {
            throw new ApiException(400, "已达团队最大人数限制（" + q.getMaxMembers() + "），请升级团队套餐或移除成员后再试");
        }
    }

    public void assertCanCreateProject(Long teamId) {
        TeamQuotaInfo q = getQuotaInfo(teamId);
        if (q.getProjectCount() >= q.getMaxProjects()) {
            throw new ApiException(400, "已达团队最大项目数限制（" + q.getMaxProjects() + "），请升级团队套餐后再试");
        }
    }

    public void assertCopyWithinProjectBytes(Long teamId, long estimatedBytes) {
        SysTeamPlan plan = resolvePlanForTeam(teamId);
        long max = plan.getMaxProjectBytes() != null ? plan.getMaxProjectBytes() : DEFAULT_MAX_PROJECT_BYTES;
        if (estimatedBytes > max) {
            throw new ApiException(400, "复制内容超出项目最大空间（" + max + " 字节），请减少勾选项");
        }
    }

    @Transactional
    public SysTeamPlan insert(SysTeamPlan plan, String operator) {
        validatePlan(plan, true);
        String now = LocalDateTime.now().format(TIME_FMT);
        plan.setCreateBy(operator);
        plan.setCreateTime(now);
        if (plan.getSystemDefault() == null) {
            plan.setSystemDefault(false);
        }
        if (Boolean.TRUE.equals(plan.getSystemDefault())) {
            clearOtherDefaults(null);
        }
        if (sysTeamPlanMapper.insert(plan) != 1) {
            throw new ApiException("创建团队套餐失败");
        }
        return plan;
    }

    @Transactional
    public int update(SysTeamPlan plan, String operator) {
        if (plan.getPlanId() == null) {
            throw new ApiException("套餐不能为空");
        }
        getById(plan.getPlanId());
        validatePlan(plan, false);
        plan.setUpdateBy(operator);
        plan.setUpdateTime(LocalDateTime.now().format(TIME_FMT));
        if (Boolean.TRUE.equals(plan.getSystemDefault())) {
            clearOtherDefaults(plan.getPlanId());
        }
        return sysTeamPlanMapper.update(plan);
    }

    @Transactional
    public int bindTeamPlan(Long teamId, Long planId) {
        getById(planId);
        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(teamId);
        if (team == null) {
            throw new ApiException(404, "团队不存在");
        }
        SysTeam update = new SysTeam();
        update.setTeamId(teamId);
        update.setPlanId(planId);
        return sysTeamMapper.updateSysTeam(update);
    }

    private void clearOtherDefaults(Long keepPlanId) {
        for (SysTeamPlan p : sysTeamPlanMapper.selectAll()) {
            if (Boolean.TRUE.equals(p.getSystemDefault())
                    && (keepPlanId == null || !keepPlanId.equals(p.getPlanId()))) {
                SysTeamPlan clear = new SysTeamPlan();
                clear.setPlanId(p.getPlanId());
                clear.setSystemDefault(false);
                sysTeamPlanMapper.update(clear);
            }
        }
    }

    private void validatePlan(SysTeamPlan plan, boolean creating) {
        if (creating && (plan.getPlanName() == null || plan.getPlanName().isBlank())) {
            throw new ApiException("套餐名称不能为空");
        }
        if (plan.getMaxMembers() != null && plan.getMaxMembers() < 1) {
            throw new ApiException("团队最大人数至少为 1");
        }
        if (plan.getMaxProjects() != null && plan.getMaxProjects() < 1) {
            throw new ApiException("团队最大项目数至少为 1");
        }
        if (plan.getMaxProjectBytes() != null && plan.getMaxProjectBytes() < 1) {
            throw new ApiException("项目最大空间至少为 1 字节");
        }
    }

    private static SysTeamPlan builtinOrdinaryPlan() {
        SysTeamPlan p = new SysTeamPlan();
        p.setPlanId(1L);
        p.setPlanName("普通团队");
        p.setMaxMembers(10);
        p.setMaxProjects(20);
        p.setMaxProjectBytes(DEFAULT_MAX_PROJECT_BYTES);
        p.setSystemDefault(true);
        return p;
    }

    private static int nz(Integer v, int d) {
        return v != null ? v : d;
    }
}
