package cn.nabr.personalspace.model;

public record NotificationView(
        long id,
        long userId,
        String type,
        Long fromUserId,
        Long postId,
        Long commentId,
        String content,
        int isRead,
        String createdAt,
        String fromNickname,
        String fromAvatar,
        String postContent
) {}
