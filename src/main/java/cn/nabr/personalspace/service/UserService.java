package cn.nabr.personalspace.service;

import cn.nabr.personalspace.dto.ChangePasswordDirectRequest;
import cn.nabr.personalspace.dto.ChangePasswordRequest;
import cn.nabr.personalspace.dto.UpdateProfileRequest;
import cn.nabr.personalspace.exception.ApiException;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UploadService uploadService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, UploadService uploadService) {
        this.userRepository = userRepository;
        this.uploadService = uploadService;
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
}
