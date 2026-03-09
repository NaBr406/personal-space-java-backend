package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.security.AuthHelper;
import cn.nabr.personalspace.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final AuthHelper authHelper;

    public NotificationController(NotificationService notificationService, AuthHelper authHelper) {
        this.notificationService = notificationService;
        this.authHelper = authHelper;
    }

    @GetMapping
    public Object list(HttpServletRequest request) {
        var user = authHelper.requireUser(request);
        return notificationService.list(user.id());
    }

    @GetMapping("/unread-count")
    public Object unreadCount(HttpServletRequest request) {
        var user = authHelper.requireUser(request);
        return notificationService.unreadCount(user.id());
    }

    @PostMapping("/read-all")
    public Object readAll(HttpServletRequest request) {
        var user = authHelper.requireUser(request);
        return notificationService.readAll(user.id());
    }

    @PostMapping("/{id}/read")
    public Object readOne(@PathVariable long id, HttpServletRequest request) {
        var user = authHelper.requireUser(request);
        return notificationService.readOne(id, user.id());
    }
}
