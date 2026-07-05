package com.cat2bug.web.service.setup;

import com.alibaba.druid.pool.DruidDataSource;
import com.cat2bug.framework.datasource.DynamicDataSource;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallRuntimeActivatorTest
{
    @Mock
    private DynamicDataSource dynamicDataSource;

    @Mock
    private SetupSubmitDataSourceFactory setupSubmitDataSourceFactory;

    @InjectMocks
    private InstallRuntimeActivator installRuntimeActivator;

    private ConfigurableEnvironment environment;

    @BeforeEach
    void setUp()
    {
        environment = new MockEnvironment();
        ReflectionTestUtils.setField(installRuntimeActivator, "environment", environment);
    }

    @Test
    void mysqlInstallRequiresRestart()
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setDatabaseType("mysql");
        request.setCacheType("local");

        assertFalse(installRuntimeActivator.activateAfterSetup(request, new DruidDataSource()));
        verify(dynamicDataSource, never()).replaceMaster(any());
    }

    @Test
    void redisCacheRequiresRestartEvenForH2()
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setDatabaseType("h2");
        request.setCacheType("redis");

        assertFalse(installRuntimeActivator.activateAfterSetup(request, null));
        verify(dynamicDataSource, never()).replaceMaster(any());
    }

    @Test
    void h2LocalInstallCanHotSwitch()
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setDatabaseType("h2");
        request.setCacheType("local");

        DruidDataSource h2 = new DruidDataSource();
        when(setupSubmitDataSourceFactory.createH2DataSource(request)).thenReturn(h2);

        assertTrue(installRuntimeActivator.activateAfterSetup(request, null));
        verify(dynamicDataSource).replaceMaster(h2);
    }
}
