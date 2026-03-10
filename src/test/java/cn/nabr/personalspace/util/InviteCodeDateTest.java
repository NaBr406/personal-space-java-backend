package cn.nabr.personalspace.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InviteCodeDateTest {

    @Test
    void inviteCodeDayUsesUtcInsteadOfServerLocalDate() {
        Clock beijingJustAfterMidnight = Clock.fixed(
                Instant.parse("2026-03-10T16:30:00Z"),
                ZoneId.of("Asia/Shanghai")
        );

        assertEquals("2026-03-10", InviteCodeDate.todayUtc(beijingJustAfterMidnight));
    }
}
