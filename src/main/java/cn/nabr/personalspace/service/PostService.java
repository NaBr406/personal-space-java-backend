package cn.nabr.personalspace.service;

import cn.nabr.personalspace.dto.CreateCommentRequest;
import cn.nabr.personalspace.dto.CreatePostRequest;
import cn.nabr.personalspace.exception.ApiException;
import cn.nabr.personalspace.model.CommentView;
import cn.nabr.personalspace.model.PostView;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.PostRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态相关业务。
 * 这里统一处理分页、上传、点赞、评论，以及对应的通知联动。
 */
@Service
public class PostService {
    private final PostRepository postRepository;
    private final UploadService uploadService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostService(PostRepository postRepository, UploadService uploadService) {
        this.postRepository = postRepository;
        this.uploadService = uploadService;
    }

    public Map<String, Object> listPosts(Integer currentUserId, int page, int limit, String start, String end) {
        int safePage = Math.max(1, page);
        int safeLimit = Math.min(50, Math.max(1, limit));
        var posts = postRepository.findPosts(currentUserId, safePage, safeLimit, start, end);
        int total = postRepository.countPosts(start, end);
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", safePage);
        pagination.put("limit", safeLimit);
        pagination.put("total", total);
        pagination.put("pages", (int) Math.ceil(total / (double) safeLimit));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("posts", posts);
        body.put("pagination", pagination);
        return body;
    }

