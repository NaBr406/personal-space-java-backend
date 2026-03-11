package cn.nabr.personalspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 文章详情和列表项的统一视图。
 */
public record ArticleView(
        long id,
        String category,
        String title,
        String content,
        String summary,
        @JsonProperty("cover_image") String coverImage,
        @JsonProperty("user_id") long userId,
        int views,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("author_name") String authorName,
        @JsonProperty("author_avatar") String authorAvatar
) {}
