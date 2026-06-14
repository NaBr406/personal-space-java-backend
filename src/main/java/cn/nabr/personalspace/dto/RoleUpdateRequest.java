package cn.nabr.personalspace.dto;

/**
 * 用户角色修改请求。
 */
public class RoleUpdateRequest {
    // 目标角色，目前只允许 guest / admin。
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
