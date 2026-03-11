package cn.nabr.personalspace.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 注册请求体。
 */
public class RegisterRequest {
    // 登录用户名。
    @NotBlank(message = "用户名和密码必填")
    private String username;
    // 登录密码。
    @NotBlank(message = "用户名和密码必填")
    private String password;
    // 展示昵称；不传时会回退成用户名。
    private String nickname;
    // 当天有效的邀请码。
    @NotBlank(message = "请填写邀请码")
    private String inviteCode;
    // 验证码题目的 token。
    @NotBlank(message = "请完成验证码")
    private String captchaToken;
    // 用户提交的验证码答案。
    @NotBlank(message = "请完成验证码")
    private String captchaAnswer;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
    public String getCaptchaAnswer() { return captchaAnswer; }
    public void setCaptchaAnswer(String captchaAnswer) { this.captchaAnswer = captchaAnswer; }
}
