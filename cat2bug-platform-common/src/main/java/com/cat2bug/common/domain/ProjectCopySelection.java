package com.cat2bug.common.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 新建项目时的复制勾选。
 * 源码 / 用例 / 交付物 / 文档支持「全部」或 ID 列表；勾选用例时服务端会补齐所需交付物。
 * targetTeamId 只用于预估对比目标团队的项目空间上限，不参与复制内容。
 */
public class ProjectCopySelection {

    private boolean copyAllCases;
    private boolean copyAllModules;
    private boolean copyAllDocuments;
    private boolean copyAllMembers;
    private List<Long> caseIds = new ArrayList<>();
    private List<Long> moduleIds = new ArrayList<>();
    private List<Long> documentIds = new ArrayList<>();
    private List<Long> memberIds = new ArrayList<>();
    private boolean copyAi;
    private boolean copyMcp;
    /** 第三方应用 systemCode：ding / feishu / wechat */
    private List<String> imSystemCodes = new ArrayList<>();
    private List<String> gitSlugs = new ArrayList<>();
    private Long sourceProjectId;
    private Long targetTeamId;
    private long estimatedBytes;

    /** 调用预估与开跑前都应先归一。 */
    public void normalize() {
        if (gitSlugs == null) {
            gitSlugs = new ArrayList<>();
        }
        if (caseIds == null) {
            caseIds = new ArrayList<>();
        }
        if (moduleIds == null) {
            moduleIds = new ArrayList<>();
        }
        if (documentIds == null) {
            documentIds = new ArrayList<>();
        }
        if (memberIds == null) {
            memberIds = new ArrayList<>();
        }
        if (imSystemCodes == null) {
            imSystemCodes = new ArrayList<>();
        } else {
            List<String> codes = new ArrayList<>();
            for (String code : imSystemCodes) {
                if (code == null || code.isBlank()) {
                    continue;
                }
                String normalized = code.trim().toLowerCase();
                if (!codes.contains(normalized)) {
                    codes.add(normalized);
                }
            }
            imSystemCodes = codes;
        }
        if (copyAllCases) {
            caseIds = new ArrayList<>();
        }
        if (copyAllModules) {
            moduleIds = new ArrayList<>();
        }
        if (copyAllDocuments) {
            documentIds = new ArrayList<>();
        }
        if (copyAllMembers) {
            memberIds = new ArrayList<>();
        }
        // 有用例则至少要有交付物步骤（全部或由服务端按用例补祖先）
        if (wantsCases() && !wantsModules()) {
            copyAllModules = true;
        }
    }

    public boolean hasWork() {
        normalize();
        return wantsModules() || wantsCases() || wantsDocuments() || wantsMembers() || copyAi || copyMcp || wantsIm()
                || !gitSlugs.isEmpty();
    }

    public boolean wantsIm() {
        return imSystemCodes != null && !imSystemCodes.isEmpty();
    }

    public boolean wantsCases() {
        return copyAllCases || (caseIds != null && !caseIds.isEmpty());
    }

    public boolean wantsModules() {
        return copyAllModules || (moduleIds != null && !moduleIds.isEmpty());
    }

    public boolean wantsDocuments() {
        return copyAllDocuments || (documentIds != null && !documentIds.isEmpty());
    }

    public boolean wantsMembers() {
        return copyAllMembers || (memberIds != null && !memberIds.isEmpty());
    }

    public boolean isCopyAllCases() {
        return copyAllCases;
    }

    public void setCopyAllCases(boolean copyAllCases) {
        this.copyAllCases = copyAllCases;
    }

    public boolean isCopyAllModules() {
        return copyAllModules;
    }

    public void setCopyAllModules(boolean copyAllModules) {
        this.copyAllModules = copyAllModules;
    }

    public boolean isCopyAllDocuments() {
        return copyAllDocuments;
    }

    public void setCopyAllDocuments(boolean copyAllDocuments) {
        this.copyAllDocuments = copyAllDocuments;
    }

    public List<Long> getCaseIds() {
        return caseIds;
    }

    public void setCaseIds(List<Long> caseIds) {
        this.caseIds = caseIds != null ? caseIds : new ArrayList<>();
    }

    public List<Long> getModuleIds() {
        return moduleIds;
    }

    public void setModuleIds(List<Long> moduleIds) {
        this.moduleIds = moduleIds != null ? moduleIds : new ArrayList<>();
    }

    public List<Long> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<Long> documentIds) {
        this.documentIds = documentIds != null ? documentIds : new ArrayList<>();
    }

    public boolean isCopyAllMembers() {
        return copyAllMembers;
    }

    public void setCopyAllMembers(boolean copyAllMembers) {
        this.copyAllMembers = copyAllMembers;
    }

    public List<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
    }

    public boolean isCopyAi() {
        return copyAi;
    }

    public void setCopyAi(boolean copyAi) {
        this.copyAi = copyAi;
    }

    public boolean isCopyMcp() {
        return copyMcp;
    }

    public void setCopyMcp(boolean copyMcp) {
        this.copyMcp = copyMcp;
    }

    public List<String> getImSystemCodes() {
        return imSystemCodes;
    }

    public void setImSystemCodes(List<String> imSystemCodes) {
        this.imSystemCodes = imSystemCodes != null ? imSystemCodes : new ArrayList<>();
    }

    public List<String> getGitSlugs() {
        return gitSlugs;
    }

    public void setGitSlugs(List<String> gitSlugs) {
        this.gitSlugs = gitSlugs != null ? gitSlugs : new ArrayList<>();
    }

    public Long getSourceProjectId() {
        return sourceProjectId;
    }

    public void setSourceProjectId(Long sourceProjectId) {
        this.sourceProjectId = sourceProjectId;
    }

    public Long getTargetTeamId() {
        return targetTeamId;
    }

    public void setTargetTeamId(Long targetTeamId) {
        this.targetTeamId = targetTeamId;
    }

    public long getEstimatedBytes() {
        return estimatedBytes;
    }

    public void setEstimatedBytes(long estimatedBytes) {
        this.estimatedBytes = estimatedBytes;
    }
}
