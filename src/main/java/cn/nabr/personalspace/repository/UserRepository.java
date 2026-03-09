package cn.nabr.personalspace.repository;

import cn.nabr.personalspace.model.UserSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserSummary> findSummaryById(long userId) {
        List<UserSummary> users = jdbcTemplate.query(
                "SELECT id, username, nickname, avatar, role FROM users WHERE id = ?",
                (rs, rowNum) -> new UserSummary(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("role")
                ),
                userId
        );
        return users.stream().findFirst();
    }

    public Optional<String> findPasswordHashById(long userId) {
        List<String> hashes = jdbcTemplate.query(
                "SELECT password_hash FROM users WHERE id = ?",
                (rs, rowNum) -> rs.getString("password_hash"),
                userId
        );
        return hashes.stream().findFirst();
    }

    public Optional<String> findAvatarById(long userId) {
        List<String> avatars = jdbcTemplate.query(
                "SELECT avatar FROM users WHERE id = ?",
                (rs, rowNum) -> rs.getString("avatar"),
                userId
        );
        return avatars.stream().findFirst();
    }

    public void updateNickname(long userId, String nickname) {
        jdbcTemplate.update("UPDATE users SET nickname = ? WHERE id = ?", nickname, userId);
    }

    public void updateAvatar(long userId, String avatar) {
        jdbcTemplate.update("UPDATE users SET avatar = ? WHERE id = ?", avatar, userId);
    }

    public void updatePasswordHash(long userId, String passwordHash) {
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }
}
