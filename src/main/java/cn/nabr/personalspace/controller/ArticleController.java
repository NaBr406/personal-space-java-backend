package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.dto.ArticleRequest;
import cn.nabr.personalspace.security.AuthHelper;
import cn.nabr.personalspace.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文章 / 博客 / 杂谈接口。
 * 文章创建和更新走 multipart，是为了顺手把封面图一起传上来。
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final AuthHelper authHelper;

    public ArticleController(ArticleService articleService, AuthHelper authHelper) {
        this.articleService = articleService;
        this.authHelper = authHelper;
    }

    @GetMapping
    public Object listArticles(
            @RequestParam String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return articleService.listArticles(category, page, limit);
    }

    @GetMapping("/{id}")
    public Object getArticle(@PathVariable long id) {
        return articleService.getArticle(id);
    }

    @PostMapping("/{id}/view")
    public Object addView(@PathVariable long id) {
        return articleService.addView(id);
    }

    /**
     * 前端用表单方式上传文章正文和封面，这里再手动组装成 ArticleRequest。
     */
    @PostMapping(consumes = "multipart/form-data")
    public Object createArticle(
            @RequestParam String category,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String summary,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            HttpServletRequest request
    ) {
        var user = authHelper.requireSuperAdmin(request);
        ArticleRequest articleRequest = new ArticleRequest();
        articleRequest.setCategory(category);
        articleRequest.setTitle(title);
        articleRequest.setContent(content);
        articleRequest.setSummary(summary);
        return articleService.createArticle(articleRequest, cover, user);
    }

    /**
     * 更新文章时沿用旧前端的表单字段格式，避免另外维护一套 JSON 版本。
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public Object updateArticle(
            @PathVariable long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String summary,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            HttpServletRequest request
    ) {
        authHelper.requireSuperAdmin(request);
        ArticleRequest articleRequest = new ArticleRequest();
        articleRequest.setTitle(title);
        articleRequest.setContent(content);
        articleRequest.setSummary(summary);
        return articleService.updateArticle(id, articleRequest, cover);
    }

    @DeleteMapping("/{id}")
    public Object deleteArticle(@PathVariable long id, HttpServletRequest request) {
        authHelper.requireSuperAdmin(request);
        return articleService.deleteArticle(id);
    }
}
