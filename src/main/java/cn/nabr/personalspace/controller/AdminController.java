package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.security.AuthHelper;
import cn.nabr.personalspace.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final AuthHelper authHelper;

    public AdminController(AdminService adminService, AuthHelper authHelper) {
        this.adminService = adminService;
        this.authHelper = authHelper;
    }

    @GetMapping("/summary")
    public Object summary(HttpServletRequest request) {
        var user = authHelper.requireAdmin(request);
        return adminService.summary(user);
    }

    @GetMapping("/posts")
    public Object posts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String author,
            HttpServletRequest request
    ) {
        authHelper.requireAdmin(request);
        return adminService.posts(keyword, author, page, limit);
    }

    @GetMapping("/comments")
    public Object comments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String author,
            HttpServletRequest request
    ) {
        authHelper.requireAdmin(request);
        return adminService.comments(keyword, author, page, limit);
    }

    @GetMapping("/articles")
    public Object articles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String author,
            @RequestParam(defaultValue = "") String category,
            HttpServletRequest request
    ) {
        authHelper.requireAdmin(request);
        return adminService.articles(keyword, author, category, page, limit);
    }

    @GetMapping("/announcements")
    public Object announcements(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String author,
            HttpServletRequest request
    ) {
        authHelper.requireAdmin(request);
        return adminService.announcements(keyword, author);
    }

    @GetMapping("/logs")
    public Object logs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request
    ) {
        authHelper.requireAdmin(request);
        return adminService.logs(page, limit);
    }

    @GetMapping("/settings")
    public Object settings(HttpServletRequest request) {
        authHelper.requireSuperAdmin(request);
        return adminService.settings();
    }

    @PutMapping("/settings")
    public Object updateSettings(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        var user = authHelper.requireSuperAdmin(request);
        return adminService.updateSettings(user, body);
    }
}
