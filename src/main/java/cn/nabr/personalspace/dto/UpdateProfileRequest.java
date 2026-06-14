package cn.nabr.personalspace.dto;

/**
 * 个人资料更新请求。
 */
public class UpdateProfileRequest {
    // 新昵称。
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
