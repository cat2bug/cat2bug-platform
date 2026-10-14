package com.cat2bug.admin.nativeimage;

import com.cat2bug.common.domain.im.DingProjectConfig;
import com.cat2bug.common.domain.im.EnterpriseWeChatProjectConfig;
import com.cat2bug.common.domain.im.FeishuProjectConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 项目企业应用配置只经 Jackson 从 {@code sys_im_project_config.config_params} 手工反序列化，
 * 不出现在 REST 签名里，Native 下需显式注册反射。
 */
@RegisterForReflection(targets = {
        DingProjectConfig.class,
        FeishuProjectConfig.class,
        EnterpriseWeChatProjectConfig.class
})
public final class ImReflectionConfig {

    private ImReflectionConfig() {
    }
}
