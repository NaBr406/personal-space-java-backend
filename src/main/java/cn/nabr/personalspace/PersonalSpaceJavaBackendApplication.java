package cn.nabr.personalspace;

import cn.nabr.personalspace.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 应用启动入口。
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class PersonalSpaceJavaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalSpaceJavaBackendApplication.class, args);
    }
}
