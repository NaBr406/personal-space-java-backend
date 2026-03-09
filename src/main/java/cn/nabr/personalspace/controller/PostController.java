package cn.nabr.personalspace.controller;

import cn.nabr.personalspace.dto.CreateCommentRequest;
import cn.nabr.personalspace.dto.CreatePostRequest;
import cn.nabr.personalspace.security.AuthHelper;
import cn.nabr.personalspace.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class PostController {
    private final PostService postService;
    private final AuthHelper authHelper;

    public PostController(PostService postService, AuthHelper authHelper) {
        this.postService = postService;
        this.authHelper = authHelper;
    }

    @GetMapping("/api/posts")
    public Map<String, Object> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            HttpServletRequest request
    ) {
        Integer userId = authHelper.getUser(request).map(user -> (int) user.id()).orElse(null);
        return postService.listPosts(userId, page, limit, start, end);
    }

    @GetMapping("/api/posts/{id}")
    public Object getPost(@PathVariable long id, HttpServletRequest request) {
        Integer userId = authHelper.getUser(request).map(user -> (int) user.id()).orElse(null);
        return postService.getPost(id, userId);
    }

    @PostMapping("/api/posts/{id}/view")
    public Map<String, Object> addView(@PathVariable long id) {
        return postService.addView(id);
    }

    @PostMapping("/api/posts")
    public Object createPost(@RequestBody CreatePostRequest request, HttpServletRequest httpRequest) {
        var user = authHelper.requireAdmin(httpRequest);
        return postService.createPost(request, user);
    }

    @DeleteMapping("/api/posts/{id}")
    public Object deletePost(@PathVariable long id, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        return postService.deletePost(id);
    }

    @PostMapping("/api/posts/{id}/like")
    public Object toggleLike(@PathVariable long id, HttpServletRequest request) {
        var user = authHelper.requireUser(request);
        return postService.toggleLike(id, user);
    }

    @GetMapping("/api/posts/{id}/comments")
    public Object listComments(@PathVariable long id) {
        return postService.listComments(id);
    }

    @PostMapping("/api/posts/{id}/comments")
    public Object createComment(@PathVariable long id, @RequestBody CreateCommentRequest request, HttpServletRequest httpRequest) {
        var user = authHelper.requireUser(httpRequest);
        return postService.createComment(id, request, user);
    }

    @DeleteMapping("/api/comments/{id}")
    public Object deleteComment(@PathVariable long id, HttpServletRequest request) {
        var user = authHelper.requireUser(request);
        return postService.deleteComment(id, user);
    }
}
