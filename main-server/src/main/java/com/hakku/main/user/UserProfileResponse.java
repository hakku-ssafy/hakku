package com.hakku.main.user;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import com.hakku.main.user.domain.DiagnosisStatus;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import java.util.Set;

/**
 * 회원 프로필 응답 DTO. 비밀번호 해시 등 민감 정보는 노출하지 않는다.
 */
public record UserProfileResponse(
        Long id,
        String email,
        String nickname,
        Role role,
        PersonalColorType personalColor,
        String profileImageUrl,
        Set<String> preferredStyles,
        DiagnosisStatus diagnosisStatus) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getPersonalColor(),
                user.getProfileImageUrl(),
                Set.copyOf(user.getPreferredStyles()),
                user.getDiagnosisStatus());
    }
}
