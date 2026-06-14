package cn.nabr.personalspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 动态接口返回结构。
 * 兼容了旧前端的单图字段和多图字段。
 */
public record PostView(
        long id,
        String content,
        String image,
        String thumbnail,
        String images,
        String thumbnails,
        @JsonProperty("user_id") long userId,
        int views,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("author_name") String authorName,
        @JsonProperty("author_avatar") String authorAvatar,
        @JsonProperty("like_count") int likeCount,
        @JsonProperty("comment_count") int commentCount,
        Boolean liked
) {}
