package com.cat2bug.common.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 删除项目成员前的影响预检。
 */
public class ProjectMemberRemovePreview {

    private boolean requiresHandoff;
    private int multiHandlerDefectCount;
    private List<DefectSummary> soleHandlerDefects = new ArrayList<>();
    private List<RepoSummary> lastAdminRepositories = new ArrayList<>();
    private List<RepoSummary> otherAclRepositories = new ArrayList<>();

    public boolean isRequiresHandoff() {
        return requiresHandoff;
    }

    public void setRequiresHandoff(boolean requiresHandoff) {
        this.requiresHandoff = requiresHandoff;
    }

    public int getMultiHandlerDefectCount() {
        return multiHandlerDefectCount;
    }

    public void setMultiHandlerDefectCount(int multiHandlerDefectCount) {
        this.multiHandlerDefectCount = multiHandlerDefectCount;
    }

    public List<DefectSummary> getSoleHandlerDefects() {
        return soleHandlerDefects;
    }

    public void setSoleHandlerDefects(List<DefectSummary> soleHandlerDefects) {
        this.soleHandlerDefects = soleHandlerDefects != null ? soleHandlerDefects : new ArrayList<>();
    }

    public List<RepoSummary> getLastAdminRepositories() {
        return lastAdminRepositories;
    }

    public void setLastAdminRepositories(List<RepoSummary> lastAdminRepositories) {
        this.lastAdminRepositories = lastAdminRepositories != null ? lastAdminRepositories : new ArrayList<>();
    }

    public List<RepoSummary> getOtherAclRepositories() {
        return otherAclRepositories;
    }

    public void setOtherAclRepositories(List<RepoSummary> otherAclRepositories) {
        this.otherAclRepositories = otherAclRepositories != null ? otherAclRepositories : new ArrayList<>();
    }

    public static class DefectSummary {
        private Long defectId;
        private Long projectNum;
        private String defectName;
        private Integer defectState;

        public Long getDefectId() {
            return defectId;
        }

        public void setDefectId(Long defectId) {
            this.defectId = defectId;
        }

        public Long getProjectNum() {
            return projectNum;
        }

        public void setProjectNum(Long projectNum) {
            this.projectNum = projectNum;
        }

        public String getDefectName() {
            return defectName;
        }

        public void setDefectName(String defectName) {
            this.defectName = defectName;
        }

        public Integer getDefectState() {
            return defectState;
        }

        public void setDefectState(Integer defectState) {
            this.defectState = defectState;
        }
    }

    public static class RepoSummary {
        private Long repositoryId;
        private String name;
        private String slug;

        public Long getRepositoryId() {
            return repositoryId;
        }

        public void setRepositoryId(Long repositoryId) {
            this.repositoryId = repositoryId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }
    }
}
