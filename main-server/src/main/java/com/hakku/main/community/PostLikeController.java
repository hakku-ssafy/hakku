package com.hakku.main.community;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글 좋아요 API. 인증 필요. POST 로 토글(누름/취소)하며 현재 상태와 총 좋아요 수를 반환한다.
 * 인증 주체(principal)는 JWT subject = 회원 id 문자열.
 */
@RestController
public class PostLikeController {

    private final PostLikeService postLikeService;

    public PostLikeController(PostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    @PostMapping("/api/posts/{postId}/likes")
    public LikeResult toggle(@AuthenticationPrincipal String userId, @PathVariable Long postId) {
        return postLikeService.toggle(postId, Long.valueOf(userId));
    }
}
