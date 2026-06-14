package cn.nabr.personalspace.dto;

/**
 * 常规改密码请求。
 */
public class ChangePasswordRequest {
    // 旧密码。
    private String oldPassword;
    // 新密码。
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
