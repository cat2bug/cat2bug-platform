package com.cat2bug.web.domain.setup;

/**
 * 安装向导提交请求
 */
public class SetupSubmitRequest
{
    private String databaseType;

    private String cacheType;

    private String mysqlHost;

    private Integer mysqlPort;

    private String mysqlDatabase;

    private String mysqlUsername;

    private String mysqlPassword;

    private String redisHost;

    private Integer redisPort;

    private String redisPassword;

    private Integer redisDatabase;

    /** 文件存储根目录（cat2bug.profile） */
    private String profile;

    /** 临时目录（cat2bug.temp） */
    private String temp;

    /** 日志目录（logging.file.path / LOG_PATH） */
    private String logPath;

    private Boolean aiEnabled;

    private String aiHost;

    private String aiDefaultModel;

    private String adminUsername;

    private String adminPassword;

    private String adminNickName;

    private Boolean registerUser;

    private Boolean captchaEnabled;

    public String getDatabaseType()
    {
        return databaseType;
    }

    public void setDatabaseType(String databaseType)
    {
        this.databaseType = databaseType;
    }

    public String getCacheType()
    {
        return cacheType;
    }

    public void setCacheType(String cacheType)
    {
        this.cacheType = cacheType;
    }

    public String getMysqlHost()
    {
        return mysqlHost;
    }

    public void setMysqlHost(String mysqlHost)
    {
        this.mysqlHost = mysqlHost;
    }

    public Integer getMysqlPort()
    {
        return mysqlPort;
    }

    public void setMysqlPort(Integer mysqlPort)
    {
        this.mysqlPort = mysqlPort;
    }

    public String getMysqlDatabase()
    {
        return mysqlDatabase;
    }

    public void setMysqlDatabase(String mysqlDatabase)
    {
        this.mysqlDatabase = mysqlDatabase;
    }

    public String getMysqlUsername()
    {
        return mysqlUsername;
    }

    public void setMysqlUsername(String mysqlUsername)
    {
        this.mysqlUsername = mysqlUsername;
    }

    public String getMysqlPassword()
    {
        return mysqlPassword;
    }

    public void setMysqlPassword(String mysqlPassword)
    {
        this.mysqlPassword = mysqlPassword;
    }

    public String getRedisHost()
    {
        return redisHost;
    }

    public void setRedisHost(String redisHost)
    {
        this.redisHost = redisHost;
    }

    public Integer getRedisPort()
    {
        return redisPort;
    }

    public void setRedisPort(Integer redisPort)
    {
        this.redisPort = redisPort;
    }

    public String getRedisPassword()
    {
        return redisPassword;
    }

    public void setRedisPassword(String redisPassword)
    {
        this.redisPassword = redisPassword;
    }

    public Integer getRedisDatabase()
    {
        return redisDatabase;
    }

    public void setRedisDatabase(Integer redisDatabase)
    {
        this.redisDatabase = redisDatabase;
    }

    public String getProfile()
    {
        return profile;
    }

    public void setProfile(String profile)
    {
        this.profile = profile;
    }

    public String getTemp()
    {
        return temp;
    }

    public void setTemp(String temp)
    {
        this.temp = temp;
    }

    public String getLogPath()
    {
        return logPath;
    }

    public void setLogPath(String logPath)
    {
        this.logPath = logPath;
    }

    public Boolean getAiEnabled()
    {
        return aiEnabled;
    }

    public void setAiEnabled(Boolean aiEnabled)
    {
        this.aiEnabled = aiEnabled;
    }

    public String getAiHost()
    {
        return aiHost;
    }

    public void setAiHost(String aiHost)
    {
        this.aiHost = aiHost;
    }

    public String getAiDefaultModel()
    {
        return aiDefaultModel;
    }

    public void setAiDefaultModel(String aiDefaultModel)
    {
        this.aiDefaultModel = aiDefaultModel;
    }

    public String getAdminUsername()
    {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername)
    {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword)
    {
        this.adminPassword = adminPassword;
    }

    public String getAdminNickName()
    {
        return adminNickName;
    }

    public void setAdminNickName(String adminNickName)
    {
        this.adminNickName = adminNickName;
    }

    public Boolean getRegisterUser()
    {
        return registerUser;
    }

    public void setRegisterUser(Boolean registerUser)
    {
        this.registerUser = registerUser;
    }

    public Boolean getCaptchaEnabled()
    {
        return captchaEnabled;
    }

    public void setCaptchaEnabled(Boolean captchaEnabled)
    {
        this.captchaEnabled = captchaEnabled;
    }
}
