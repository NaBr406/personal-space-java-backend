package cn.nabr.personalspace.service;

import cn.nabr.personalspace.dto.CreateCommentRequest;
import cn.nabr.personalspace.dto.CreatePostRequest;
import cn.nabr.personalspace.exception.ApiException;
import cn.nabr.personalspace.model.CommentView;
import cn.nabr.personalspace.model.PostView;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
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
        String content = request.getContent() == null ? null : request.getContent().trim();
        if (content == null || content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "内容不能为空");
        }
        long postId = postRepository.createPost(content, user.id());
        return getPost(postId, Math.toIntExact(user.id()));
    }

    @Transactional
    public Map<String, Object> deletePost(long postId) {
        postRepository.findPostOwner(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "动态不存在"));
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
}
