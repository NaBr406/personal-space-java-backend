package cn.nabr.personalspace.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimitServiceTest {

    @Test
    void allowsRequestsUntilLimitIsExceeded() {
        AuthRateLimitService authRateLimitService = new AuthRateLimitService();
        assertFalse(authRateLimitService.isLimitedAt("login:127.0.0.1", 2, 60_000L, 1_000L));
        assertFalse(authRateLimitService.isLimitedAt("login:127.0.0.1", 2, 60_000L, 2_000L));
        assertTrue(authRateLimitService.isLimitedAt("login:127.0.0.1", 2, 60_000L, 3_000L));
    }

    @Test
    void resetsCounterAfterWindowExpires() {
        AuthRateLimitService authRateLimitService = new AuthRateLimitService();
        assertFalse(authRateLimitService.isLimitedAt("register:127.0.0.1", 1, 60_000L, 1_000L));
        assertTrue(authRateLimitService.isLimitedAt("register:127.0.0.1", 1, 60_000L, 2_000L));
        assertFalse(authRateLimitService.isLimitedAt("register:127.0.0.1", 1, 60_000L, 62_000L));
    }

    @Test
    void keepsDifferentKeysIndependent() {
        AuthRateLimitService authRateLimitService = new AuthRateLimitService();
        assertFalse(authRateLimitService.isLimitedAt("login:ip-a", 1, 60_000L, 1_000L));
        assertTrue(authRateLimitService.isLimitedAt("login:ip-a", 1, 60_000L, 2_000L));
        assertFalse(authRateLimitService.isLimitedAt("login:ip-b", 1, 60_000L, 2_000L));
    }
}
