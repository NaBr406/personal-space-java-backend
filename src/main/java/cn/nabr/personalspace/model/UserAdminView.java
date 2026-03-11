package cn.nabr.personalspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 管理后台查看用户时使用的结构。
 */
public record UserAdminView(
        long id,
        String username,
        String nickname,
        String avatar,
        String role,
        @JsonProperty("created_at") String createdAt
) {}
