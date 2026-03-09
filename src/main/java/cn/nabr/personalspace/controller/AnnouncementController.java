package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.dto.AnnouncementRequest;
import cn.nabr.personalspace.security.AuthHelper;
import cn.nabr.personalspace.service.AnnouncementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;
    private final AuthHelper authHelper;

    public AnnouncementController(AnnouncementService announcementService, AuthHelper authHelper) {
        this.announcementService = announcementService;
        this.authHelper = authHelper;
    }

    @GetMapping
    public Map<String, Object> listAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return announcementService.listAnnouncements(page, limit);
    }

    @GetMapping("/{id}")
    public Object getAnnouncement(@PathVariable long id) {
        return announcementService.getAnnouncement(id);
    }

    @PostMapping
    public Object createAnnouncement(@RequestBody AnnouncementRequest request, HttpServletRequest httpRequest) {
        var user = authHelper.requireSuperAdmin(httpRequest);
        return announcementService.createAnnouncement(request, user);
    }

    @DeleteMapping("/{id}")
    public Object deleteAnnouncement(@PathVariable long id, HttpServletRequest httpRequest) {
        authHelper.requireSuperAdmin(httpRequest);
        return announcementService.deleteAnnouncement(id);
    }

    @PatchMapping("/{id}/pin")
    public Object togglePin(@PathVariable long id, HttpServletRequest httpRequest) {
        authHelper.requireSuperAdmin(httpRequest);
        return announcementService.togglePin(id);
    }
}
