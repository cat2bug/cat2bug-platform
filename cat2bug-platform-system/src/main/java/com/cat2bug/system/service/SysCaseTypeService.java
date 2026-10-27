package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysCaseType;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysCaseTypeMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysCaseTypeService {

    public static final List<String> DEFAULT_NAMES = List.of("功能", "接口", "界面");
    public static final String BUILTIN_REVIEW_TEMPLATE_RESOURCE = "case-review/default-template.md";
    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    @Inject
    SysCaseTypeMapper sysCaseTypeMapper;

    public Long resolveTeamIdFromProject(Long projectId) {
        if (projectId == null) {
            throw new ApiException("项目不能为空");
        }
        Long teamId = sysCaseTypeMapper.selectTeamIdByProjectId(projectId);
        if (teamId == null) {
            throw new ApiException("项目不存在");
        }
        return teamId;
    }

    public List<SysCaseType> list(Long projectId) {
        return list(projectId, false);
    }

    public List<SysCaseType> list(Long projectId, boolean enabledOnly) {
        return listByTeamId(resolveTeamIdFromProject(projectId), enabledOnly);
    }

    public List<SysCaseType> listByTeamId(Long teamId) {
        return listByTeamId(teamId, false);
    }

    public List<SysCaseType> listByTeamId(Long teamId, boolean enabledOnly) {
        requireTeam(teamId);
        List<SysCaseType> list = sysCaseTypeMapper.selectByTeamId(teamId);
        if (!enabledOnly) {
            return list;
        }
        return list.stream().filter(SysCaseTypeService::isEnabled).collect(Collectors.toList());
    }

    @Transactional
    public void seedDefaultsForTeam(Long teamId) {
        if (teamId == null) {
            return;
        }
        int sort = 1;
        for (String name : DEFAULT_NAMES) {
            if (sysCaseTypeMapper.selectByTeamAndName(teamId, name) == null) {
                SysCaseType type = new SysCaseType();
                type.setTeamId(teamId);
                type.setTypeName(name);
                type.setSortOrder(sort);
                type.setEnabled(ENABLED);
                sysCaseTypeMapper.insert(type);
            }
            sort++;
        }
        String existing = sysCaseTypeMapper.selectReviewTemplate(teamId);
        if (existing == null || existing.isBlank()) {
            sysCaseTypeMapper.updateReviewTemplate(teamId, builtinReviewTemplate());
        }
    }

    /** 新建项目时：若团队尚无类型名单，则补种默认项（团队创建时已 seed 则跳过）。 */
    @Transactional
    public void ensureTeamDefaultsIfEmpty(Long projectId) {
        Long teamId = resolveTeamIdFromProject(projectId);
        if (sysCaseTypeMapper.selectByTeamId(teamId).isEmpty()) {
            seedDefaultsForTeam(teamId);
        }
    }

    public void requireBelongsToProject(Long projectId, Long typeId) {
        if (typeId == null) {
            return;
        }
        Long teamId = resolveTeamIdFromProject(projectId);
        SysCaseType type = sysCaseTypeMapper.selectById(typeId);
        if (type == null || type.getTeamId() == null || !type.getTeamId().equals(teamId)) {
            throw new ApiException("用例类型不属于当前项目");
        }
    }

    /**
     * 新建或改绑类型时要求目标类型已启用；保留当前类型时允许停用项（编辑旧用例）。
     */
    public void requireAssignable(Long projectId, Long typeId, Long currentTypeId) {
        if (typeId == null) {
            return;
        }
        Long teamId = resolveTeamIdFromProject(projectId);
        SysCaseType type = sysCaseTypeMapper.selectById(typeId);
        if (type == null || type.getTeamId() == null || !type.getTeamId().equals(teamId)) {
            throw new ApiException("用例类型不属于当前项目");
        }
        if (Objects.equals(typeId, currentTypeId)) {
            return;
        }
        if (!isEnabled(type)) {
            throw new ApiException("该类型已停用");
        }
    }

    @Transactional
    public SysCaseType create(Long teamId, String typeName) {
        requireTeam(teamId);
        String name = normalizeName(typeName);
        if (sysCaseTypeMapper.selectByTeamAndName(teamId, name) != null) {
            throw new ApiException("类型名称已存在");
        }
        SysCaseType type = new SysCaseType();
        type.setTeamId(teamId);
        type.setTypeName(name);
        type.setSortOrder(nextSort(teamId));
        type.setEnabled(ENABLED);
        sysCaseTypeMapper.insert(type);
        return type;
    }

    @Transactional
    public void rename(Long teamId, Long typeId, String typeName) {
        SysCaseType existing = requireOwned(teamId, typeId);
        String name = normalizeName(typeName);
        SysCaseType same = sysCaseTypeMapper.selectByTeamAndName(teamId, name);
        if (same != null && !same.getTypeId().equals(existing.getTypeId())) {
            throw new ApiException("类型名称已存在");
        }
        existing.setTypeName(name);
        sysCaseTypeMapper.updateName(existing);
    }

    @Transactional
    public void setEnabled(Long teamId, Long typeId, Integer enabled) {
        SysCaseType existing = requireOwned(teamId, typeId);
        int value = normalizeEnabled(enabled);
        existing.setEnabled(value);
        sysCaseTypeMapper.updateEnabled(existing);
    }

    @Transactional
    public void delete(Long teamId, Long typeId) {
        requireOwned(teamId, typeId);
        int used = sysCaseTypeMapper.countCases(teamId, typeId);
        if (used > 0) {
            throw new ApiException("仍有 " + used + " 条用例使用该类型");
        }
        sysCaseTypeMapper.deleteById(typeId);
    }

    public String reviewTemplate(Long projectId) {
        return reviewTemplateByTeam(resolveTeamIdFromProject(projectId));
    }

    public String reviewTemplateByTeam(Long teamId) {
        requireTeam(teamId);
        String stored = sysCaseTypeMapper.selectReviewTemplate(teamId);
        if (stored == null || stored.isBlank()) {
            return builtinReviewTemplate();
        }
        return stored;
    }

    @Transactional
    public void saveReviewTemplateByTeam(Long teamId, String template) {
        requireTeam(teamId);
        String value = template == null || template.isBlank() ? null : template;
        sysCaseTypeMapper.updateReviewTemplate(teamId, value);
    }

    /** 完成评审与设置页共用；资源缺失时退回最小骨架。 */
    public String builtinReviewTemplate() {
        try (InputStream in = SysCaseTypeService.class.getClassLoader()
                .getResourceAsStream(BUILTIN_REVIEW_TEMPLATE_RESOURCE)) {
            if (in == null) {
                return "# 测试用例评审纪要\n\n{{cases}}\n";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "# 测试用例评审纪要\n\n{{cases}}\n";
        }
    }

    public String projectName(Long projectId) {
        return sysCaseTypeMapper.selectProjectName(projectId);
    }

    private SysCaseType requireOwned(Long teamId, Long typeId) {
        requireTeam(teamId);
        if (typeId == null) {
            throw new ApiException("类型不存在");
        }
        SysCaseType type = sysCaseTypeMapper.selectById(typeId);
        if (type == null || !teamId.equals(type.getTeamId())) {
            throw new ApiException("类型不存在");
        }
        return type;
    }

    private int nextSort(Long teamId) {
        List<SysCaseType> list = sysCaseTypeMapper.selectByTeamId(teamId);
        int max = 0;
        for (SysCaseType type : list) {
            if (type.getSortOrder() != null && type.getSortOrder() > max) {
                max = type.getSortOrder();
            }
        }
        return max + 1;
    }

    private static String normalizeName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            throw new ApiException("类型名称不能为空");
        }
        String name = typeName.trim();
        if (name.length() > 64) {
            throw new ApiException("类型名称不能超过 64 个字符");
        }
        return name;
    }

    private static int normalizeEnabled(Integer enabled) {
        if (enabled == null) {
            throw new ApiException("启用状态不能为空");
        }
        if (enabled != ENABLED && enabled != DISABLED) {
            throw new ApiException("启用状态无效");
        }
        return enabled;
    }

    public static boolean isEnabled(SysCaseType type) {
        return type != null && Objects.equals(type.getEnabled(), ENABLED);
    }

    private static void requireTeam(Long teamId) {
        if (teamId == null) {
            throw new ApiException("团队不能为空");
        }
    }
}
