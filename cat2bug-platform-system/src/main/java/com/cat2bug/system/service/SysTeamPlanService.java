package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysTeam;
import com.cat2bug.common.domain.SysTeamPlan;
import com.cat2bug.common.domain.TeamQuotaInfo;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.MessageUtils;
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
    /** 普通团队 / 回落默认：每项目最大空间 5GiB。 */
    public static final long DEFAULT_MAX_PROJECT_BYTES = 5_368_709_120L;
    /** 种子套餐库内固定中文名，用于识别后做展示国际化。 */
    public static final String SEED_ORDINARY_PLAN_NAME = "普通团队";

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
            throw new ApiException(404, MessageUtils.message("team.plan.not-found"));
        }
        return plan;
    }

    /** 系统默认套餐（种子「普通团队」）；无标记时回落内置 plan_id=1。 */
    public SysTeamPlan resolveSystemDefaultPlan() {
        SysTeamPlan def = sysTeamPlanMapper.selectSystemDefault();
        if (def != null && def.getPlanId() != null) {
            return def;
        }
        SysTeamPlan byId = sysTeamPlanMapper.selectByPlanId(1L);
        return byId != null ? byId : builtinOrdinaryPlan();
    }

    public Long resolveSystemDefaultPlanId() {
        return resolveSystemDefaultPlan().getPlanId();
    }

    /** 解析团队有效套餐：显式绑定 → 系统默认 → 内置默认值。 */
    public SysTeamPlan resolvePlanForTeam(Long teamId) {
        if (teamId == null) {
            throw new ApiException(MessageUtils.message("team.plan.team-required"));
        }
        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(teamId);
        if (team == null) {
            throw new ApiException(404, MessageUtils.message("team.plan.team-not-found"));
        }
        if (team.getPlanId() != null) {
            SysTeamPlan bound = sysTeamPlanMapper.selectByPlanId(team.getPlanId());
            if (bound != null) {
                return bound;
            }
        }
        return resolveSystemDefaultPlan();
    }

    public TeamQuotaInfo getQuotaInfo(Long teamId) {
        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(teamId);
        if (team == null) {
            throw new ApiException(404, MessageUtils.message("team.plan.team-not-found"));
        }
        SysTeamPlan plan = resolvePlanForTeam(teamId);
        int members = team.getMemberCount();
        int projects = sysProjectMapper.countActiveByTeamId(teamId);
        TeamQuotaInfo info = new TeamQuotaInfo();
        info.setTeamId(teamId);
        info.setPlanId(plan.getPlanId());
        info.setPlanName(localizeSeedPlanName(plan.getPlanName()));
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
            throw new ApiException(400, MessageUtils.message("team.plan.quota.members", q.getMaxMembers()));
        }
    }

    public void assertCanCreateProject(Long teamId) {
        TeamQuotaInfo q = getQuotaInfo(teamId);
        if (q.getProjectCount() >= q.getMaxProjects()) {
            throw new ApiException(400, MessageUtils.message("team.plan.quota.projects", q.getMaxProjects()));
        }
    }

    public void assertCopyWithinProjectBytes(Long teamId, long estimatedBytes) {
        SysTeamPlan plan = resolvePlanForTeam(teamId);
        long max = plan.getMaxProjectBytes() != null ? plan.getMaxProjectBytes() : DEFAULT_MAX_PROJECT_BYTES;
        if (estimatedBytes > max) {
            throw new ApiException(400, MessageUtils.message("team.plan.quota.project-bytes", max));
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
            throw new ApiException(MessageUtils.message("team.plan.create-failed"));
        }
        return plan;
    }

    @Transactional
    public int update(SysTeamPlan plan, String operator) {
        if (plan.getPlanId() == null) {
            throw new ApiException(MessageUtils.message("team.plan.id-required"));
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
    public int delete(Long planId) {
        SysTeamPlan plan = getById(planId);
        if (Boolean.TRUE.equals(plan.getSystemDefault())) {
            throw new ApiException(400, MessageUtils.message("team.plan.system-default-undeletable"));
        }
        int bound = sysTeamPlanMapper.countTeamsByPlanId(planId);
        if (bound > 0) {
            throw new ApiException(400, MessageUtils.message("team.plan.in-use", bound));
        }
        return sysTeamPlanMapper.deleteByPlanId(planId);
    }

    @Transactional
    public int bindTeamPlan(Long teamId, Long planId) {
        getById(planId);
        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(teamId);
        if (team == null) {
            throw new ApiException(404, MessageUtils.message("team.plan.team-not-found"));
        }
        SysTeam update = new SysTeam();
        update.setTeamId(teamId);
        update.setPlanId(planId);
        return sysTeamMapper.updateSysTeam(update);
    }

    /** 种子套餐名按当前请求语言翻译；自定义名称原样返回。 */
    public static String localizeSeedPlanName(String planName) {
        if (SEED_ORDINARY_PLAN_NAME.equals(planName)) {
            return MessageUtils.message("team.plan.ordinary");
        }
        return planName;
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
            throw new ApiException(MessageUtils.message("team.plan.name-required"));
        }
        if (plan.getMaxMembers() != null && plan.getMaxMembers() < 1) {
            throw new ApiException(MessageUtils.message("team.plan.max-members-min"));
        }
        if (plan.getMaxProjects() != null && plan.getMaxProjects() < 1) {
            throw new ApiException(MessageUtils.message("team.plan.max-projects-min"));
        }
        if (plan.getMaxProjectBytes() != null && plan.getMaxProjectBytes() < 1) {
            throw new ApiException(MessageUtils.message("team.plan.max-project-bytes-min"));
        }
    }

    private static SysTeamPlan builtinOrdinaryPlan() {
        SysTeamPlan p = new SysTeamPlan();
        p.setPlanId(1L);
        p.setPlanName(SEED_ORDINARY_PLAN_NAME);
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
