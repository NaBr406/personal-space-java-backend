package cn.nabr.personalspace.config;

import cn.nabr.personalspace.repository.AuthRepository;
import cn.nabr.personalspace.util.InviteCodeDate;
import cn.nabr.personalspace.util.InviteCodeGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;

@Component
public class StartupInitializer {
    private final AppProperties appProperties;
    private final AuthRepository authRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public StartupInitializer(AppProperties appProperties, AuthRepository authRepository) {
        this.appProperties = appProperties;
        this.authRepository = authRepository;
    }

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
