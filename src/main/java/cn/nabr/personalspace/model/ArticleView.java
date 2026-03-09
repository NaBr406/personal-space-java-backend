package cn.nabr.personalspace.model;

public record ArticleView(
        long id,
        String category,
        String title,
        String content,
        String summary,
        String coverImage,
        long userId,
        int views,
        String createdAt,
        String updatedAt,
        String authorName,
        String authorAvatar
) {}
