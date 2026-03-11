package cn.nabr.personalspace.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * 邀请码日期工具。
 * 统一按 UTC 计算“今天”，避免服务器时区导致邀请码串天。
 */
public final class InviteCodeDate {
    private InviteCodeDate() {
    }

    /**
     * 生产环境默认走系统 UTC 时钟。
     */
    public static String todayUtc() {
        return todayUtc(Clock.systemUTC());
    }

    static String todayUtc(Clock clock) {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC)).toString();
    }
}
