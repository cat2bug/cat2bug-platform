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

    /** 获取地址开关 */
    private static boolean addressEnabled;

    /** 验证码类型 */
    private static String captchaType;

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
        Cat2BugConfig.profile = profile;
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
        return File.separator + pathList.stream().collect(Collectors.joining(File.separator));
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
        return File.separator + pathList.stream().collect(Collectors.joining(File.separator));
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
        return File.separator + pathList.stream().collect(Collectors.joining(File.separator));
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
        return File.separator + pathList.stream().collect(Collectors.joining(File.separator));
    }
}
