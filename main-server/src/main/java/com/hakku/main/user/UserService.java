package com.hakku.main.user;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.exception.UserNotFoundException;
import com.hakku.main.user.repository.UserRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 프로필 조회/수정 (PRD §3.1).
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(findOrThrow(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, String nickname,
                                             String profileImageUrl, Set<String> preferredStyles) {
        User user = findOrThrow(userId);
        user.updateProfile(nickname, profileImageUrl, preferredStyles);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse assignDiagnosis(Long userId,
                                               PersonalColorType personalColor,
                                               String resultImageUrl) {
        User user = findOrThrow(userId);
        user.assignPersonalColor(personalColor);
        user.updateProfile(user.getNickname(), resultImageUrl, user.getPreferredStyles());
        return UserProfileResponse.from(user);
    }

    private User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
