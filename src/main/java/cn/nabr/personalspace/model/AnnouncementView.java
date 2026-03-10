package cn.nabr.personalspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
