package com.cat2bug.nativeimage;

import java.util.Arrays;

import com.cat2bug.common.nativeimage.NativeDomainEntityRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeDomainEntityRegistryTest
{
    @Test
    void includesApiCase()
    {
        Class<?>[] types = NativeDomainEntityRegistry.ensureInitialized(getClass().getClassLoader());
        boolean found = Arrays.stream(types).anyMatch(type -> "ApiCase".equals(type.getSimpleName()));
        assertTrue(found, "ApiCase should be registered, total=" + types.length);
    }
}
