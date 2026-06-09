package com.hakku.main.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.follow.repository.FollowRepository;
import com.hakku.main.notification.NotificationEvent;
import com.hakku.main.notification.NotificationProducer;
import com.hakku.main.personalcolor.domain.PersonalColorType;
import com.hakku.main.user.domain.DiagnosisStatus;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.exception.DiagnosisAlreadyRequestedException;
import com.hakku.main.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDiagnosisServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private FollowRepository followRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, followRepository, notificationProducer);
    }

    @Test
    @DisplayName("requestDiagnosis: NONE 상태면 PENDING으로 전환한다")
    void requestDiagnosis_none_setsPending() {
        User user = new User("u@hakku.dev", "유저", "hash", Role.NORMAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.requestDiagnosis(1L);

        assertThat(user.getDiagnosisStatus()).isEqualTo(DiagnosisStatus.PENDING);
    }

    @Test
    @DisplayName("requestDiagnosis: PENDING 상태면 예외를 던진다")
    void requestDiagnosis_pending_throws() {
        User user = new User("u@hakku.dev", "유저", "hash", Role.NORMAL);
        user.startDiagnosis();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.requestDiagnosis(1L))
                .isInstanceOf(DiagnosisAlreadyRequestedException.class);
    }

    @Test
    @DisplayName("requestDiagnosis: COMPLETED 상태면 예외를 던진다")
    void requestDiagnosis_completed_throws() {
        User user = new User("u@hakku.dev", "유저", "hash", Role.NORMAL);
        user.startDiagnosis();
        user.assignPersonalColor(PersonalColorType.LIGHT_SUMMER);
        user.completeDiagnosis();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.requestDiagnosis(1L))
                .isInstanceOf(DiagnosisAlreadyRequestedException.class);
    }

    @Test
    @DisplayName("resetDiagnosis: PENDING 상태를 NONE으로 되돌린다")
    void resetDiagnosis_pendingToNone() {
        User user = new User("u@hakku.dev", "유저", "hash", Role.NORMAL);
        user.startDiagnosis();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.resetDiagnosis(1L);

        assertThat(user.getDiagnosisStatus()).isEqualTo(DiagnosisStatus.NONE);
    }

    @Test
    @DisplayName("assignDiagnosis: COMPLETED로 전환하고 알림을 발행한다")
    void assignDiagnosis_completedAndPublishesNotification() {
        User user = new User("u@hakku.dev", "유저", "hash", Role.NORMAL);
        user.startDiagnosis();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.assignDiagnosis(1L, PersonalColorType.LIGHT_SUMMER, "https://img/result.png");

        assertThat(user.getDiagnosisStatus()).isEqualTo(DiagnosisStatus.COMPLETED);
        assertThat(user.getPersonalColor()).isEqualTo(PersonalColorType.LIGHT_SUMMER);
        verify(notificationProducer).publish(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("assignDiagnosis: 진단 결과가 diagnosisImageUrl에 반영된다")
    void assignDiagnosis_setsResultImageUrl() {
        User user = new User("u@hakku.dev", "유저", "hash", Role.NORMAL);
        user.startDiagnosis();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.assignDiagnosis(1L, PersonalColorType.LIGHT_SUMMER, "https://img/result.png");

        assertThat(user.getDiagnosisImageUrl()).isEqualTo("https://img/result.png");
    }

    @Test
    @DisplayName("getProfile: diagnosisStatus가 응답에 포함된다")
    void getProfile_includesDiagnosisStatus() {
        User user = new User("u@hakku.dev", "유저", "hash", Role.NORMAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse profile = userService.getProfile(1L);

        assertThat(profile.diagnosisStatus()).isEqualTo(DiagnosisStatus.NONE);
    }
}
