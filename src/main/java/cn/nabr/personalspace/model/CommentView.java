package cn.nabr.personalspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommentView(
        long id,
        @JsonProperty("post_id") long postId,
        @JsonProperty("user_id") long userId,
        String content,
        @JsonProperty("parent_id") Long parentId,
        @JsonProperty("reply_to_user_id") Long replyToUserId,
        @JsonProperty("created_at") String createdAt,
        String nickname,
        String avatar,
        @JsonProperty("reply_to_nickname") String replyToNickname
) {}
