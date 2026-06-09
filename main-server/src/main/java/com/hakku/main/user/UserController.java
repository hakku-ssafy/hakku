package com.hakku.main.user;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @GetMapping("/{id}")
    public PublicProfileResponse publicProfile(@AuthenticationPrincipal String userId,
                                               @PathVariable Long id) {
        Long viewerId = userId != null ? Long.valueOf(userId) : null;
        return userService.getPublicProfile(id, viewerId);
    }

    @PatchMapping("/me/preferred-colors")
    public UserProfileResponse updatePreferredColors(@AuthenticationPrincipal String userId,
                                                     @RequestBody PreferredColorsRequest request) {
        return userService.updatePreferredColors(Long.valueOf(userId), request.preferredColors());
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(@AuthenticationPrincipal String userId,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(
                Long.valueOf(userId),
                request.nickname(),
                request.profileImageUrl(),
                request.preferredStyles(), request.preferredColors(),
                request.onboardingCompleted());
    }

    @PostMapping("/me/diagnosis-request")
    @ResponseStatus(HttpStatus.OK)
    public void requestDiagnosis(@AuthenticationPrincipal String userId) {
        userService.requestDiagnosis(Long.valueOf(userId));
    }

    @DeleteMapping("/me/diagnosis-request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetDiagnosis(@AuthenticationPrincipal String userId) {
        userService.resetDiagnosis(Long.valueOf(userId));
    }

    @PatchMapping("/me/personal-color")
    public UserProfileResponse updateDiagnosis(@AuthenticationPrincipal String userId,
                                               @Valid @RequestBody DiagnosisRequest request) {
        return userService.assignDiagnosis(
                Long.valueOf(userId),
                request.personalColor(),
                request.resultImageUrl());
    }

    public record UpdateProfileRequest(
            @NotBlank String nickname,
            String profileImageUrl,
            Set<String> preferredStyles,
            Set<String> preferredColors,
            Boolean onboardingCompleted) {
    }

    public record DiagnosisRequest(
            @NotNull PersonalColorType personalColor,
            String resultImageUrl) {
    }

    public record PreferredColorsRequest(
            Set<String> preferredColors) {
    }
}
