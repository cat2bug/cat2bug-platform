package com.cat2bug.system.mybatis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongCsvTypeHandlerTest {

    @Test
    void parsesSpringCommaFormat() {
        assertEquals(List.of(1172L), LongCsvTypeHandler.parse(",1172"));
        assertEquals(List.of(1L, 2L, 3L), LongCsvTypeHandler.parse("1,2,3"));
        assertEquals(List.of(), LongCsvTypeHandler.parse(""));
        assertEquals(List.of(), LongCsvTypeHandler.parse(null));
    }

    @Test
    void parsesLegacyJsonArrayWrittenByEarlierQuarkus() {
        assertEquals(List.of(5L, 6L), LongCsvTypeHandler.parse("[5,6]"));
        assertEquals(List.of(5L, 6L, 7L), LongCsvTypeHandler.parse("[5,6],7"));
        assertEquals(List.of(8L), LongCsvTypeHandler.parse("\"[8]\""));
    }
}
