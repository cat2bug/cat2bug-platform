package com.cat2bug.web.service.setup;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.spring.SpringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.MessageSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SetupMysqlDatabaseServiceTest
{
    @Test
    void assertValidDatabaseName_acceptsSimpleName()
    {
        assertDoesNotThrow(() -> SetupMysqlDatabaseService.assertValidDatabaseName("cat2bug_platform"));
    }

    @Test
    void assertValidDatabaseName_rejectsInvalidCharacters()
    {
        try (MockedStatic<SpringUtils> springUtils = mockStatic(SpringUtils.class))
        {
            MessageSource messageSource = mock(MessageSource.class);
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("invalid database name");
            springUtils.when(() -> SpringUtils.getBean(MessageSource.class)).thenReturn(messageSource);

            assertThrows(ServiceException.class,
                    () -> SetupMysqlDatabaseService.assertValidDatabaseName("cat2bug-platform"));
        }
    }

}
