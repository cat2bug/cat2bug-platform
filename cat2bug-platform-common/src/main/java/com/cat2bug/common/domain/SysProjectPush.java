package com.cat2bug.common.domain;

public class SysProjectPush {

    /** 推送包契约版本：1.0 业务主体；1.1 含团队配置与 Git bare；1.2 可选 Git/文档分包 */
    public static final String PACKAGE_SCHEMA_1_1 = "1.1";
    public static final String PACKAGE_SCHEMA_1_2 = "1.2";

    private String version;
    private String pushKey;
    private String packageSchema;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPushKey() {
        return pushKey;
    }

    public void setPushKey(String pushKey) {
        this.pushKey = pushKey;
    }

    public String getPackageSchema() {
        return packageSchema;
    }

    public void setPackageSchema(String packageSchema) {
        this.packageSchema = packageSchema;
    }
}
