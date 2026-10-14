package com.cat2bug.quarkus.system.service;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * 安装引导期状态：运行期热切换数据源成功后退出引导，{@code restartRequired} 变为 false。
 */
@ApplicationScoped
public class InstallBootstrapState {

    private volatile boolean runtimeActivated;

    public boolean isRuntimeActivated() {
        return runtimeActivated;
    }

    public void markRuntimeActivated() {
        runtimeActivated = true;
    }
}
