package cn.nabr.personalspace.model;

public record CommentView(
        long id,
        long postId,
        long userId,
        String content,
        Long parentId,
        Long replyToUserId,
        String createdAt,
        String nickname,
        String avatar,
        String replyToNickname
) {}
