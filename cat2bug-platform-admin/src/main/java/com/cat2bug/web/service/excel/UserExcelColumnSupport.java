package com.cat2bug.web.service.excel;
import com.cat2bug.common.core.domain.entity.SysDept;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.StringUtils;
import java.util.List;
import java.util.Map;/**
 * 用户 Excel 列定义（对齐 Spring {@code SysUser} @Excel 注解）。
 */
final class UserExcelColumnSupport {static final String EXPORT_SHEET_NAME = "用户数据";
    static final String TEMPLATE_SHEET_NAME = "用户数据";private static final Map<String, String> SEX_LABELS = Map.of("0", "男", "1", "女", "2", "未知");
    private static final Map<String, String> SEX_VALUES = Map.of("男", "0", "女", "1", "未知", "2");
    private static final Map<String, String> STATUS_LABELS = Map.of("0", "正常", "1", "停用");
    private static final Map<String, String> STATUS_VALUES = Map.of("正常", "0", "停用", "1");record ColumnDef(String header, String fieldName, boolean exportOnly) {
    }

    private static final List<ColumnDef> EXPORT_COLUMNS = List.of(
            new ColumnDef("用户序号", "userId", false),
            new ColumnDef("登录名称", "userName", false),
            new ColumnDef("用户名称", "nickName", false),
            new ColumnDef("用户邮箱", "email", false),
            new ColumnDef("手机号码", "phoneNumber", false),
            new ColumnDef("用户性别", "sex", false),
            new ColumnDef("帐号状态", "status", false),
            new ColumnDef("部门名称", "deptName", true),
            new ColumnDef("部门负责人", "deptLeader", true)
    );private static final List<ColumnDef> TEMPLATE_COLUMNS = List.of(
            new ColumnDef("部门编号", "deptId", false),
            new ColumnDef("登录名称", "userName", false),
            new ColumnDef("用户名称", "nickName", false),
            new ColumnDef("用户邮箱", "email", false),
            new ColumnDef("手机号码", "phoneNumber", false),
            new ColumnDef("用户性别", "sex", false),
            new ColumnDef("帐号状态", "status", false)
    );private UserExcelColumnSupport() {
    }

    static List<ColumnDef> exportColumns() {
        return EXPORT_COLUMNS;
    }

    static List<ColumnDef> templateColumns() {
        return TEMPLATE_COLUMNS;
    }

    static ColumnDef findByHeader(String header, List<ColumnDef> columns) {
        if (StringUtils.isBlank(header)) {
            return null;
        }
        String trimmed = header.trim();
        for (ColumnDef column : columns) {
            if (column.header().equals(trimmed)) {
                return column;
            }
        }
        return null;
    }

    static String formatExportValue(SysUser user, ColumnDef column) {
        return switch (column.fieldName()) {
            case "userId" -> user.getUserId() == null ? "" : String.valueOf(user.getUserId());
            case "userName" -> nullToEmpty(user.getUserName());
            case "nickName" -> nullToEmpty(user.getNickName());
            case "email" -> nullToEmpty(user.getEmail());
            case "phoneNumber" -> nullToEmpty(user.getPhoneNumber());
            case "sex" -> SEX_LABELS.getOrDefault(nullToEmpty(user.getSex()), nullToEmpty(user.getSex()));
            case "status" -> STATUS_LABELS.getOrDefault(nullToEmpty(user.getStatus()), nullToEmpty(user.getStatus()));
            case "deptName" -> user.getDept() == null ? "" : nullToEmpty(user.getDept().getDeptName());
            case "deptLeader" -> user.getDept() == null ? "" : nullToEmpty(user.getDept().getLeader());
            default -> "";
        };
    }

    static void applyImportValue(SysUser user, ColumnDef column, String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        switch (column.fieldName()) {
            case "deptId" -> user.setDeptId(parseLong(value));
            case "userName" -> user.setUserName(value);
            case "nickName" -> user.setNickName(value);
            case "email" -> user.setEmail(value);
            case "phoneNumber" -> user.setPhoneNumber(value);
            case "sex" -> user.setSex(parseSex(value));
            case "status" -> user.setStatus(parseStatus(value));
            default -> {
            }
        }
    }

    private static String parseSex(String value) {
        if (StringUtils.isBlank(value)) {
            return "2";
        }
        if (SEX_VALUES.containsKey(value)) {
            return SEX_VALUES.get(value);
        }
        return value;
    }

    private static String parseStatus(String value) {
        if (StringUtils.isBlank(value)) {
            return "0";
        }
        if (STATUS_VALUES.containsKey(value)) {
            return STATUS_VALUES.get(value);
        }
        return value;
    }

    private static Long parseLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
