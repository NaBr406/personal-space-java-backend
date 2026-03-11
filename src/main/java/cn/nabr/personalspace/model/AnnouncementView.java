package cn.nabr.personalspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 公告列表 / 详情返回给前端的结构。
 */
public record AnnouncementView(
        long id,
        @JsonProperty("user_id") long userId,
        String title,
        String content,
        int pinned,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("author_name") String authorName,
        @JsonProperty("author_avatar") String authorAvatar
) {}
