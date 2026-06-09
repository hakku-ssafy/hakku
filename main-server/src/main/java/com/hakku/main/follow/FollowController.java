package com.hakku.main.follow;

import com.hakku.main.user.UserSummaryResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팔로우 API (요구사항 §1). 인증 주체(principal)는 JWT subject = 회원 id 문자열.
 */
@RestController
@RequestMapping("/api/users")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{id}/follow")
    public FollowToggleResponse toggle(@AuthenticationPrincipal String userId, @PathVariable Long id) {
        return followService.toggle(Long.valueOf(userId), id);
    }

    @GetMapping("/{id}/followers")
    public List<UserSummaryResponse> followers(@PathVariable Long id) {
        return followService.listFollowers(id);
    }

    @GetMapping("/{id}/following")
    public List<UserSummaryResponse> following(@PathVariable Long id) {
        return followService.listFollowing(id);
    }
}
