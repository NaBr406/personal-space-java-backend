package cn.nabr.personalspace.service;

import cn.nabr.personalspace.config.AppProperties;
import cn.nabr.personalspace.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {
    private final AppProperties appProperties;

    public UploadService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "只能上传图片文件");
        }
        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = getExtension(originalName);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = appProperties.uploadDirPath().resolve(filename);
        try {
            Files.createDirectories(appProperties.uploadDirPath());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "保存图片失败");
        }
        return "/uploads/" + filename;
    }

    public List<String> saveImages(List<MultipartFile> files, int maxCount) {
        List<String> paths = new ArrayList<>();
        if (files == null) {
            return paths;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            paths.add(saveImage(file));
            if (paths.size() > maxCount) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "图片数量不能超过 " + maxCount + " 张");
            }
        }
        return paths;
    }

    public void deleteIfUploaded(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        if (!fileUrl.startsWith("/uploads/")) {
            return;
        }
        Path filePath = appProperties.uploadDirPath().resolve(fileUrl.substring("/uploads/".length()));
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0) {
            return ".png";
        }
        return filename.substring(idx);
    }
}
