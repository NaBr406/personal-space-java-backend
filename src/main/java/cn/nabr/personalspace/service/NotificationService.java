package cn.nabr.personalspace.service;

import cn.nabr.personalspace.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Object list(long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public Map<String, Object> unreadCount(long userId) {
        return Map.of("count", notificationRepository.countUnread(userId));
    }

    public Map<String, Object> readAll(long userId) {
        notificationRepository.markAllRead(userId);
        return Map.of("ok", true);
    }

    public Map<String, Object> readOne(long id, long userId) {
        notificationRepository.markOneRead(id, userId);
        return Map.of("ok", true);
    }
}
