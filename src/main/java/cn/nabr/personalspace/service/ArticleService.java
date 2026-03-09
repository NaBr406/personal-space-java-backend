package cn.nabr.personalspace.service;

import cn.nabr.personalspace.dto.ArticleRequest;
import cn.nabr.personalspace.exception.ApiException;
import cn.nabr.personalspace.model.ArticleView;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.ArticleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final UploadService uploadService;

    public ArticleService(ArticleRepository articleRepository, UploadService uploadService) {
        this.articleRepository = articleRepository;
        this.uploadService = uploadService;
    }

    public Map<String, Object> listArticles(String category, int page, int limit) {
        validateCategory(category);
        int safePage = Math.max(1, page);
        int safeLimit = Math.min(50, Math.max(1, limit));
        var articles = articleRepository.findPage(category, safePage, safeLimit);
        int total = articleRepository.countByCategory(category);

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", safePage);
        pagination.put("limit", safeLimit);
        pagination.put("total", total);
        pagination.put("pages", (int) Math.ceil(total / (double) safeLimit));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("articles", articles);
        body.put("pagination", pagination);
        return body;
    }

    public ArticleView getArticle(long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
    }

    public Map<String, Object> addView(long id) {
        return Map.of("views", articleRepository.incrementViews(id));
    }

    @Transactional
    public ArticleView createArticle(ArticleRequest request, MultipartFile cover, UserSummary user) {
        String category = normalizeCategory(request.getCategory());
        String title = normalizeText(request.getTitle());
        String content = normalizeText(request.getContent());
        String summary = normalizeNullable(request.getSummary());
        if (title.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题不能为空");
        }
        if (content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "内容不能为空");
        }
        String coverImage = cover != null && !cover.isEmpty() ? uploadService.saveImage(cover) : null;
        long id = articleRepository.create(category, title, content, summary, coverImage, user.id());
        return getArticle(id);
    }

    @Transactional
    public ArticleView updateArticle(long id, ArticleRequest request, MultipartFile cover) {
        ArticleView existing = getArticle(id);
        String title = normalizeText(request.getTitle());
        String content = normalizeText(request.getContent());
        String summary = normalizeNullable(request.getSummary());
        if (title.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题不能为空");
        }
        if (content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "内容不能为空");
        }

        String coverImage = existing.coverImage();
        if (cover != null && !cover.isEmpty()) {
            coverImage = uploadService.saveImage(cover);
            uploadService.deleteIfUploaded(existing.coverImage());
        }
        articleRepository.update(id, title, content, summary, coverImage);
        return getArticle(id);
    }

    @Transactional
    public Map<String, Object> deleteArticle(long id) {
        ArticleView existing = getArticle(id);
        uploadService.deleteIfUploaded(existing.coverImage());
        articleRepository.delete(id);
        return Map.of("ok", true);
    }

    private void validateCategory(String category) {
        if (category == null || (!"blog".equals(category) && !"chitchat".equals(category))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "category 必须是 blog 或 chitchat");
        }
    }

    private String normalizeCategory(String category) {
        validateCategory(category);
        return category;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
