package cn.nabr.personalspace.security;

import cn.nabr.personalspace.exception.ApiException;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.AuthRepository;
import cn.nabr.personalspace.util.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 认证辅助类。
 * 封装 Bearer token 提取、当前用户查询，以及角色校验。
 */
@Component
public class AuthHelper {
    private final AuthRepository authRepository;

    public AuthHelper(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public UserSummary requireUser(HttpServletRequest request) {
        return getUser(request).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "请先登录"));
    }

    public UserSummary requireAdmin(HttpServletRequest request) {
        UserSummary user = requireUser(request);
        if (!"admin".equals(user.role()) && !"superadmin".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        return user;
    }

    public UserSummary requireSuperAdmin(HttpServletRequest request) {
        UserSummary user = requireUser(request);
        if (!"superadmin".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "需要超级管理员权限");
        }
        return user;
    }

    /**
     * 当前接口允许匿名时，可以用这个方法尝试解析登录用户。
     */
    public java.util.Optional<UserSummary> getUser(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            return java.util.Optional.empty();
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return java.util.Optional.empty();
        }
        return authRepository.findUserByTokenHash(TokenUtils.sha256(token));
    }

    /**
     * 登出时只需要拿到原始 Bearer token，再交给 repository 按 hash 删除 session。
     */
    public String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring("Bearer ".length()).trim();
    }
}
