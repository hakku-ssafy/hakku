package com.hakku.main.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 프로필 API. 인증 주체(principal)는 JWT subject = 회원 id 문자열.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal String userId) {
        return userService.getProfile(Long.valueOf(userId));
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(@AuthenticationPrincipal String userId,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(
                Long.valueOf(userId),
                request.nickname(),
                request.profileImageUrl(),
                request.preferredStyles());
    }

    public record UpdateProfileRequest(
            @NotBlank String nickname,
            String profileImageUrl,
            Set<String> preferredStyles) {
    }
}
