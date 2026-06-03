package com.hakku.main.user;

import com.hakku.main.notification.NotificationEvent;
import com.hakku.main.notification.NotificationMessageFormatter;
import com.hakku.main.notification.NotificationProducer;
import com.hakku.main.notification.domain.NotificationType;
import com.hakku.main.personalcolor.domain.PersonalColorType;
import com.hakku.main.user.domain.DiagnosisStatus;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.exception.DiagnosisAlreadyRequestedException;
import com.hakku.main.user.exception.UserNotFoundException;
import com.hakku.main.user.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 프로필 조회/수정 (PRD §3.1).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    public UserService(UserRepository userRepository, NotificationProducer notificationProducer) {
        this.userRepository = userRepository;
        this.notificationProducer = notificationProducer;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(findOrThrow(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, String nickname,
                                             String profileImageUrl, Set<String> preferredStyles,
                                             Set<String> preferredColors, Boolean onboardingCompleted) {
        User user = findOrThrow(userId);
        user.updateProfile(nickname, profileImageUrl, preferredStyles, preferredColors);
        if (Boolean.TRUE.equals(onboardingCompleted)) {
            user.completeOnboarding();
        }
        return UserProfileResponse.from(user);
    }

    /** 진단 요청을 접수한다 — NONE → PENDING. 이미 요청된 경우 409. */
    @Transactional
    public void requestDiagnosis(Long userId) {
        User user = findOrThrow(userId);
        if (user.getDiagnosisStatus() != DiagnosisStatus.NONE) {
            throw new DiagnosisAlreadyRequestedException(userId);
        }
        user.startDiagnosis();
    }

    /** 진단 요청을 초기화한다 — AI 서버 오류 복구용. NONE으로 되돌린다. */
    @Transactional
    public void resetDiagnosis(Long userId) {
        User user = findOrThrow(userId);
        user.resetDiagnosis();
    }

    /** AI 진단 결과를 반영한다 — COMPLETED로 전환 + 알림 발행. */
    @Transactional
    public UserProfileResponse assignDiagnosis(Long userId,
                                               PersonalColorType personalColor,
                                               String resultImageUrl) {
        User user = findOrThrow(userId);
        user.assignPersonalColor(personalColor);
        user.completeDiagnosis();
        user.assignDiagnosisImage(resultImageUrl);
        notificationProducer.publish(new NotificationEvent(
                NotificationType.DIAGNOSIS_COMPLETE,
                userId,
                null,
                null,
                null,
                null,
                NotificationMessageFormatter.diagnosisComplete(),
                Instant.now().toEpochMilli()));
        return UserProfileResponse.from(user);
    }

    private User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
