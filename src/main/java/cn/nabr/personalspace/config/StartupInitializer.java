package cn.nabr.personalspace.config;

import cn.nabr.personalspace.repository.AuthRepository;
import cn.nabr.personalspace.util.InviteCodeDate;
import cn.nabr.personalspace.util.InviteCodeGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;

/**
 * 启动收尾初始化。
 * 负责准备目录、补超管账号、补当天邀请码。
 */
@Component
public class StartupInitializer {
    private final AppProperties appProperties;
    private final AuthRepository authRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public StartupInitializer(AppProperties appProperties, AuthRepository authRepository) {
        this.appProperties = appProperties;
        this.authRepository = authRepository;
    }

    /**
     * 这些动作都设计成幂等的，服务重复启动也不会反复脏写。
     */
    @PostConstruct
    public void init() throws Exception {
        Files.createDirectories(appProperties.dataDirPath());
        Files.createDirectories(appProperties.uploadDirPath());

        if (!authRepository.hasSuperAdmin()) {
            authRepository.createSuperAdmin(
                    appProperties.getSuperAdminUsername(),
                    passwordEncoder.encode(appProperties.getSuperAdminPassword()),
                    appProperties.getSuperAdminNickname(),
                    appProperties.getDefaultAvatar()
            );
        }

        String today = InviteCodeDate.todayUtc();
        if (!authRepository.hasUnusedInviteCodeForDate(today)) {
            authRepository.createInviteCode(InviteCodeGenerator.generate(), today);
        }
    }
}
