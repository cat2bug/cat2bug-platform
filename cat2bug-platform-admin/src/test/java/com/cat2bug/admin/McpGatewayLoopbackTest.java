package com.cat2bug.admin;

import com.cat2bug.admin.mcp.McpGateway;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MCP 回环并发许可推算：须同时给嵌套 REST 留工作线程，并给网页与令牌校验留数据库连接。
 */
class McpGatewayLoopbackTest {

    @Test
    void explicitPermitWins() {
        assertEquals(8, McpGateway.resolveLoopbackPermits(200, 40, OptionalInt.of(8)));
    }

    @Test
    void threadLimitStillAppliesWhenPoolIsLarge() {
        assertEquals(66, McpGateway.resolveLoopbackPermits(200, 200, OptionalInt.empty()));
        assertEquals(4, McpGateway.resolveLoopbackPermits(9, 40, OptionalInt.empty()));
        assertEquals(4, McpGateway.resolveLoopbackPermits(1, 40, OptionalInt.empty()));
        assertEquals(4, McpGateway.resolveLoopbackPermits(12, 40, OptionalInt.empty()));
        assertEquals(5, McpGateway.resolveLoopbackPermits(15, 40, OptionalInt.empty()));
    }

    @Test
    void databasePoolKeepsHalfForOtherApis() {
        assertEquals(20, McpGateway.resolveLoopbackPermits(200, 40, OptionalInt.empty()));
        assertEquals(10, McpGateway.resolveLoopbackPermits(200, 20, OptionalInt.empty()));
        assertEquals(1, McpGateway.resolveLoopbackPermits(200, 1, OptionalInt.empty()));
    }

    @Test
    void explicitMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> McpGateway.resolveLoopbackPermits(200, 40, OptionalInt.of(0)));
    }
}
