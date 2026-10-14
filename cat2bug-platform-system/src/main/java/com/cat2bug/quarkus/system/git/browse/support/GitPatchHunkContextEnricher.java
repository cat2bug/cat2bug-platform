package com.cat2bug.quarkus.system.git.browse.support;

import org.eclipse.jgit.diff.RawText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JGit {@code DiffFormatter} 只输出 {@code @@ -a,b +c,d @@}，不附带 Git 原生的
 * hunk 尾部函数/类名上下文。本类按近似 Git {@code xfuncname} 的启发式补上该段文字。
 */
public final class GitPatchHunkContextEnricher
{
    private static final Pattern HUNK_HEADER = Pattern.compile(
            "^@@\\s+-(\\d+)(?:,\\d+)?\\s+\\+(\\d+)(?:,\\d+)?\\s+@@(.*)$");

    /** 与 Git 展示习惯接近：过长则截断 */
    private static final int MAX_CONTEXT_LEN = 80;

    /** 向上 / 向下搜索行数上限 */
    private static final int SCAN_LIMIT = 3000;

    private GitPatchHunkContextEnricher()
    {
    }

    /**
     * @param patch   unified diff 全文
     * @param oldText 变更前内容（可为 {@link RawText#EMPTY_TEXT}）
     * @param newText 变更后内容（可为 {@link RawText#EMPTY_TEXT}）
     */
    public static String enrich(String patch, RawText oldText, RawText newText)
    {
        if (patch == null || patch.isEmpty())
        {
            return patch;
        }
        String[] lines = patch.split("\n", -1);
        boolean changed = false;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            Matcher m = HUNK_HEADER.matcher(line);
            if (!m.matches())
            {
                continue;
            }
            String existing = m.group(3) != null ? m.group(3).trim() : "";
            if (!existing.isEmpty())
            {
                continue;
            }
            int oldStart = Integer.parseInt(m.group(1));
            int newStart = Integer.parseInt(m.group(2));
            String ctx = findContext(newText, newStart);
            if (ctx == null)
            {
                ctx = findContext(oldText, oldStart);
            }
            if (ctx == null || ctx.isEmpty())
            {
                continue;
            }
            lines[i] = line + " " + ctx;
            changed = true;
        }
        if (!changed)
        {
            return patch;
        }
        return String.join("\n", lines);
    }

    /**
     * @param oneBasedStart hunk 头中的起始行号（1-based；0 表示空侧）
     */
    static String findContext(RawText text, int oneBasedStart)
    {
        if (text == null || text.size() == 0)
        {
            return null;
        }
        int startIdx = oneBasedStart <= 0 ? 0 : Math.min(text.size() - 1, Math.max(0, oneBasedStart - 1));

        int backFrom = oneBasedStart <= 0 ? 0 : startIdx;
        for (int i = backFrom - 1; i >= 0 && backFrom - i <= SCAN_LIMIT; i--)
        {
            String candidate = sanitizeContext(text.getString(i));
            if (candidate != null && isFuncNameLine(candidate))
            {
                return truncate(candidate);
            }
        }

        // 新文件 / 文件开头：向后找首个像定义的行
        for (int i = startIdx; i < text.size() && i - startIdx < 40; i++)
        {
            String candidate = sanitizeContext(text.getString(i));
            if (candidate != null && isFuncNameLine(candidate))
            {
                return truncate(candidate);
            }
        }
        return null;
    }

    static boolean isFuncNameLine(String line)
    {
        if (line == null)
        {
            return false;
        }
        String s = line.stripLeading();
        if (s.isEmpty())
        {
            return false;
        }
        if (s.startsWith("//") || s.startsWith("/*") || s.startsWith("*") || s.startsWith("#")
                || s.startsWith("<!--") || s.startsWith("import ") || s.startsWith("package ")
                || s.startsWith("using ") || s.startsWith("from ") || s.equals("{") || s.equals("}"))
        {
            return false;
        }
        if (s.startsWith("else") || s.startsWith("if ") || s.startsWith("if(") || s.startsWith("for ")
                || s.startsWith("for(") || s.startsWith("while ") || s.startsWith("while(")
                || s.startsWith("switch ") || s.startsWith("switch(") || s.startsWith("catch ")
                || s.startsWith("catch(") || s.startsWith("return ") || s.startsWith("throw ")
                || s.startsWith("case ") || s.startsWith("default:"))
        {
            return false;
        }
        if (s.startsWith("def ") || s.startsWith("async def ") || s.startsWith("function ")
                || s.startsWith("async function ") || s.startsWith("export ") || s.startsWith("class ")
                || s.startsWith("interface ") || s.startsWith("enum ") || s.startsWith("record ")
                || s.startsWith("trait ") || s.startsWith("impl ") || s.startsWith("fn ")
                || s.startsWith("func ") || s.startsWith("type ") || s.startsWith("struct "))
        {
            return true;
        }
        if (s.matches("^(public|protected|private|static|final|abstract|synchronized|native|default)\\b.*"))
        {
            return true;
        }
        // 无缩进且含 '('：常见 C/Java 风格声明
        if (!Character.isWhitespace(line.charAt(0)) && s.indexOf('(') > 0)
        {
            return true;
        }
        return false;
    }

    private static String sanitizeContext(String raw)
    {
        if (raw == null)
        {
            return null;
        }
        String t = raw.replace('\t', ' ').strip();
        return t.isEmpty() ? null : t;
    }

    private static String truncate(String s)
    {
        if (s.length() <= MAX_CONTEXT_LEN)
        {
            return s;
        }
        return s.substring(0, MAX_CONTEXT_LEN - 1) + "…";
    }
}
