package com.hakku.main.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.exception.UserNotFoundException;
import com.hakku.main.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    @DisplayName("getProfile: 존재하는 회원이면 프로필을 반환한다")
    void getProfile_existing_returnsProfile() {
        User user = new User("me@hakku.dev", "지호", "hash", Role.NORMAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse profile = userService.getProfile(1L);

        assertThat(profile.email()).isEqualTo("me@hakku.dev");
        assertThat(profile.nickname()).isEqualTo("지호");
        assertThat(profile.role()).isEqualTo(Role.NORMAL);
    }

    @Test
    @DisplayName("getProfile: 회원이 없으면 UserNotFoundException")
    void getProfile_missing_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("updateProfile: 닉네임/이미지/선호스타일을 갱신해 반환한다")
    void updateProfile_updatesFields() {
        User user = new User("me@hakku.dev", "지호", "hash", Role.NORMAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse updated = userService.updateProfile(
                1L, "지호2", "https://img/p.png", Set.of("캐주얼", "미니멀"));

        assertThat(updated.nickname()).isEqualTo("지호2");
        assertThat(updated.profileImageUrl()).isEqualTo("https://img/p.png");
        assertThat(updated.preferredStyles()).containsExactlyInAnyOrder("캐주얼", "미니멀");
    }

    @Test
    @DisplayName("updateProfile: 회원이 없으면 UserNotFoundException")
    void updateProfile_missing_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.updateProfile(99L, "x", null, Set.of()))
                .isInstanceOf(UserNotFoundException.class);
    }
}
