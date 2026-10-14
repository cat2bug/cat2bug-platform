package com.cat2bug.system.git.browse.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 仓库内全文检索结果（按行匹配）。
 */
public class GitCodeSearchResult
{
    public static class Match
    {
        private String path;

        private int line;

        private String text;

        public Match()
        {
        }

        public Match(String path, int line, String text)
        {
            this.path = path;
            this.line = line;
            this.text = text;
        }

        public String getPath()
        {
            return path;
        }

        public void setPath(String path)
        {
            this.path = path;
        }

        public int getLine()
        {
            return line;
        }

        public void setLine(int line)
        {
            this.line = line;
        }

        public String getText()
        {
            return text;
        }

        public void setText(String text)
        {
            this.text = text;
        }
    }

    private String commitId;

    private List<Match> matches = new ArrayList<>();

    /** 命中数达到上限或扫描量达到上限时为 true，结果不完整 */
    private boolean truncated;

    private int filesScanned;

    public String getCommitId()
    {
        return commitId;
    }

    public void setCommitId(String commitId)
    {
        this.commitId = commitId;
    }

    public List<Match> getMatches()
    {
        return matches;
    }

    public void setMatches(List<Match> matches)
    {
        this.matches = matches;
    }

    public boolean isTruncated()
    {
        return truncated;
    }

    public void setTruncated(boolean truncated)
    {
        this.truncated = truncated;
    }

    public int getFilesScanned()
    {
        return filesScanned;
    }

    public void setFilesScanned(int filesScanned)
    {
        this.filesScanned = filesScanned;
    }
}
