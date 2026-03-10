package cn.nabr.personalspace.service;

import cn.nabr.personalspace.config.AppProperties;
import cn.nabr.personalspace.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {
    private static final int THUMBNAIL_MAX_WIDTH = 480;
    private static final int THUMBNAIL_MAX_HEIGHT = 480;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> ALLOWED_MIME_HINTS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final AppProperties appProperties;

    public UploadService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public record UploadedImage(String imageUrl, String thumbnailUrl) {}

    public String saveImage(MultipartFile file) {
        validateImage(file);
        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String filename = UUID.randomUUID().toString().replace("-", "") + getExtension(originalName);
        Path target = uploadDirPath().resolve(filename);
        try {
            Files.createDirectories(uploadDirPath());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "保存图片失败");
        }
        return "/uploads/" + filename;
    }

    public List<String> saveImages(List<MultipartFile> files, int maxCount) {
        List<MultipartFile> validFiles = filterNonEmptyFiles(files);
        if (validFiles.size() > maxCount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图片数量不能超过 " + maxCount + " 张");
        }

        List<String> images = new ArrayList<>();
        try {
            for (MultipartFile file : validFiles) {
                images.add(saveImage(file));
            }
            return images;
        } catch (RuntimeException e) {
            for (String image : images) {
                deleteIfUploaded(image);
            }
            throw e;
        }
    }

    public UploadedImage saveImageWithThumbnail(MultipartFile file) {
        validateImage(file);

        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String baseName = UUID.randomUUID().toString().replace("-", "");
        String imageFilename = baseName + getExtension(originalName);
        String thumbnailFilename = baseName + "_thumb.jpg";
        Path imagePath = uploadDirPath().resolve(imageFilename);
        Path thumbnailPath = uploadDirPath().resolve(thumbnailFilename);
        String imageUrl = "/uploads/" + imageFilename;
        String thumbnailUrl = imageUrl;

        try {
            Files.createDirectories(uploadDirPath());
            Files.copy(file.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            deleteIfExists(imagePath);
            deleteIfExists(thumbnailPath);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "保存图片失败");
        }

        try {
            generateThumbnail(imagePath, thumbnailPath);
            thumbnailUrl = "/uploads/" + thumbnailFilename;
        } catch (IOException | RuntimeException e) {
            deleteIfExists(thumbnailPath);
        }

        return new UploadedImage(imageUrl, thumbnailUrl);
    }

    public List<UploadedImage> saveImagesWithThumbnails(List<MultipartFile> files, int maxCount) {
        List<MultipartFile> validFiles = filterNonEmptyFiles(files);
        if (validFiles.size() > maxCount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图片数量不能超过 " + maxCount + " 张");
        }

        List<UploadedImage> images = new ArrayList<>();
        try {
            for (MultipartFile file : validFiles) {
                images.add(saveImageWithThumbnail(file));
            }
            return images;
        } catch (RuntimeException e) {
            deleteUploadedImages(images);
            throw e;
        }
    }

    public void deleteUploadedImages(List<UploadedImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (UploadedImage image : images) {
            if (image == null) {
                continue;
            }
            deleteIfUploaded(image.imageUrl());
            deleteIfUploaded(image.thumbnailUrl());
        }
    }

    public void deleteIfUploaded(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        if (!fileUrl.startsWith("/uploads/")) {
            return;
        }
        String relativePath = fileUrl.substring("/uploads/".length());
        Path basePath = uploadDirPath().toAbsolutePath().normalize();
        Path filePath = basePath.resolve(relativePath).normalize();
        if (!filePath.startsWith(basePath)) {
            return;
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择图片");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = extractOriginalExtension(originalName);
        boolean allowedExtension = ALLOWED_EXTENSIONS.contains(extension);
        boolean allowedMime = ALLOWED_MIME_HINTS.stream().anyMatch(contentType::contains);
        if (!allowedExtension || !allowedMime) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "只支持 jpg/png/gif/webp 格式");
        }
    }

    private List<MultipartFile> filterNonEmptyFiles(List<MultipartFile> files) {
        List<MultipartFile> valid = new ArrayList<>();
        if (files == null) {
            return valid;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            valid.add(file);
        }
        return valid;
    }

    private void generateThumbnail(Path sourcePath, Path thumbnailPath) throws IOException {
        BufferedImage source = ImageIO.read(sourcePath.toFile());
        if (source == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图片格式不支持");
        }

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图片内容无效");
        }

        int[] size = scaleSize(sourceWidth, sourceHeight, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
        BufferedImage thumbnail = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, size[0], size[1]);
            graphics.drawImage(source, 0, 0, size[0], size[1], null);
        } finally {
            graphics.dispose();
        }

        if (!ImageIO.write(thumbnail, "jpg", thumbnailPath.toFile())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "生成缩略图失败");
        }
    }

    private int[] scaleSize(int width, int height, int maxWidth, int maxHeight) {
        double ratio = Math.min(maxWidth / (double) width, maxHeight / (double) height);
        if (ratio > 1) {
            ratio = 1;
        }
        int newWidth = Math.max(1, (int) Math.round(width * ratio));
        int newHeight = Math.max(1, (int) Math.round(height * ratio));
        return new int[]{newWidth, newHeight};
    }

    private Path uploadDirPath() {
        return appProperties.uploadDirPath().toAbsolutePath().normalize();
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0) {
            return ".png";
        }
        String ext = filename.substring(idx).toLowerCase(Locale.ROOT);
        if (!ext.matches("\\.[a-z0-9]{1,10}")) {
            return ".png";
        }
        return ext;
    }

    private String extractOriginalExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return filename.substring(idx).toLowerCase(Locale.ROOT);
    }
}
