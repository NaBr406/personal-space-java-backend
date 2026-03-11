package cn.nabr.personalspace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层静态资源配置。
 * 把本地上传目录映射到 /uploads/**，让前端能直接访问图片。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * 上传文件不走打包资源目录，而是直接映射到磁盘路径。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = appProperties.uploadDirPath().toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
