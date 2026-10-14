package com.cat2bug.quarkus.system.service.git;
import jakarta.inject.Inject;

import jakarta.enterprise.context.ApplicationScoped;

import com.cat2bug.quarkus.common.domain.SysGitRepository;
import com.cat2bug.quarkus.system.git.browse.domain.GitLanguageShare;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 轻量语言分布扫描器（design.md D3）：对项目下每个可见仓库打开 bare 仓、解析默认分支，
 * 按文件路径的扩展名将文件数加权归入语言标签，合并后取 Top 9 + 其余并入「其他」。
 *
 * <p>约束：</p>
 * <ul>
 *     <li>只读文件路径，不读取 blob 内容（性能优先，见 D3 备选否决）</li>
 *     <li>路径段命中 {@code node_modules}/{@code vendor}/{@code dist}/{@code build}/{@code .git}
 *         时整棵子树跳过，不计入扫描上限</li>
 *     <li>每仓最多计入 {@link #PER_REPOSITORY_FILE_CAP} 个文件；项目合计最多
 *         {@link #PROJECT_FILE_CAP} 个文件（跨仓共享预算，先到先得）</li>
 *     <li>单仓打开失败 / 无默认分支 / IO 异常：跳过该仓，不向上抛出，不影响 overview 整体成功</li>
 * </ul>
 *
 * @author cat2bug
 */
@ApplicationScoped
public class GitRepoLanguageScanner
{
    private static final Logger log = LoggerFactory.getLogger(GitRepoLanguageScanner.class);

    /** 每仓最多计入的文件数 */
    static final int PER_REPOSITORY_FILE_CAP = 5000;

    /** 项目合计最多计入的文件数（跨仓共享） */
    static final int PROJECT_FILE_CAP = 20000;

    /** 返回的语言条目上限（不含「其他」） */
    static final int TOP_LANGUAGE_COUNT = 9;

    /** 未匹配到内置映射表的扩展名/文件名统一归入的桶名 */
    static final String OTHER_LANGUAGE_NAME = "其他";

    /** 跳过的路径段（子树命中即整棵跳过，不计入文件数上限） */
    private static final Set<String> SKIP_PATH_SEGMENTS = Set.of(
        "node_modules", "vendor", "dist", "build", ".git");

    /** 精确文件名（大小写不敏感）→ 语言，优先于扩展名映射（如 Dockerfile 无扩展名） */
    private static final Map<String, String> FILENAME_LANGUAGE_MAP = buildFilenameMap();

    /** 扩展名（不含点，小写）→ 语言 */
    private static final Map<String, String> EXTENSION_LANGUAGE_MAP = buildExtensionMap();

    /** 包内可见（非 private），供测试直接注入，避免仅为测试暴露 public setter */
    @Inject
    GitPathValidator gitPathValidator;

    private final int perRepositoryFileCap;

    private final int projectFileCap;

    public GitRepoLanguageScanner()
    {
        this(PER_REPOSITORY_FILE_CAP, PROJECT_FILE_CAP);
    }

    /**
     * 包内可见构造：仅供单测注入更小的文件预算，验证每仓/项目上限逻辑而不需要真的创建上万个文件。
     * 生产环境（Spring 容器）始终经由无参构造使用 design.md D3 规定的默认值。
     */
    GitRepoLanguageScanner(int perRepositoryFileCap, int projectFileCap)
    {
        this.perRepositoryFileCap = perRepositoryFileCap;
        this.projectFileCap = projectFileCap;
    }

    /**
     * 扫描一批可见仓库，返回合并后的语言分布（Top 9 +「其他」，百分比整数，总和约 100）。
     * 单仓失败不抛出，直接跳过继续扫描其余仓库；项目级文件预算用尽后提前结束。
     *
     * @param repositories 项目下可见的仓库集合（须含 diskPath / defaultBranch）
     * @return 语言分布列表；无仓库或全部扫描失败/为空仓时返回空列表
     */
    public List<GitLanguageShare> scan(List<SysGitRepository> repositories)
    {
        Map<String, Long> languageFileCounts = new LinkedHashMap<>();
        if (repositories == null || repositories.isEmpty())
        {
            return buildLanguageShares(languageFileCounts);
        }

        int remainingProjectBudget = projectFileCap;
        for (SysGitRepository repository : repositories)
        {
            if (remainingProjectBudget <= 0)
            {
                break;
            }
            int repoBudget = Math.min(perRepositoryFileCap, remainingProjectBudget);
            int scanned = scanRepositorySafely(repository, languageFileCounts, repoBudget);
            remainingProjectBudget -= scanned;
        }
        return buildLanguageShares(languageFileCounts);
    }

    /**
     * 扫描单仓；任何异常（磁盘缺失、无默认分支、IO 错误）均被吞掉并记录 warn 日志，返回 0。
     */
    private int scanRepositorySafely(SysGitRepository repository, Map<String, Long> languageFileCounts, int budget)
    {
        try
        {
            return scanRepository(repository, languageFileCounts, budget);
        }
        catch (Exception e)
        {
            log.warn("语言扫描跳过仓库 repositoryId={}：{}",
                repository != null ? repository.getRepositoryId() : null, e.getMessage());
            return 0;
        }
    }

    private int scanRepository(SysGitRepository repository, Map<String, Long> languageFileCounts, int budget)
        throws IOException
    {
        if (repository == null || budget <= 0)
        {
            return 0;
        }
        String diskPath = safeResolveDiskPath(repository.getDiskPath());
        if (diskPath == null)
        {
            return 0;
        }
        File gitDir = new File(diskPath);
        if (!gitDir.exists() || !gitDir.isDirectory())
        {
            return 0;
        }
        try (Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).setBare().build())
        {
            ObjectId commitId = resolveDefaultBranchTip(repo, repository.getDefaultBranch());
            if (commitId == null)
            {
                return 0;
            }
            try (RevWalk revWalk = new RevWalk(repo))
            {
                RevCommit commit = revWalk.parseCommit(commitId);
                return walkTreeFilePaths(repo, commit, languageFileCounts, budget);
            }
        }
    }

    private String safeResolveDiskPath(String diskPath)
    {
        if (diskPath == null || diskPath.isEmpty())
        {
            return null;
        }
        try
        {
            return gitPathValidator.resolveStoredDiskPath(diskPath);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    /**
     * 解析默认分支 tip：优先仓库元数据 {@code defaultBranch}；解析不到则回退符号 HEAD；
     * 二者皆无（空仓 / 元数据分支已不存在）返回 {@code null}，调用方跳过该仓。
     */
    private static ObjectId resolveDefaultBranchTip(Repository repo, String defaultBranch) throws IOException
    {
        if (defaultBranch != null && !defaultBranch.trim().isEmpty())
        {
            ObjectId byMetadata = repo.resolve(Constants.R_HEADS + defaultBranch.trim());
            if (byMetadata != null)
            {
                return byMetadata;
            }
        }
        return repo.resolve(Constants.HEAD);
    }

    /**
     * 非递归手动下钻：命中跳过目录时不进入子树（整棵跳过，不计入预算消耗），
     * 其余目录进入子树继续遍历；达到 budget 或遍历完毕即停止。
     */
    private static int walkTreeFilePaths(
        Repository repo, RevCommit commit, Map<String, Long> languageFileCounts, int budget) throws IOException
    {
        int scanned = 0;
        try (TreeWalk treeWalk = new TreeWalk(repo))
        {
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(false);
            while (scanned < budget && treeWalk.next())
            {
                boolean isTree = (treeWalk.getRawMode(0) & FileMode.TYPE_MASK) == FileMode.TYPE_TREE;
                if (isTree)
                {
                    if (SKIP_PATH_SEGMENTS.contains(treeWalk.getNameString()))
                    {
                        continue;
                    }
                    treeWalk.enterSubtree();
                    continue;
                }
                String language = mapPathToLanguage(treeWalk.getNameString());
                languageFileCounts.merge(language, 1L, Long::sum);
                scanned++;
            }
        }
        return scanned;
    }

    /**
     * 按文件名映射语言：先精确文件名（如 Dockerfile），再扩展名；均未命中归「其他」。
     */
    static String mapPathToLanguage(String fileName)
    {
        if (fileName == null || fileName.isEmpty())
        {
            return OTHER_LANGUAGE_NAME;
        }
        String byName = FILENAME_LANGUAGE_MAP.get(fileName.toLowerCase(Locale.ROOT));
        if (byName != null)
        {
            return byName;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1)
        {
            return OTHER_LANGUAGE_NAME;
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return EXTENSION_LANGUAGE_MAP.getOrDefault(ext, OTHER_LANGUAGE_NAME);
    }

    /**
     * 合并规则：先剥离「其他」预累计值，剩余语言按文件数降序排序取前 9；
     * 第 10 名及之后与预累计「其他」相加，若 &gt; 0 则作为最后一条追加。
     * 百分比：前 n-1 条四舍五入，末条 = 100 - 之前各条之和（调平，保证总和为 100）。
     */
    static List<GitLanguageShare> buildLanguageShares(Map<String, Long> languageFileCounts)
    {
        List<GitLanguageShare> result = new ArrayList<>();
        if (languageFileCounts == null || languageFileCounts.isEmpty())
        {
            return result;
        }

        long otherCount = languageFileCounts.getOrDefault(OTHER_LANGUAGE_NAME, 0L);
        List<Map.Entry<String, Long>> ranked = new ArrayList<>();
        for (Map.Entry<String, Long> entry : languageFileCounts.entrySet())
        {
            if (OTHER_LANGUAGE_NAME.equals(entry.getKey()))
            {
                continue;
            }
            ranked.add(entry);
        }
        ranked.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
            .thenComparing(Map.Entry::getKey));

        List<Map.Entry<String, Long>> kept;
        if (ranked.size() > TOP_LANGUAGE_COUNT)
        {
            kept = ranked.subList(0, TOP_LANGUAGE_COUNT);
            for (int i = TOP_LANGUAGE_COUNT; i < ranked.size(); i++)
            {
                otherCount += ranked.get(i).getValue();
            }
        }
        else
        {
            kept = ranked;
        }

        long total = otherCount;
        for (Map.Entry<String, Long> entry : kept)
        {
            total += entry.getValue();
        }
        if (total <= 0)
        {
            return result;
        }

        List<String> names = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (Map.Entry<String, Long> entry : kept)
        {
            names.add(entry.getKey());
            counts.add(entry.getValue());
        }
        if (otherCount > 0)
        {
            names.add(OTHER_LANGUAGE_NAME);
            counts.add(otherCount);
        }

        int percentSum = 0;
        for (int i = 0; i < names.size() - 1; i++)
        {
            int percent = (int) Math.round(counts.get(i) * 100.0 / total);
            percentSum += percent;
            result.add(new GitLanguageShare(names.get(i), percent));
        }
        int lastIndex = names.size() - 1;
        int lastPercent = Math.max(0, 100 - percentSum);
        result.add(new GitLanguageShare(names.get(lastIndex), lastPercent));
        return result;
    }

    private static Map<String, String> buildFilenameMap()
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("dockerfile", "Docker");
        return map;
    }

    private static Map<String, String> buildExtensionMap()
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("java", "Java");
        map.put("js", "JavaScript");
        map.put("jsx", "JavaScript");
        map.put("ts", "TypeScript");
        map.put("tsx", "TypeScript");
        map.put("vue", "Vue");
        map.put("css", "CSS");
        map.put("scss", "CSS");
        map.put("sh", "Shell");
        map.put("sql", "SQL");
        map.put("py", "Python");
        map.put("go", "Go");
        map.put("md", "Markdown");
        map.put("xml", "XML");
        map.put("html", "HTML");
        map.put("kt", "Kotlin");
        map.put("c", "C/C++");
        map.put("cpp", "C/C++");
        map.put("h", "C/C++");
        map.put("yml", "YAML");
        map.put("yaml", "YAML");
        return map;
    }
}
