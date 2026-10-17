package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

public class SysPlan {

    private String planId;
    private Long planNumber;
    private String planName;
    private String planVersion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planEndTime;
    private Long createById;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Long updateById;
    private String updateBy;
    private String updateByAvatar;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private Long projectId;
    private String remark;
    private int itemTotal;
    private int passCount;
    private int failCount;
    private int unexecutedCount;
    private int defectProcessingStateCount;
    private int defectAuditStateCount;
    private int defectRejectedStateCount;
    private int defectCloseStateCount;
    private int defectLevelUrgentCount;
    private int defectLevelHeightCount;
    private int defectLevelMiddleCount;
    private int defectLevelLowCount;
    private int defectCount;
    private int moduleCount;
    private int createDefectCountByTester;
    private int createDefectCountByOutsider;
    private int defectRestartCount;
    private int defectHistoryPassCount;
    private long defectUseHourTime;
    private List<SysPlanItem> sysPlanItemList;

    public int getExecutedCount() {
        return this.itemTotal - this.unexecutedCount;
    }

    public String getDefectDiscoveryRate() {
        if (this.itemTotal == 0) {
            return "0%";
        }
        return rate((double) (this.defectCount * 100) / (double) this.itemTotal);
    }

    public String getDefectRepairRate() {
        if (this.defectCount == 0) {
            return "0%";
        }
        return rate((double) (this.defectCloseStateCount * 100) / (double) this.defectCount);
    }

    public String getDefectDensity() {
        if (this.moduleCount == 0) {
            return "0";
        }
        return Math.floor(this.defectCount * 100.0 / this.moduleCount) / 100 + "";
    }

    public String getDefectDetectionRate() {
        if ((this.createDefectCountByOutsider + this.createDefectCountByTester) == 0) {
            return "0%";
        }
        return rate((double) (this.createDefectCountByTester * 100)
                / (double) (this.createDefectCountByOutsider + this.createDefectCountByTester));
    }

    public String getDefectSeverityRate() {
        if (this.defectCount == 0) {
            return "0%";
        }
        return rate((double) (this.defectLevelUrgentCount * 100) / (double) this.defectCount);
    }

    public String getDefectRestartRate() {
        if (this.defectHistoryPassCount == 0) {
            return "0%";
        }
        return rate((double) (this.defectRestartCount * 100) / (double) this.defectHistoryPassCount);
    }

    public String getDefectEscapeRate() {
        if (this.defectCount == 0) {
            return "0%";
        }
        return rate((double) (this.createDefectCountByOutsider * 100) / (double) this.defectCount);
    }

    public String getDefectRepairAvgHour() {
        if (this.defectCount == 0) {
            return "0";
        }
        return Math.floor(this.defectUseHourTime * 100.0 / this.defectCount) / 100 + "";
    }

