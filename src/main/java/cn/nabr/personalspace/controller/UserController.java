package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.dto.ChangePasswordDirectRequest;
import cn.nabr.personalspace.dto.ChangePasswordRequest;
import cn.nabr.personalspace.dto.UpdateProfileRequest;
import cn.nabr.personalspace.security.AuthHelper;
import cn.nabr.personalspace.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
