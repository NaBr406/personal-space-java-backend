package cn.nabr.personalspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VisitorView(
        long id,
        @JsonProperty("user_id") Long userId,
        String ip,
        @JsonProperty("visited_at") String visitedAt,
        String nickname,
        String avatar
) {}
