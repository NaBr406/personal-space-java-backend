package cn.nabr.personalspace.service;

import cn.nabr.personalspace.model.AnnouncementView;
import cn.nabr.personalspace.repository.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    @Test
    void returnsAllAnnouncementsWhenFrontendOmitsPaginationParams() {
        List<AnnouncementView> announcements = List.of(announcement(1L), announcement(2L));
        when(announcementRepository.countAll()).thenReturn(25);
        when(announcementRepository.findPage(1, 25)).thenReturn(announcements);

        Map<String, Object> body = announcementService.listAnnouncements(null, null);

        assertEquals(announcements, body.get("announcements"));
        assertEquals(1, pagination(body).get("page"));
        assertEquals(25, pagination(body).get("limit"));
        assertEquals(25, pagination(body).get("total"));
        assertEquals(1, pagination(body).get("pages"));
        verify(announcementRepository).findPage(1, 25);
    }

    @Test
    void keepsExplicitPaginationForPagedClients() {
        when(announcementRepository.countAll()).thenReturn(70);
        when(announcementRepository.findPage(2, 50)).thenReturn(List.of());

        Map<String, Object> body = announcementService.listAnnouncements(2, 200);

        assertEquals(2, pagination(body).get("page"));
        assertEquals(50, pagination(body).get("limit"));
        assertEquals(70, pagination(body).get("total"));
        assertEquals(2, pagination(body).get("pages"));
        verify(announcementRepository).findPage(2, 50);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pagination(Map<String, Object> body) {
        return (Map<String, Object>) body.get("pagination");
    }

    private AnnouncementView announcement(long id) {
        return new AnnouncementView(
                id,
                1L,
                "Announcement " + id,
                "Content " + id,
                0,
                "2026-03-10 00:00:00",
                "Admin",
                "/default-avatar.png"
        );
    }
}
