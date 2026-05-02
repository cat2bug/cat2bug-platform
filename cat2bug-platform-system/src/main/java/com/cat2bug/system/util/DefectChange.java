package com.cat2bug.system.util;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 缺陷字段变更记录，用于在缺陷日志中以 JSON 形式持久化"旧值 -> 新值"。
 * <p>field/type 字段供前端 update.vue 决定渲染分支；oldDisplay/newDisplay 是后端预解析后的展示文本，避免前端再去查接口。</p>
 */
@Data
@NoArgsConstructor
public class DefectChange implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 缺陷字段名（与 SysDefect 上的属性名一致），例如 defectName、handleBy。 */
    private String field;

    /** 渲染类型：text / enum / users / module / case / time / images / files / number */
    private String type;

    /** 原始旧值（id、字符串或数字） */
    private Object oldValue;

    /** 原始新值 */
    private Object newValue;

    /** 旧值的展示文本（用户名、模块名等），可空 */
    private String oldDisplay;

    /** 新值的展示文本，可空 */
    private String newDisplay;

    /**
     * type 为 images / files 时：变更中新增的条目数（便于前端用自然语言描述，避免旧版 “2 (+1/-0)” 难以理解）。
     */
    private Integer urlAddedCount;

    /** type 为 images / files 时：变更中移除的条目数 */
    private Integer urlRemovedCount;

    public DefectChange(String field, String type, Object oldValue, Object newValue) {
        this.field = field;
        this.type = type;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public DefectChange(String field, String type, Object oldValue, Object newValue,
                        String oldDisplay, String newDisplay) {
        this(field, type, oldValue, newValue);
        this.oldDisplay = oldDisplay;
        this.newDisplay = newDisplay;
    }
}
