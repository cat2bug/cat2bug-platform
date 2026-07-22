package com.cat2bug.system.git.browse.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目级 Git 概览响应（OpenSpec {@code git-repo-project-overview}）。
 *
 * <p>口径（design.md D2）：</p>
 * <ul>
 *     <li>{@code repoCount}：调用者在该项目下可见的仓库数量（与仓库列表口径一致）</li>
 *     <li>{@code openPrCount}：上述可见仓集合中 {@code state='open'} 的 Pull Request 行数</li>
 *     <li>{@code collectCount}：上述可见仓在 {@code sys_user_git_repository} 中关注启用的行数
 *         （跨用户合计，非去重用户数）</li>
 *     <li>{@code languages}：按文件扩展名估算的语言分布（Top 9 +「其他」，百分比整数，见 D3）</li>
 * </ul>
 *
 * @author cat2bug
 */
public class GitProjectOverview
{
    /** 可见仓库数量 */
    private long repoCount;

    /** 可见仓库集合中 open 状态的 PR 数 */
    private long openPrCount;

    /** 可见仓库集合的关注（收藏）行数合计，跨用户 */
    private long collectCount;

    /** 语言分布（Top 9 +「其他」），无可见仓或扫描无结果时为空列表 */
    private List<GitLanguageShare> languages = new ArrayList<>();

    public long getRepoCount()
    {
        return repoCount;
    }

    public void setRepoCount(long repoCount)
    {
        this.repoCount = repoCount;
    }

    public long getOpenPrCount()
    {
        return openPrCount;
    }

    public void setOpenPrCount(long openPrCount)
    {
        this.openPrCount = openPrCount;
    }

    public long getCollectCount()
    {
        return collectCount;
    }

    public void setCollectCount(long collectCount)
    {
        this.collectCount = collectCount;
    }

    public List<GitLanguageShare> getLanguages()
    {
        return languages;
    }

    public void setLanguages(List<GitLanguageShare> languages)
    {
        this.languages = languages != null ? languages : new ArrayList<>();
    }
}
