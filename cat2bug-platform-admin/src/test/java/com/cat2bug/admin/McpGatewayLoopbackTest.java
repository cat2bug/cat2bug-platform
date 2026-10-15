package com.cat2bug.admin;

import com.cat2bug.admin.mcp.McpGateway;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MCP 回环并发许可推算：须为嵌套 REST 预留工作线程，避免自死锁。
 */
class McpGatewayLoopbackTest {

    @Test
    void explicitPermitWins() {
        assertEquals(8, McpGateway.resolveLoopbackPermits(200, OptionalInt.of(8)));
    }

    @Test
    void defaultIsOneThirdOfPoolAtLeastFour() {
        assertEquals(66, McpGateway.resolveLoopbackPermits(200, OptionalInt.empty()));
        assertEquals(4, McpGateway.resolveLoopbackPermits(9, OptionalInt.empty()));
        assertEquals(4, McpGateway.resolveLoopbackPermits(1, OptionalInt.empty()));
        assertEquals(4, McpGateway.resolveLoopbackPermits(12, OptionalInt.empty()));
        assertEquals(5, McpGateway.resolveLoopbackPermits(15, OptionalInt.empty()));
    }

    @Test
    void explicitMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> McpGateway.resolveLoopbackPermits(200, OptionalInt.of(0)));
    }
}