    private String rate(double val) {
        return String.format("%.2f", Math.round(val * 100) * 0.01d) + "%";
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Long getPlanNumber() {
        return planNumber;
    }

    public void setPlanNumber(Long planNumber) {
        this.planNumber = planNumber;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanVersion() {
        return planVersion;
    }

    public void setPlanVersion(String planVersion) {
        this.planVersion = planVersion;
    }

    public Date getPlanStartTime() {
        return planStartTime;
    }

    public void setPlanStartTime(Date planStartTime) {
        this.planStartTime = planStartTime;
    }

    public Date getPlanEndTime() {
        return planEndTime;
    }

    public void setPlanEndTime(Date planEndTime) {
        this.planEndTime = planEndTime;
    }

    public Long getCreateById() {
        return createById;
    }

    public void setCreateById(Long createById) {
        this.createById = createById;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateById() {
        return updateById;
    }

    public void setUpdateById(Long updateById) {
        this.updateById = updateById;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getUpdateByAvatar() {
        return updateByAvatar;
    }

    public void setUpdateByAvatar(String updateByAvatar) {
        this.updateByAvatar = updateByAvatar;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(int itemTotal) {
        this.itemTotal = itemTotal;
    }

    public int getPassCount() {
        return passCount;
    }

    public void setPassCount(int passCount) {
        this.passCount = passCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public int getUnexecutedCount() {
        return unexecutedCount;
    }

    public void setUnexecutedCount(int unexecutedCount) {
        this.unexecutedCount = unexecutedCount;
    }

    public int getDefectProcessingStateCount() {
        return defectProcessingStateCount;
    }

    public void setDefectProcessingStateCount(int defectProcessingStateCount) {
        this.defectProcessingStateCount = defectProcessingStateCount;
    }

    public int getDefectAuditStateCount() {
        return defectAuditStateCount;
    }

    public void setDefectAuditStateCount(int defectAuditStateCount) {
        this.defectAuditStateCount = defectAuditStateCount;
    }

    public int getDefectRejectedStateCount() {
        return defectRejectedStateCount;
    }

    public void setDefectRejectedStateCount(int defectRejectedStateCount) {
        this.defectRejectedStateCount = defectRejectedStateCount;
    }

    public int getDefectCloseStateCount() {
        return defectCloseStateCount;
    }

    public void setDefectCloseStateCount(int defectCloseStateCount) {
        this.defectCloseStateCount = defectCloseStateCount;
    }

    public int getDefectLevelUrgentCount() {
        return defectLevelUrgentCount;
    }

    public void setDefectLevelUrgentCount(int defectLevelUrgentCount) {
        this.defectLevelUrgentCount = defectLevelUrgentCount;
    }

    public int getDefectLevelHeightCount() {
        return defectLevelHeightCount;
    }

    public void setDefectLevelHeightCount(int defectLevelHeightCount) {
        this.defectLevelHeightCount = defectLevelHeightCount;
    }

    public int getDefectLevelMiddleCount() {
        return defectLevelMiddleCount;
    }

    public void setDefectLevelMiddleCount(int defectLevelMiddleCount) {
        this.defectLevelMiddleCount = defectLevelMiddleCount;
    }

    public int getDefectLevelLowCount() {
        return defectLevelLowCount;
    }

    public void setDefectLevelLowCount(int defectLevelLowCount) {
        this.defectLevelLowCount = defectLevelLowCount;
    }

    public int getDefectCount() {
        return defectCount;
    }

    public void setDefectCount(int defectCount) {
        this.defectCount = defectCount;
    }

    public int getModuleCount() {
        return moduleCount;
    }

    public void setModuleCount(int moduleCount) {
        this.moduleCount = moduleCount;
    }

    public int getCreateDefectCountByTester() {
        return createDefectCountByTester;
    }

    public void setCreateDefectCountByTester(int createDefectCountByTester) {
        this.createDefectCountByTester = createDefectCountByTester;
    }

    public int getCreateDefectCountByOutsider() {
        return createDefectCountByOutsider;
    }

    public void setCreateDefectCountByOutsider(int createDefectCountByOutsider) {
        this.createDefectCountByOutsider = createDefectCountByOutsider;
    }

    public int getDefectRestartCount() {
        return defectRestartCount;
    }

    public void setDefectRestartCount(int defectRestartCount) {
        this.defectRestartCount = defectRestartCount;
    }

    public int getDefectHistoryPassCount() {
        return defectHistoryPassCount;
    }

    public void setDefectHistoryPassCount(int defectHistoryPassCount) {
        this.defectHistoryPassCount = defectHistoryPassCount;
    }

    public long getDefectUseHourTime() {
        return defectUseHourTime;
    }

    public void setDefectUseHourTime(long defectUseHourTime) {
        this.defectUseHourTime = defectUseHourTime;
    }

    public List<SysPlanItem> getSysPlanItemList() {
        return sysPlanItemList;
    }

    public void setSysPlanItemList(List<SysPlanItem> sysPlanItemList) {
        this.sysPlanItemList = sysPlanItemList;
    }
}
