package cn.nabr.personalspace.service;

import cn.nabr.personalspace.dto.AnnouncementRequest;
import cn.nabr.personalspace.exception.ApiException;
import cn.nabr.personalspace.model.AnnouncementView;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.AnnouncementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    public Map<String, Object> listAnnouncements(Integer page, Integer limit) {
        int total = announcementRepository.countAll();
        boolean useFrontendDefault = page == null && limit == null;
        int safePage = useFrontendDefault ? 1 : Math.max(1, page == null ? 1 : page);
        int safeLimit = useFrontendDefault
                ? Math.max(total, 1)
                : Math.min(50, Math.max(1, limit == null ? 20 : limit));
        var announcements = total == 0
                ? java.util.List.<AnnouncementView>of()
                : announcementRepository.findPage(safePage, safeLimit);

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", safePage);
        pagination.put("limit", safeLimit);
        pagination.put("total", total);
        pagination.put("pages", (int) Math.ceil(total / (double) safeLimit));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("announcements", announcements);
        body.put("pagination", pagination);
        return body;
    }

    public AnnouncementView getAnnouncement(long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "公告不存在"));
    }

    @Transactional
    public AnnouncementView createAnnouncement(AnnouncementRequest request, UserSummary user) {
        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (title.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题不能为空");
        }
        if (content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "内容不能为空");
        }
        boolean pinned = Boolean.TRUE.equals(request.getPinned());
        long id = announcementRepository.create(user.id(), title, content, pinned);
        return getAnnouncement(id);
    }

    @Transactional
    public Map<String, Object> deleteAnnouncement(long id) {
        getAnnouncement(id);
        announcementRepository.delete(id);
        return Map.of("ok", true);
    }

    @Transactional
    public Map<String, Object> togglePin(long id) {
        AnnouncementView current = getAnnouncement(id);
        boolean pinned = current.pinned() == 0;
        announcementRepository.updatePinned(id, pinned);
        return Map.of("ok", true, "pinned", pinned);
    }
}
