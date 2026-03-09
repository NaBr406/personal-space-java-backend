package cn.nabr.personalspace.model;

public record UserAdminView(
        long id,
        String username,
        String nickname,
        String avatar,
        String role,
        String createdAt
) {}
