package cn.nabr.personalspace.service;

import cn.nabr.personalspace.config.AppProperties;
import cn.nabr.personalspace.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void fallsBackToOriginalImageWhenThumbnailGenerationFails() {
        UploadService uploadService = new UploadService(appProperties());
        MockMultipartFile webpFile = new MockMultipartFile(
                "image",
                "post-image.webp",
                "image/webp",
                "not-a-decodable-webp".getBytes()
        );

        UploadService.UploadedImage uploaded = uploadService.saveImageWithThumbnail(webpFile);

        assertEquals(uploaded.imageUrl(), uploaded.thumbnailUrl());
        assertTrue(Files.exists(uploadPath(uploaded.imageUrl())));
    }

    @Test
    void keepsDedicatedThumbnailForDecodableImages() throws IOException {
        UploadService uploadService = new UploadService(appProperties());
        MockMultipartFile pngFile = new MockMultipartFile(
                "image",
                "post-image.png",
                "image/png",
                pngBytes()
        );

        UploadService.UploadedImage uploaded = uploadService.saveImageWithThumbnail(pngFile);

        assertNotEquals(uploaded.imageUrl(), uploaded.thumbnailUrl());
        assertTrue(Files.exists(uploadPath(uploaded.imageUrl())));
        assertTrue(Files.exists(uploadPath(uploaded.thumbnailUrl())));
    }

    @Test
    void rejectsFormatsOutsideFrontendUploadContract() {
        UploadService uploadService = new UploadService(appProperties());
        MockMultipartFile svgFile = new MockMultipartFile(
                "image",
                "vector.svg",
                "image/svg+xml",
                "<svg></svg>".getBytes()
        );

        ApiException error = assertThrows(ApiException.class, () -> uploadService.saveImage(svgFile));

        assertEquals("只支持 jpg/png/gif/webp 格式", error.getMessage());
    }

    private AppProperties appProperties() {
        AppProperties properties = new AppProperties();
        properties.setUploadDir(tempDir.resolve("uploads").toString());
        return properties;
    }

    private Path uploadPath(String uploadUrl) {
        return tempDir.resolve("uploads").resolve(uploadUrl.substring("/uploads/".length()));
    }

    private byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
