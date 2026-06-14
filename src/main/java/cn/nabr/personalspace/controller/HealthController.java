package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.config.AppProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 健康检查接口。
 * 主要给部署后的 smoke test 和反向代理探活使用。
 */
@RestController
@RequestMapping("/api")
public class HealthController {
    private final AppProperties appProperties;

    public HealthController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * 顺手带上环境名和当前时间，方便区分正式 / 沙盒实例。
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "ok", true,
                "app", "personal-space-java-backend",
                "env", appProperties.getEnvName(),
                "time", OffsetDateTime.now().toString()
        );
    }
}
