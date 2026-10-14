package com.cat2bug.system.git.browse.domain;

/**
 * 项目 Git 概览中的单条语言占比（design.md D3）。
 *
 * <p>{@code name} 为语言展示名（内置扩展名映射表，未知扩展名与 Top N 之外的尾部统一归并为
 * {@code 其他}）；{@code percent} 为整数百分比，同一响应内所有条目之和约为 100（末项调平）。</p>
 *
 * @author cat2bug
 */
public class GitLanguageShare
{
    /** 语言展示名（如 Java / JavaScript / 其他） */
    private String name;

    /** 占比（整数百分比，0~100） */
    private int percent;

    public GitLanguageShare()
    {
    }

    public GitLanguageShare(String name, int percent)
    {
        this.name = name;
        this.percent = percent;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getPercent()
    {
        return percent;
    }

    public void setPercent(int percent)
    {
        this.percent = percent;
    }
}