    public PostView getPost(long id, Integer currentUserId) {
        return postRepository.findPostById(id, currentUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "动态不存在"));
    }

    public Map<String, Object> addView(long id) {
        int views = postRepository.incrementViews(id);
        return Map.of("views", views);
    }

    @Transactional
    public PostView createPost(CreatePostRequest request, UserSummary user) {
        return createPostInternal(request.getContent(), List.of(), user);
    }

    @Transactional
    public PostView createPostMultipart(String content, List<MultipartFile> images, UserSummary user) {
        return createPostInternal(content, images, user);
    }

    /**
     * JSON 提交和 multipart 提交最后都会汇总到这里，
     * 这样上传、校验、回滚逻辑只维护一份。
     */
    private PostView createPostInternal(String rawContent, List<MultipartFile> images, UserSummary user) {
        String content = rawContent == null ? null : rawContent.trim();
        // 先把图片落盘；如果后面数据库写入失败，再统一回收这些文件。
        List<UploadService.UploadedImage> uploadedImages = uploadService.saveImagesWithThumbnails(images, 9);
        if ((content == null || content.isEmpty()) && uploadedImages.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "内容和图片至少需要一个");
        }

        List<String> imageUrls = uploadedImages.stream().map(UploadService.UploadedImage::imageUrl).collect(Collectors.toList());
        List<String> thumbnailUrls = uploadedImages.stream().map(UploadService.UploadedImage::thumbnailUrl).collect(Collectors.toList());
        // 兼容旧版前端：单图字段保留首图，多图字段再额外存完整数组。
        String image = imageUrls.isEmpty() ? null : imageUrls.get(0);
        String thumbnail = thumbnailUrls.isEmpty() ? null : thumbnailUrls.get(0);
        String imagesJson = imageUrls.isEmpty() ? null : toJson(imageUrls);
        String thumbnailsJson = thumbnailUrls.isEmpty() ? null : toJson(thumbnailUrls);

        boolean postCreated = false;
        try {
            long postId = postRepository.createPost(content, image, thumbnail, imagesJson, thumbnailsJson, user.id());
            postCreated = true;
            return getPost(postId, Math.toIntExact(user.id()));
        } catch (RuntimeException e) {
            if (!postCreated) {
                uploadService.deleteUploadedImages(uploadedImages);
            }
            throw e;
        }
    }

    @Transactional
    public Map<String, Object> deletePost(long postId, UserSummary user) {
        PostView post = getPost(postId, null);
        if (post.userId() != user.id() && !"superadmin".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权删除");
        }
        deletePostFiles(post);
        postRepository.deletePostAndRelations(postId);
        return Map.of("message", "已删除");
    }

    @Transactional
    public Map<String, Object> toggleLike(long postId, UserSummary user) {
        PostRepository.PostOwner post = postRepository.findPostOwner(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "动态不存在"));

        boolean exists = postRepository.hasLike(postId, user.id());
        if (exists) {
            postRepository.deleteLike(postId, user.id());
        } else {
            postRepository.insertLike(postId, user.id());
            if (post.userId() != user.id()) {
                // 给别人点赞才发通知，自己点自己就别刷通知了。
                postRepository.addLikeNotification(post.userId(), user.id(), postId);
            }
        }

        return Map.of(
                "liked", !exists,
                "count", postRepository.countLikes(postId)
        );
    }

    public List<CommentView> listComments(long postId) {
        return postRepository.findComments(postId);
    }

    @Transactional
    public CommentView createComment(long postId, CreateCommentRequest request, UserSummary user) {
        String content = request.getContent() == null ? null : request.getContent().trim();
        if (content == null || content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "评论不能超过 500 字");
        }

        PostRepository.PostOwner post = postRepository.findPostOwner(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "动态不存在"));

        Long parentId = request.getParentId();
        Long replyToUserId = null;
        if (parentId != null) {
            PostRepository.CommentRecord parent = postRepository.findCommentRecordInPost(parentId, postId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "回复的评论不存在"));
            replyToUserId = parent.userId();
        }

        long commentId = postRepository.createComment(postId, user.id(), content, parentId, replyToUserId);
        String snippet = content.length() > 100 ? content.substring(0, 100) : content;

        if (replyToUserId != null && replyToUserId != user.id()) {
            postRepository.addReplyNotification(replyToUserId, user.id(), postId, commentId, snippet);
        }
        // 回复别人评论和评论帖子是两种通知，避免同一条操作给同一个人重复发。
        if (post.userId() != user.id() && (replyToUserId == null || post.userId() != replyToUserId)) {
            postRepository.addCommentNotification(post.userId(), user.id(), postId, commentId, snippet);
        }

        return postRepository.findCommentViewById(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "评论创建失败"));
    }

    @Transactional
    public Map<String, Object> deleteComment(long commentId, UserSummary user) {
        PostRepository.CommentRecord comment = postRepository.findCommentRecord(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "评论不存在"));

        boolean isAdmin = "admin".equals(user.role()) || "superadmin".equals(user.role());
        if (comment.userId() != user.id() && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权删除");
        }

        List<Long> childIds = postRepository.findChildCommentIds(commentId);
        for (Long childId : childIds) {
            postRepository.deleteNotificationsByCommentId(childId);
        }
        postRepository.deleteCommentsByParentId(commentId);
        postRepository.deleteNotificationsByCommentId(commentId);
        postRepository.deleteCommentById(commentId);
        return Map.of("ok", true);
    }

    public Map<String, Object> uploadEditorImage(MultipartFile image, UserSummary user) {
        if (!"superadmin".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "需要超级管理员权限");
        }
        String url = uploadService.saveImage(image);
        return Map.of(
                "msg", "",
                "code", 0,
                "data", Map.of(
                        "errFiles", List.of(),
                        "succMap", Map.of(image.getOriginalFilename(), url)
                )
        );
    }

    /**
     * 删除动态前，先把它引用到的所有上传文件收集出来一起清掉。
     */
    private void deletePostFiles(PostView post) {
        Set<String> files = new LinkedHashSet<>();
        addIfNotBlank(files, post.image());
        addIfNotBlank(files, post.thumbnail());
        files.addAll(parseJsonArray(post.images()));
        files.addAll(parseJsonArray(post.thumbnails()));
        for (String file : files) {
            uploadService.deleteIfUploaded(file);
        }
    }

    /**
     * 旧数据里可能没有多图字段，或者字段内容已经损坏。
     * 这里选择静默兜底，避免删除流程因为脏数据中断。
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private void addIfNotBlank(Set<String> files, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        files.add(value);
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "图片数据保存失败");
        }
    }
}
