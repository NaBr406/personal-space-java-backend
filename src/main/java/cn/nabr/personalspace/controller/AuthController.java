package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.dto.LoginRequest;
import cn.nabr.personalspace.dto.RegisterRequest;
import cn.nabr.personalspace.security.AuthHelper;
import cn.nabr.personalspace.service.AuthService;
import cn.nabr.personalspace.service.AuthRateLimitService;
import cn.nabr.personalspace.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private static final long AUTH_RATE_LIMIT_WINDOW_MILLIS = 60_000L;

    private final CaptchaService captchaService;
    private final AuthService authService;
    private final AuthHelper authHelper;
    private final AuthRateLimitService authRateLimitService;

    public AuthController(
            CaptchaService captchaService,
            AuthService authService,
            AuthHelper authHelper,
            AuthRateLimitService authRateLimitService
    ) {
        this.captchaService = captchaService;
        this.authService = authService;
        this.authHelper = authHelper;
        this.authRateLimitService = authRateLimitService;
    }

    @GetMapping("/captcha")
    public Map<String, String> captcha() {
        return captchaService.createCaptcha();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        authRateLimitService.check(
                "register:" + clientIp,
                5,
                AUTH_RATE_LIMIT_WINDOW_MILLIS,
                "注册请求过于频繁，请稍后再试"
        );
        return ResponseEntity.status(201).body(authService.register(request, clientIp));
    }

    @PostMapping("/login")
    public Object login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        authRateLimitService.check(
                "login:" + extractClientIp(servletRequest),
                10,
                AUTH_RATE_LIMIT_WINDOW_MILLIS,
                "登录尝试过于频繁，请稍后再试"
        );
        return authService.login(request);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        authService.logout(authHelper.extractBearerToken(request));
        return Map.of("message", "已登出");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(authHelper.requireUser(request));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("x-forwarded-for");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("x-real-ip");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null) {
            return "";
        }
        return remoteAddr.replace("::ffff:", "");
    }
}
