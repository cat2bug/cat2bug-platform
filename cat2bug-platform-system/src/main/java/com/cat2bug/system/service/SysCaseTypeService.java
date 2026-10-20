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

    public List<SysCaseType> list(Long projectId) {
        return list(projectId, false);
    }

    public List<SysCaseType> list(Long projectId, boolean enabledOnly) {
        requireProject(projectId);
        List<SysCaseType> list = sysCaseTypeMapper.selectByProjectId(projectId);
        if (!enabledOnly) {
            return list;
        }
        return list.stream().filter(SysCaseTypeService::isEnabled).collect(Collectors.toList());
    }

    @Transactional
    public void seedDefaults(Long projectId) {
        if (projectId == null) {
            return;
        }
        int sort = 1;
        for (String name : DEFAULT_NAMES) {
            if (sysCaseTypeMapper.selectByProjectAndName(projectId, name) == null) {
                SysCaseType type = new SysCaseType();
                type.setProjectId(projectId);
                type.setTypeName(name);
                type.setSortOrder(sort);
                type.setEnabled(ENABLED);
                sysCaseTypeMapper.insert(type);
            }
            sort++;
        }
        String existing = sysCaseTypeMapper.selectReviewTemplate(projectId);
        if (existing == null || existing.isBlank()) {
            sysCaseTypeMapper.updateReviewTemplate(projectId, builtinReviewTemplate());
        }
    }

    public void requireBelongsToProject(Long projectId, Long typeId) {
        if (typeId == null) {
            return;
        }
        SysCaseType type = sysCaseTypeMapper.selectById(typeId);
        if (type == null || type.getProjectId() == null || !type.getProjectId().equals(projectId)) {
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
        SysCaseType type = sysCaseTypeMapper.selectById(typeId);
        if (type == null || type.getProjectId() == null || !type.getProjectId().equals(projectId)) {
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
    public SysCaseType create(Long projectId, String typeName) {
        requireProject(projectId);
        String name = normalizeName(typeName);
        if (sysCaseTypeMapper.selectByProjectAndName(projectId, name) != null) {
            throw new ApiException("类型名称已存在");
        }
        SysCaseType type = new SysCaseType();
        type.setProjectId(projectId);
        type.setTypeName(name);
        type.setSortOrder(nextSort(projectId));
        type.setEnabled(ENABLED);
        sysCaseTypeMapper.insert(type);
        return type;
    }

    @Transactional
    public void rename(Long projectId, Long typeId, String typeName) {
        SysCaseType existing = requireOwned(projectId, typeId);
        String name = normalizeName(typeName);
        SysCaseType same = sysCaseTypeMapper.selectByProjectAndName(projectId, name);
        if (same != null && !same.getTypeId().equals(existing.getTypeId())) {
            throw new ApiException("类型名称已存在");
        }
        existing.setTypeName(name);
        sysCaseTypeMapper.updateName(existing);
    }

    @Transactional
    public void setEnabled(Long projectId, Long typeId, Integer enabled) {
        SysCaseType existing = requireOwned(projectId, typeId);
        int value = normalizeEnabled(enabled);
        existing.setEnabled(value);
        sysCaseTypeMapper.updateEnabled(existing);
    }

    @Transactional
    public void delete(Long projectId, Long typeId) {
        requireOwned(projectId, typeId);
        int used = sysCaseTypeMapper.countCases(projectId, typeId);
        if (used > 0) {
            throw new ApiException("仍有 " + used + " 条用例使用该类型");
        }
        sysCaseTypeMapper.deleteById(typeId);
    }

    public String reviewTemplate(Long projectId) {
        requireProject(projectId);
        String stored = sysCaseTypeMapper.selectReviewTemplate(projectId);
        if (stored == null || stored.isBlank()) {
            return builtinReviewTemplate();
        }
        return stored;
    }

    @Transactional
    public void saveReviewTemplate(Long projectId, String template) {
        requireProject(projectId);
        String value = template == null || template.isBlank() ? null : template;
        sysCaseTypeMapper.updateReviewTemplate(projectId, value);
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

    private SysCaseType requireOwned(Long projectId, Long typeId) {
        requireProject(projectId);
        if (typeId == null) {
            throw new ApiException("类型不存在");
        }
        SysCaseType type = sysCaseTypeMapper.selectById(typeId);
        if (type == null || !projectId.equals(type.getProjectId())) {
            throw new ApiException("类型不存在");
        }
        return type;
    }

    private int nextSort(Long projectId) {
        List<SysCaseType> list = sysCaseTypeMapper.selectByProjectId(projectId);
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

    private static void requireProject(Long projectId) {
        if (projectId == null) {
            throw new ApiException("项目不能为空");
        }
    }
}
