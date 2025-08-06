package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.TreeEntity;

import java.util.List;

/**
 * 模块对象 sys_module
 * 
 * @author yuzhantao
 * @date 2023-11-26
 */
public class SysModule extends TreeEntity
{
    private static final long serialVersionUID = 1L;

    /** 模块id */
    private Long moduleId;

    /** 父模块id */
    @Excel(name = "父模块id")
    private Long modulePid;

    /** 模块名称 */
    @Excel(name = "模块名称")
    private String moduleName;

    /** 批量添加时，传递用的模块名称 */
    private List<String> batchModuleNames;

    /** 项目id */
    @Excel(name = "项目id")
    private Long projectId;

    /** 子菜单的数量 */
    private int childrenCount;

    /** 菜单路径 */
    private String modulePath;

    /** 附件集合 */
    private String annexUrls;

    public void setModuleId(Long moduleId) 
    {
        this.moduleId = moduleId;
    }

    public Long getModuleId() 
    {
        return moduleId;
    }
    public void setModulePid(Long modulePid) 
    {
        this.modulePid = modulePid;
    }

    public Long getModulePid() 
    {
        return modulePid;
    }
    public void setModuleName(String moduleName) 
    {
        this.moduleName = moduleName;
    }

    public String getModuleName() 
    {
        return moduleName;
    }
    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(String modulePath) {
        this.modulePath = modulePath;
    }

    public List<String> getBatchModuleNames() {
        return batchModuleNames;
    }

    public void setBatchModuleNames(List<String> batchModuleNames) {
        this.batchModuleNames = batchModuleNames;
    }

    public String getAnnexUrls() {
        return annexUrls;
    }

    public void setAnnexUrls(String annexUrls) {
        this.annexUrls = annexUrls;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("moduleId", getModuleId())
            .append("modulePid", getModulePid())
            .append("moduleName", getModuleName())
            .append("remark", getRemark())
            .append("projectId", getProjectId())
            .append("annexUrls", getAnnexUrls())
            .toString();
    }
}
