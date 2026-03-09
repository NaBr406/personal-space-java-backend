package cn.nabr.personalspace.repository;

import cn.nabr.personalspace.model.UserAdminView;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.model.VisitorView;
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

    public Optional<UserAdminView> findAdminViewById(long userId) {
        List<UserAdminView> users = jdbcTemplate.query(
                "SELECT id, username, nickname, avatar, role, created_at FROM users WHERE id = ?",
                (rs, rowNum) -> new UserAdminView(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("role"),
                        rs.getString("created_at")
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

    public Optional<UserIdentity> findIdentityByUsername(String username) {
        List<UserIdentity> users = jdbcTemplate.query(
                "SELECT id, username, role FROM users WHERE username = ?",
                (rs, rowNum) -> new UserIdentity(rs.getLong("id"), rs.getString("username"), rs.getString("role")),
                username
        );
        return users.stream().findFirst();
    }

    public List<UserAdminView> listUsers() {
        return jdbcTemplate.query(
                "SELECT id, username, nickname, avatar, role, created_at FROM users ORDER BY created_at DESC",
                (rs, rowNum) -> new UserAdminView(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("role"),
                        rs.getString("created_at")
                )
        );
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

    public void updateRole(long userId, String role) {
        jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", role, userId);
    }

    public Optional<String> findTodayInviteCode() {
        List<String> codes = jdbcTemplate.query(
                "SELECT code FROM invite_codes WHERE created_date = date('now') AND used_by IS NULL LIMIT 1",
                (rs, rowNum) -> rs.getString("code")
        );
        return codes.stream().findFirst();
    }

    public void deleteUnusedInviteCodesToday() {
        jdbcTemplate.update("DELETE FROM invite_codes WHERE created_date = date('now') AND used_by IS NULL");
    }

    public void invalidateResetCodes(long userId) {
        jdbcTemplate.update("UPDATE password_reset_codes SET used = 1 WHERE user_id = ? AND used = 0", userId);
    }

    public void createResetCode(long userId, String code) {
        jdbcTemplate.update("INSERT INTO password_reset_codes (user_id, code) VALUES (?, ?)", userId, code);
    }

    public Optional<String> getLatestUnusedResetCode(long userId) {
        List<String> codes = jdbcTemplate.query(
                "SELECT code FROM password_reset_codes WHERE user_id = ? AND used = 0 ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("code"),
                userId
        );
        return codes.stream().findFirst();
    }

    public Optional<ResetCodeRecord> findValidResetCode(long userId, String code) {
        List<ResetCodeRecord> list = jdbcTemplate.query(
                "SELECT id, user_id, code FROM password_reset_codes WHERE user_id = ? AND code = ? AND used = 0",
                (rs, rowNum) -> new ResetCodeRecord(rs.getLong("id"), rs.getLong("user_id"), rs.getString("code")),
                userId,
                code
        );
        return list.stream().findFirst();
    }

    public void markResetCodeUsed(long resetCodeId) {
        jdbcTemplate.update("UPDATE password_reset_codes SET used = 1 WHERE id = ?", resetCodeId);
    }

    public void recordVisit(Long userId, String ip, String userAgent) {
        jdbcTemplate.update("INSERT INTO visitors (user_id, ip, user_agent) VALUES (?, ?, ?)", userId, ip, userAgent);
    }

    public boolean hasRecentVisitByUser(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM visitors WHERE user_id = ? AND visited_at > datetime('now','localtime','-5 minutes')",
                Integer.class,
                userId
        );
        return count != null && count > 0;
    }

    public boolean hasRecentAnonymousVisit(String ip) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM visitors WHERE user_id IS NULL AND ip = ? AND visited_at > datetime('now','localtime','-5 minutes')",
                Integer.class,
                ip
        );
        return count != null && count > 0;
    }

    public List<VisitorView> listVisitors(int limit) {
        return jdbcTemplate.query(
                """
                SELECT v.id, v.user_id, v.ip, v.visited_at,
                       u.nickname, u.avatar
                FROM visitors v
                LEFT JOIN users u ON v.user_id = u.id
                ORDER BY v.visited_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new VisitorView(
                        rs.getLong("id"),
                        rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        rs.getString("ip"),
                        rs.getString("visited_at"),
                        rs.getString("nickname"),
                        rs.getString("avatar")
                ),
                limit
        );
    }

    public void deleteUserDeep(long userId) {
        jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ? OR from_user_id = ?", userId, userId);
        jdbcTemplate.update("DELETE FROM likes WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM comments WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM sessions WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM password_reset_codes WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM invite_codes WHERE used_by = ?", userId);
        jdbcTemplate.update("DELETE FROM visitors WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM articles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM posts WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    public record UserIdentity(long id, String username, String role) {}
    public record ResetCodeRecord(long id, long userId, String code) {}
}
