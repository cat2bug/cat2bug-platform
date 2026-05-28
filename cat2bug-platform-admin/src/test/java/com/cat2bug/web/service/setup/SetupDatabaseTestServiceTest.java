package com.cat2bug.web.service.setup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupDatabaseTestServiceTest
{
    @Test
    void isUnknownDatabaseError_detectsMysqlMessage()
    {
        assertTrue(SetupDatabaseTestService.isUnknownDatabaseError(
                new RuntimeException("Unknown database 'missing'")));
    }

    @Test
    void isUnknownDatabaseError_returnsFalseForOtherErrors()
    {
        assertFalse(SetupDatabaseTestService.isUnknownDatabaseError(
                new RuntimeException("Access denied for user")));
    }
}
