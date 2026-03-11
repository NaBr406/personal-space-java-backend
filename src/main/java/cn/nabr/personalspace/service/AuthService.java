package cn.nabr.personalspace.service;

import cn.nabr.personalspace.config.AppProperties;
import cn.nabr.personalspace.dto.LoginRequest;
import cn.nabr.personalspace.dto.RegisterRequest;
import cn.nabr.personalspace.exception.ApiException;
import cn.nabr.personalspace.model.UserAuthView;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.AuthRepository;
import cn.nabr.personalspace.util.InviteCodeDate;
import cn.nabr.personalspace.util.TokenUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证业务。
 * 这里尽量对齐旧 Node 版的注册、登录和 session 行为，方便平滑切换。
 */
@Service
public class AuthService {
    private final AuthRepository authRepository;
    private final CaptchaService captchaService;
    private final AppProperties appProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AuthRepository authRepository, CaptchaService captchaService, AppProperties appProperties) {
        this.authRepository = authRepository;
        this.captchaService = captchaService;
        this.appProperties = appProperties;
    }

    @Transactional
    public UserAuthView register(RegisterRequest request, String clientIp) {
        String inviteCode = request.getInviteCode().trim().toUpperCase();
        // 邀请码按 UTC 日期分桶，避免服务器时区变化把“当天邀请码”算串天。
        String today = InviteCodeDate.todayUtc();
        if (authRepository.findUnusedInviteCodeId(inviteCode, today).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "邀请码无效或已过期");
        }

        captchaService.verify(request.getCaptchaToken(), request.getCaptchaAnswer());

        String username = request.getUsername().trim();
        String password = request.getPassword();
        if (username.length() < 2 || username.length() > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名 2-20 字符");
        }
        if (password.length() < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码至少 4 位");
        }
        if (authRepository.usernameExists(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
        }
        if (authRepository.registerIpExists(clientIp)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "该网络已注册过账号");
        }

        String nickname = request.getNickname() == null || request.getNickname().trim().isEmpty()
                ? username
                : request.getNickname().trim();
        long userId = authRepository.createUser(
                username,
                passwordEncoder.encode(password),
                nickname,
                appProperties.getDefaultAvatar(),
                clientIp
        );
        authRepository.consumeInviteCode(userId, inviteCode, today);
        String token = TokenUtils.newToken();
        // 数据库里只存 token 摘要，库泄露时不会直接暴露登录态。
        authRepository.createSession(userId, TokenUtils.sha256(token));
        UserSummary user = authRepository.findUserSummaryById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "注册失败"));
        return new UserAuthView(user.id(), user.username(), user.nickname(), user.avatar(), user.role(), token);
    }

    public UserAuthView login(LoginRequest request) {
        var dbUser = authRepository.findDbUserByUsername(request.getUsername().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), dbUser.passwordHash())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "用户名或密码错误");
        }
        String token = TokenUtils.newToken();
        // 登录成功后签发一条新的 session，和旧版多设备并存的行为一致。
        authRepository.createSession(dbUser.id(), TokenUtils.sha256(token));
        return new UserAuthView(dbUser.id(), dbUser.username(), dbUser.nickname(), dbUser.avatar(), dbUser.role(), token);
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        authRepository.deleteSessionByTokenHash(TokenUtils.sha256(token));
    }
}
