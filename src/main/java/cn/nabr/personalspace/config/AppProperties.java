package cn.nabr.personalspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * 项目自定义配置。
 * 主要放数据目录、上传目录、默认头像，以及初始化超管账号这些参数。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String envName = "sandbox";
    private String dataDir = "./data";
    private String uploadDir = "./data/uploads";
    private String defaultAvatar = "/default-avatar.png";
    private String superAdminUsername = "NaBr406";
    private String superAdminNickname = "NaBr406";
    private String superAdminPassword = "admin123";

    public String getEnvName() { return envName; }
    public void setEnvName(String envName) { this.envName = envName; }
    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    public String getDefaultAvatar() { return defaultAvatar; }
    public void setDefaultAvatar(String defaultAvatar) { this.defaultAvatar = defaultAvatar; }
    public String getSuperAdminUsername() { return superAdminUsername; }
    public void setSuperAdminUsername(String superAdminUsername) { this.superAdminUsername = superAdminUsername; }
    public String getSuperAdminNickname() { return superAdminNickname; }
    public void setSuperAdminNickname(String superAdminNickname) { this.superAdminNickname = superAdminNickname; }
    public String getSuperAdminPassword() { return superAdminPassword; }
    public void setSuperAdminPassword(String superAdminPassword) { this.superAdminPassword = superAdminPassword; }

    /**
     * 统一把数据目录转成规范化 Path，方便文件操作复用。
     */
    public Path dataDirPath() {
        return Path.of(dataDir).normalize();
    }

    /**
     * 上传目录和静态资源映射都走这里，避免每处自己拼路径。
     */
    public Path uploadDirPath() {
        return Path.of(uploadDir).normalize();
    }
}
