package com.cat2bug.system.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanUpdateTimeThrottleTest {

    @Test
    void samePlanSkipsUntilIntervalElapses() {
        PlanUpdateTimeThrottle throttle = new PlanUpdateTimeThrottle();
        assertTrue(throttle.shouldTouch("p1", 1_000));
        assertFalse(throttle.shouldTouch("p1", 1_000 + PlanUpdateTimeThrottle.MIN_INTERVAL_MILLIS - 1));
        assertTrue(throttle.shouldTouch("p1", 1_000 + PlanUpdateTimeThrottle.MIN_INTERVAL_MILLIS));
    }

    @Test
    void differentPlansDoNotBlockEachOther() {
        PlanUpdateTimeThrottle throttle = new PlanUpdateTimeThrottle();
        assertTrue(throttle.shouldTouch("p1", 1_000));
        assertTrue(throttle.shouldTouch("p2", 1_000));
    }

    @Test
    void blankPlanIdIsSkipped() {
        PlanUpdateTimeThrottle throttle = new PlanUpdateTimeThrottle();
        assertFalse(throttle.shouldTouch(null, 1_000));
        assertFalse(throttle.shouldTouch("  ", 1_000));
    }
}
