package cn.nabr.personalspace.service;

import cn.nabr.personalspace.exception.ApiException;
import cn.nabr.personalspace.model.PostView;
import cn.nabr.personalspace.model.UserSummary;
import cn.nabr.personalspace.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UploadService uploadService;

    @InjectMocks
    private PostService postService;

    @Test
    void allowsPostOwnerToDeleteEvenWithoutAdminRole() {
        when(postRepository.findPostById(42L, null)).thenReturn(Optional.of(post(42L, 7L)));

        postService.deletePost(42L, new UserSummary(7L, "owner", "Owner", "/default-avatar.png", "guest"));

        verify(postRepository).deletePostAndRelations(42L);
    }

    @Test
    void rejectsNonOwnerWhoIsNotSuperadmin() {
        when(postRepository.findPostById(42L, null)).thenReturn(Optional.of(post(42L, 7L)));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> postService.deletePost(42L, new UserSummary(9L, "admin", "Admin", "/default-avatar.png", "admin"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("无权删除", exception.getMessage());
        verify(postRepository, never()).deletePostAndRelations(42L);
    }

    private PostView post(long id, long userId) {
        return new PostView(
                id,
                "content",
                null,
                null,
                null,
                null,
                userId,
                0,
                "2026-03-10 00:00:00",
                "Owner",
                "/default-avatar.png",
                0,
                0,
                false
        );
    }
}
