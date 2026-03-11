package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.dto.*;
import cn.nabr.personalspace.security.AuthHelper;
import cn.nabr.personalspace.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户相关接口。
 * 包括个人资料、密码、邀请码、访客记录，以及后台用户管理。
 */
@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final AuthHelper authHelper;

    public UserController(UserService userService, AuthHelper authHelper) {
        this.userService = userService;
        this.authHelper = authHelper;
    }

    @PutMapping(value = "/me", consumes = "application/json")
    public Object updateProfileJson(@RequestBody UpdateProfileRequest request, HttpServletRequest httpRequest) {
        var user = authHelper.requireUser(httpRequest);
        return userService.updateProfile(user, request);
    }

    /**
     * 改昵称和传头像会一起走 multipart，和旧前端的表单提交方式保持一致。
     */
    @PutMapping(value = "/me", consumes = "multipart/form-data")
    public Object updateProfileMultipart(
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            HttpServletRequest httpRequest
    ) {
        var user = authHelper.requireUser(httpRequest);
        return userService.updateProfileMultipart(user, nickname, avatar);
    }

    @PostMapping("/change-password-direct")
    public Object changePasswordDirect(@RequestBody ChangePasswordDirectRequest request, HttpServletRequest httpRequest) {
        var user = authHelper.requireUser(httpRequest);
        return userService.changePasswordDirect(user, request);
    }

    @PostMapping("/change-password")
    public Object changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        var user = authHelper.requireUser(httpRequest);
        return userService.changePassword(user, request);
    }

    @GetMapping("/users")
    public Object listUsers(HttpServletRequest request) {
        authHelper.requireSuperAdmin(request);
        return userService.listUsers();
    }

    @PutMapping("/users/{id}/role")
    public Object updateRole(@PathVariable long id, @RequestBody RoleUpdateRequest request, HttpServletRequest httpRequest) {
        authHelper.requireSuperAdmin(httpRequest);
        return userService.updateRole(id, request);
    }

    @DeleteMapping("/users/{id}")
    public Object deleteUser(@PathVariable long id, HttpServletRequest request) {
        authHelper.requireSuperAdmin(request);
        return userService.deleteUser(id);
    }

    @GetMapping("/invite-code")
    public Object getInviteCode(HttpServletRequest request) {
        authHelper.requireSuperAdmin(request);
        return userService.getInviteCode();
    }

    @PostMapping("/invite-code/refresh")
    public Object refreshInviteCode(HttpServletRequest request) {
        authHelper.requireSuperAdmin(request);
        return userService.refreshInviteCode();
    }

    @PostMapping("/users/{id}/reset-code")
    public Object createResetCode(@PathVariable long id, HttpServletRequest request) {
        authHelper.requireSuperAdmin(request);
        return userService.createResetCode(id);
    }

    @GetMapping("/users/{id}/reset-code")
    public Object getResetCode(@PathVariable long id, HttpServletRequest request) {
        authHelper.requireSuperAdmin(request);
        return userService.getResetCode(id);
    }

    @PostMapping("/reset-password")
    public Object resetPassword(@RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(request);
    }

    /**
     * 访客上报允许匿名访问，用来给后台统计最近访问情况。
     */
    @PostMapping("/visit")
    public Object recordVisit(HttpServletRequest request) {
        var user = authHelper.getUser(request).orElse(null);
        String ip = UserService.extractClientIp(request);
        String userAgent = request.getHeader("user-agent");
        if (userAgent == null) {
            userAgent = "";
        }
        if (userAgent.length() > 200) {
            userAgent = userAgent.substring(0, 200);
        }
        return userService.recordVisit(user, ip, userAgent);
    }

    @GetMapping("/visitors")
    public Object listVisitors(@RequestParam(defaultValue = "50") int limit, HttpServletRequest request) {
        authHelper.requireSuperAdmin(request);
        return userService.listVisitors(limit);
    }
}
