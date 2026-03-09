package cn.nabr.personalspace.model;

public record VisitorView(
        long id,
        Long userId,
        String ip,
        String visitedAt,
        String nickname,
        String avatar
) {}
