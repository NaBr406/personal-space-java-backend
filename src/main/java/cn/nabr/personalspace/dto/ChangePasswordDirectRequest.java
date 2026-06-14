package cn.nabr.personalspace.dto;

/**
 * 直接改密码请求。
 * 适合不校验旧密码的场景。
 */
public class ChangePasswordDirectRequest {
    // 新密码明文，进入 service 后再做长度校验和加密。
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
