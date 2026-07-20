package com.cat2bug.common.config;

import com.cat2bug.common.utils.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 读取项目相关配置
 * 
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "cat2bug")
public class Cat2BugConfig
{
    /** 项目名称 */
    private String name;

    /** 版本 */
    private String version;

    /** 版权年份 */
    private String copyrightYear;

    /** 实例演示开关 */
    private boolean demoEnabled;

    /** 上传路径 */
    private static String profile;

    /**
     * Git bare 仓配置（{@code cat2bug.git.*}）。
     * 默认路径形态：{@code {profile}/{git.root}/{projectId}/{slug}.git}
     */
    private Git git = new Git();

    /** 绑定后的 git.root（供静态工具读取） */
    private static String gitRoot = "git";

    /** 获取地址开关 */
    private static boolean addressEnabled;

    /** 验证码类型 */
    private static String captchaType;

    /** 临时目录（与 cat2bug.temp 对应） */
    private String temp;

    /** 缓存配置 */
    private Cache cache = new Cache();

    public static class Git
    {
        /** 相对 {@link Cat2BugConfig#profile} 的根目录名 */
        private String root = "git";

        public String getRoot()
        {
            return root;
        }

        public void setRoot(String root)
        {
            this.root = root;
            Cat2BugConfig.gitRoot = StringUtils.isEmpty(root) ? "git" : root;
        }
    }

    public static class Cache
    {
        /** local：仅 Caffeine；redis：Caffeine + Redis L2 */
        private String type = "local";

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getCopyrightYear()
    {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear)
    {
        this.copyrightYear = copyrightYear;
    }

    public boolean isDemoEnabled()
    {
        return demoEnabled;
    }

    public void setDemoEnabled(boolean demoEnabled)
    {
        this.demoEnabled = demoEnabled;
    }

    public static String getProfile()
    {
        return profile;
    }

    public void setProfile(String profile)
    {
        // 相对路径立即转绝对，避免后续 cwd 变化导致 {profile}/git/... 漂移
        Cat2BugConfig.profile = toAbsolutePathOrNull(profile);
    }

    public Git getGit()
    {
        return git;
    }

    public void setGit(Git git)
    {
        this.git = git != null ? git : new Git();
        if (this.git.getRoot() != null)
        {
            Cat2BugConfig.gitRoot = StringUtils.isEmpty(this.git.getRoot()) ? "git" : this.git.getRoot();
        }
    }

    public static String getGitRoot()
    {
        return StringUtils.isEmpty(gitRoot) ? "git" : gitRoot;
    }

    /**
     * 默认 Git bare 仓根目录绝对路径：{@code {profile}/{git.root}}。
     * {@link #setProfile(String)} 已将 profile 绝对化，此处再 normalize 一次保证稳定。
     */
    public static String getGitRootPath()
    {
        File root = new File(getProfile(), getGitRoot());
        return root.getAbsoluteFile().toPath().normalize().toString();
    }

    public static boolean isAddressEnabled()
    {
        return addressEnabled;
    }

    public void setAddressEnabled(boolean addressEnabled)
    {
        Cat2BugConfig.addressEnabled = addressEnabled;
    }

    public static String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        Cat2BugConfig.captchaType = captchaType;
    }

    public String getTemp()
    {
        return temp;
    }

    public void setTemp(String temp)
    {
        this.temp = temp;
    }

    public Cache getCache()
    {
        return cache;
    }

    public void setCache(Cache cache)
    {
        this.cache = cache;
    }

    /**
     * 获取导入上传路径
     */
    public static String getImportPath()
    {
        return getProfile() + "/import";
    }

    public static String getImportPath(String path)
    {
        List<String> pathList = new ArrayList<>();
        pathList.add(getProfile());
        if(StringUtils.isNotBlank(path))
            pathList.add(path);
        pathList.add("import");
        return pathList.stream().collect(Collectors.joining(File.separator));
    }

    /**
     * 获取头像上传路径
     */
    public static String getAvatarPath()
    {
        return getProfile() + "/avatar";
    }

    public static String getAvatarPath(String path)
    {
        List<String> pathList = new ArrayList<>();
        pathList.add(getProfile());
        if(StringUtils.isNotBlank(path))
            pathList.add(path);
        pathList.add("avatar");
        return pathList.stream().collect(Collectors.joining(File.separator));
    }

    /**
     * 获取下载路径
     */
    public static String getDownloadPath()
    {
        return getProfile() + "/download/";
    }

    public static String getDownloadPath(String path)
    {
        List<String> pathList = new ArrayList<>();
        pathList.add(getProfile());
        if(StringUtils.isNotBlank(path))
            pathList.add(path);
        pathList.add("download");
        return pathList.stream().collect(Collectors.joining(File.separator));
    }

    /**
     * 获取上传路径
     */
    public static String getUploadPath()
    {
        return getProfile() + "/upload";
    }

    public static String getUploadPath(String path)
    {
        List<String> pathList = new ArrayList<>();
        pathList.add(getProfile());
        if(StringUtils.isNotBlank(path))
            pathList.add(path);
        pathList.add("upload");
        return pathList.stream().collect(Collectors.joining(File.separator));
    }

    /**
     * 将路径转为绝对路径；空值原样返回。绑定 install 的 profile 时调用，避免相对路径随 cwd 漂移。
     */
    static String toAbsolutePathOrNull(String path)
    {
        if (StringUtils.isEmpty(path))
        {
            return path;
        }
        return new File(path.trim()).getAbsoluteFile().toPath().normalize().toString();
    }
}
