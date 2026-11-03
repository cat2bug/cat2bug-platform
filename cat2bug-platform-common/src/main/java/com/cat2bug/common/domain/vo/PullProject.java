package com.cat2bug.common.domain.vo;

import java.util.List;

/**
 * 出站同步请求。
 * {@code gitSlugs} 与 {@code documentIds} 都缺省时视为旧客户端：分包仍上传，但包含全部仓库与本地可读文档。
 * 显式传空数组表示不勾选该项。
 */
public class PullProject {

    private String pullKey;
    private List<String> gitSlugs;
    private List<Long> documentIds;

    public String getPullKey() {
        return pullKey;
    }

    public void setPullKey(String pullKey) {
        this.pullKey = pullKey;
    }

    public List<String> getGitSlugs() {
        return gitSlugs;
    }

    public void setGitSlugs(List<String> gitSlugs) {
        this.gitSlugs = gitSlugs;
    }

    public List<Long> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<Long> documentIds) {
        this.documentIds = documentIds;
    }
}
