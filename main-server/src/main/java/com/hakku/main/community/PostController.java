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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커뮤니티 게시글 API. 조회(GET)는 공개, 작성/수정/삭제는 인증 필요하며 수정·삭제는 작성자 본인만.
 * 인증 주체(principal)는 JWT subject = 회원 id 문자열.
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@AuthenticationPrincipal String userId,
                               @Valid @RequestBody PostRequest request) {
        return postService.create(Long.valueOf(userId), request.title(), request.content());
    }

    @GetMapping
    public List<PostResponse> list() {
        return postService.list();
    }

    @GetMapping("/{id}")
    public PostResponse get(@PathVariable Long id) {
        return postService.get(id);
    }

    @PutMapping("/{id}")
    public PostResponse update(@AuthenticationPrincipal String userId,
                               @PathVariable Long id,
                               @Valid @RequestBody PostRequest request) {
        return postService.update(id, Long.valueOf(userId), request.title(), request.content());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String userId, @PathVariable Long id) {
        postService.delete(id, Long.valueOf(userId));
    }

    public record PostRequest(
            @NotBlank String title,
            @NotBlank String content) {
    }
}
