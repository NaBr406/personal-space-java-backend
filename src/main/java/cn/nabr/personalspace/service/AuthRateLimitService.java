package cn.nabr.personalspace.service;

import cn.nabr.personalspace.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthRateLimitService {
    private static final long CLEANUP_AFTER_MILLIS = 10 * 60 * 1000L;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

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

    private void cleanupOldEntries(long nowMillis) {
        attempts.entrySet().removeIf(entry -> nowMillis - entry.getValue().startedAtMillis() > CLEANUP_AFTER_MILLIS);
    }

    private record AttemptWindow(long startedAtMillis, int count) {}
}
