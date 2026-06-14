package cn.nabr.personalspace.dto;

/**
 * 用重置码找回密码的请求体。
 */
public class ResetPasswordRequest {
    // 要重置的用户名。
    private String username;
    // 超管生成的重置码。
    private String code;
    // 新密码。
    private String newPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
