package com.hakku.main.user;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import com.hakku.main.user.domain.DiagnosisStatus;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import java.util.Set;

public record UserProfileResponse(
        Long id,
        String email,
        String nickname,
        Role role,
        PersonalColorType personalColor,
        String profileImageUrl,
        String diagnosisImageUrl,
        Set<String> preferredStyles,
        Set<String> preferredColors,
        DiagnosisStatus diagnosisStatus,
        boolean onboardingCompleted) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getPersonalColor(),
                user.getProfileImageUrl(),
                user.getDiagnosisImageUrl(),
                Set.copyOf(user.getPreferredStyles()),
                Set.copyOf(user.getPreferredColors()),
                user.getDiagnosisStatus(),
                user.isOnboardingCompleted());
    }
}
