package cn.nabr.personalspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserAdminView(
        long id,
        String username,
        String nickname,
        String avatar,
        String role,
        @JsonProperty("created_at") String createdAt
) {}
