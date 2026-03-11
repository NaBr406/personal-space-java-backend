package cn.nabr.personalspace.model;

/**
 * 当前登录用户的轻量摘要信息。
 */
public record UserSummary(
        long id,
        String username,
        String nickname,
        String avatar,
        String role
) {}
