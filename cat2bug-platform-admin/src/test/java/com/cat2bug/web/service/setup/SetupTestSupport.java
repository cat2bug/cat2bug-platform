package com.cat2bug.web.service.setup;

import com.cat2bug.common.utils.spring.SpringUtils;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.mockito.MockedStatic;

/**
 * 安装向导测试共用：消息源 mock、可配置 H2 数据目录的探测器。
 */
final class SetupTestSupport
{
    private SetupTestSupport()
    {
    }

    static DatabaseExistenceProbe probeWithDataDir(Path workDir, SetupMysqlDatabaseService mysqlService)
    {
        DatabaseExistenceProbe probe = new DatabaseExistenceProbe()
        {
            @Override
            public Path h2DataFile(String databaseName)
            {
                DatabaseExistenceProbe.assertValidDatabaseName(databaseName);
                return workDir.resolve("data").resolve(databaseName + ".mv.db");
            }
        };
        ReflectionTestUtils.setField(probe, "setupMysqlDatabaseService", mysqlService);
        return probe;
    }

    static String h2JdbcUrl(Path workDir, String databaseName)
    {
        DatabaseExistenceProbe.assertValidDatabaseName(databaseName);
        return "jdbc:h2:file:" + workDir.resolve("data").resolve(databaseName).toAbsolutePath()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;";
    }

    static void runWithMessages(Runnable action)
    {
        withMessages(() ->
        {
            action.run();
            return null;
        });
    }

    static <T> T withMessages(Supplier<T> action)
    {
        try (MockedStatic<SpringUtils> springUtils = mockStatic(SpringUtils.class))
        {
            MessageSource messageSource = mock(MessageSource.class);
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("msg");
            springUtils.when(() -> SpringUtils.getBean(MessageSource.class)).thenReturn(messageSource);
            return action.get();
        }
    }
}
