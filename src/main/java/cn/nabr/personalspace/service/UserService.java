package cn.nabr.personalspace.service;

import cn.nabr.personalspace.dto.*;
import cn.nabr.personalspace.exception.ApiException;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.AuthRepository;
import cn.nabr.personalspace.repository.UserRepository;
import cn.nabr.personalspace.util.InviteCodeGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UploadService uploadService;
    private final AuthRepository authRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, UploadService uploadService, AuthRepository authRepository) {
        this.userRepository = userRepository;
        this.uploadService = uploadService;
        this.authRepository = authRepository;
    }

    public UserSummary updateProfile(UserSummary user, UpdateProfileRequest request) {
        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();
        if (!nickname.isEmpty()) {
            userRepository.updateNickname(user.id(), nickname);
        }
        return userRepository.findSummaryById(user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    public UserSummary updateProfileMultipart(UserSummary user, String nickname, MultipartFile avatar) {
        String safeNickname = nickname == null ? "" : nickname.trim();
        if (!safeNickname.isEmpty()) {
            userRepository.updateNickname(user.id(), safeNickname);
        }
        if (avatar != null && !avatar.isEmpty()) {
            String oldAvatar = userRepository.findAvatarById(user.id()).orElse(null);
            String newAvatar = uploadService.saveImage(avatar);
            userRepository.updateAvatar(user.id(), newAvatar);
            if (oldAvatar != null && !oldAvatar.equals("/default-avatar.png")) {
                uploadService.deleteIfUploaded(oldAvatar);
            }
        }
        return userRepository.findSummaryById(user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    public Map<String, Object> changePasswordDirect(UserSummary user, ChangePasswordDirectRequest request) {
        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码至少 4 位");
        }
        userRepository.updatePasswordHash(user.id(), passwordEncoder.encode(newPassword));
        return Map.of("ok", true);
    }

    public Map<String, Object> changePassword(UserSummary user, ChangePasswordRequest request) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        if (oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请输入旧密码和新密码");
        }
        if (newPassword.length() < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "新密码至少 4 位");
        }
        String oldHash = userRepository.findPasswordHashById(user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (!passwordEncoder.matches(oldPassword, oldHash)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "旧密码错误");
        }
        userRepository.updatePasswordHash(user.id(), passwordEncoder.encode(newPassword));
        return Map.of("ok", true);
    }

    public Object listUsers() {
        return userRepository.listUsers();
    }

    @Transactional
    public Map<String, Object> updateRole(long userId, RoleUpdateRequest request) {
        String role = request.getRole();
        if (!"guest".equals(role) && !"admin".equals(role)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "角色只能是 guest 或 admin");
        }
        var target = userRepository.findAdminViewById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        if ("superadmin".equals(target.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "不能修改超级管理员");
        }
        userRepository.updateRole(userId, role);
        return Map.of("ok", true);
    }

    public Map<String, Object> getInviteCode() {
        String code = userRepository.findTodayInviteCode().orElseGet(() -> {
            String newCode = InviteCodeGenerator.generate();
            authRepository.createInviteCode(newCode, java.time.LocalDate.now().toString());
            return newCode;
        });
        return Map.of("code", code, "date", java.time.LocalDate.now().toString());
    }

    @Transactional
    public Map<String, Object> refreshInviteCode() {
        userRepository.deleteUnusedInviteCodesToday();
        String newCode = InviteCodeGenerator.generate();
        authRepository.createInviteCode(newCode, java.time.LocalDate.now().toString());
        return Map.of("code", newCode, "date", java.time.LocalDate.now().toString());
    }

    @Transactional
    public Map<String, Object> createResetCode(long userId) {
        UserRepository.UserIdentity user = userRepository.findAdminViewById(userId)
                .map(v -> new UserRepository.UserIdentity(v.id(), v.username(), v.role()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        userRepository.invalidateResetCodes(user.id());
        String code = InviteCodeGenerator.generate();
        userRepository.createResetCode(user.id(), code);
        return Map.of("code", code);
    }

    public Map<String, Object> getResetCode(long userId) {
        userRepository.findAdminViewById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        return Map.of("code", userRepository.getLatestUnusedResetCode(userId).orElse(null));
    }

    @Transactional
    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        if (request.getUsername() == null || request.getCode() == null || request.getNewPassword() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写完整");
        }
        if (request.getNewPassword().length() < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码至少 4 位");
        }
        UserRepository.UserIdentity user = userRepository.findIdentityByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        UserRepository.ResetCodeRecord resetCode = userRepository.findValidResetCode(user.id(), request.getCode().trim().toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "校验码无效或已使用"));
        userRepository.updatePasswordHash(user.id(), passwordEncoder.encode(request.getNewPassword()));
        userRepository.markResetCodeUsed(resetCode.id());
        return Map.of("ok", true);
    }

    public Map<String, Object> recordVisit(UserSummary user, String ip, String userAgent) {
        boolean recent = user != null
                ? userRepository.hasRecentVisitByUser(user.id())
                : userRepository.hasRecentAnonymousVisit(ip);
        if (!recent) {
            userRepository.recordVisit(user == null ? null : user.id(), ip, userAgent);
        }
        return Map.of("ok", true);
    }

    public Object listVisitors(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return userRepository.listVisitors(safeLimit);
    }

    @Transactional
    public Map<String, Object> deleteUser(long userId) {
        var target = userRepository.findAdminViewById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        if ("superadmin".equals(target.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "不能删除超级管理员");
        }
        if (target.avatar() != null && !target.avatar().equals("/default-avatar.png")) {
            uploadService.deleteIfUploaded(target.avatar());
        }
        userRepository.deleteUserDeep(userId);
        return Map.of("message", "用户已删除");
    }

    public static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("x-forwarded-for");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim().replace("::ffff:", "");
        }
        String realIp = request.getHeader("x-real-ip");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim().replace("::ffff:", "");
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "" : remoteAddr.replace("::ffff:", "");
    }
}
