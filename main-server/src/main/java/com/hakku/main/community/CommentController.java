package com.hakku.main.community;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글 댓글 API. 목록(GET)은 공개, 작성/수정/삭제는 인증 필요하며 수정·삭제는 작성자 본인만.
 * 인증 주체(principal)는 JWT subject = 회원 id 문자열.
 */
@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@AuthenticationPrincipal String userId,
                                  @PathVariable Long postId,
                                  @Valid @RequestBody CommentRequest request) {
        return commentService.create(postId, Long.valueOf(userId), request.content());
    }

    @GetMapping("/api/posts/{postId}/comments")
    public List<CommentResponse> list(@PathVariable Long postId) {
        return commentService.listByPost(postId);
    }

    @PutMapping("/api/comments/{id}")
    public CommentResponse update(@AuthenticationPrincipal String userId,
                                  @PathVariable Long id,
                                  @Valid @RequestBody CommentRequest request) {
        return commentService.update(id, Long.valueOf(userId), request.content());
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String userId, @PathVariable Long id) {
        commentService.delete(id, Long.valueOf(userId));
    }

    public record CommentRequest(@NotBlank String content) {
    }
}
