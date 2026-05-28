package com.cat2bug.web.service.setup;

import com.cat2bug.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertThrows(ServiceException.class,
                () -> SetupMysqlDatabaseService.assertValidDatabaseName("cat2bug-platform"));
    }

}
