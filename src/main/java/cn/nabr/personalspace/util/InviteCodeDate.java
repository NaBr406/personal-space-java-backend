package cn.nabr.personalspace.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

public final class InviteCodeDate {
    private InviteCodeDate() {
    }

    public static String todayUtc() {
        return todayUtc(Clock.systemUTC());
    }

    static String todayUtc(Clock clock) {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC)).toString();
    }
}
