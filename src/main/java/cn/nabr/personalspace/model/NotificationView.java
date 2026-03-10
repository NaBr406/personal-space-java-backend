package cn.nabr.personalspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotificationView(
        long id,
        @JsonProperty("user_id") long userId,
        String type,
        @JsonProperty("from_user_id") Long fromUserId,
        @JsonProperty("post_id") Long postId,
        @JsonProperty("comment_id") Long commentId,
        String content,
        @JsonProperty("is_read") int isRead,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("from_nickname") String fromNickname,
        @JsonProperty("from_avatar") String fromAvatar,
        @JsonProperty("post_content") String postContent
) {}
