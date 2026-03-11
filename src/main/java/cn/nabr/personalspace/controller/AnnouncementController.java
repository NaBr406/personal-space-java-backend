package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.dto.AnnouncementRequest;
import cn.nabr.personalspace.security.AuthHelper;
import cn.nabr.personalspace.service.AnnouncementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公告相关接口。
 * 负责公告列表、详情，以及超级管理员的发布/删除/置顶操作。
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;
    private final AuthHelper authHelper;

    public AnnouncementController(AnnouncementService announcementService, AuthHelper authHelper) {
        this.announcementService = announcementService;
        this.authHelper = authHelper;
    }

    /**
     * 前端不传 page/limit 时，会走 service 里的兼容逻辑直接返回完整列表。
     */
    @GetMapping
    public Map<String, Object> listAnnouncements(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
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

    /**
     * 置顶接口本质是切换状态，不区分“置顶”和“取消置顶”两个单独动作。
     */
    @PatchMapping("/{id}/pin")
    public Object togglePin(@PathVariable long id, HttpServletRequest httpRequest) {
        authHelper.requireSuperAdmin(httpRequest);
        return announcementService.togglePin(id);
    }
}
