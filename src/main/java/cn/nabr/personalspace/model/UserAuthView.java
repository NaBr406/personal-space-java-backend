package cn.nabr.personalspace.model;

/**
 * 登录 / 注册成功后返回给前端的用户信息和 token。
 */
public record UserAuthView(
        long id,
        String username,
        String nickname,
        String avatar,
        String role,
        String token
) {}
