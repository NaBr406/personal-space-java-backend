package cn.nabr.personalspace.service;

import cn.nabr.personalspace.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级认证限流器。
 * 用内存窗口统计登录/注册尝试次数，重启后自然清空。
 */
@Service
public class AuthRateLimitService {
    private static final long CLEANUP_AFTER_MILLIS = 10 * 60 * 1000L;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    /**
     * 超过窗口内最大尝试次数时，直接抛 429。
     */
    public void check(String key, int maxAttempts, long windowMillis, String message) {
        if (isLimited(key, maxAttempts, windowMillis)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
        }
    }

    boolean isLimited(String key, int maxAttempts, long windowMillis) {
        return isLimitedAt(key, maxAttempts, windowMillis, System.currentTimeMillis());
    }

    boolean isLimitedAt(String key, int maxAttempts, long windowMillis, long nowMillis) {
        AttemptWindow window = attempts.compute(key, (ignored, current) -> {
            if (current == null || nowMillis - current.startedAtMillis() > windowMillis) {
                return new AttemptWindow(nowMillis, 1);
            }
            return new AttemptWindow(current.startedAtMillis(), current.count() + 1);
        });

        cleanupOldEntries(nowMillis);
        return window != null && window.count() > maxAttempts;
    }

    /**
     * 顺手清理长时间不用的 key，避免内存表无限长。
     */
    private void cleanupOldEntries(long nowMillis) {
        attempts.entrySet().removeIf(entry -> nowMillis - entry.getValue().startedAtMillis() > CLEANUP_AFTER_MILLIS);
    }

    private record AttemptWindow(long startedAtMillis, int count) {}
}
