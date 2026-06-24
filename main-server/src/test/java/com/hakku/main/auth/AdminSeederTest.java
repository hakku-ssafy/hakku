package com.hakku.main.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AdminSeedProperties props;

    @BeforeEach
    void setUp() {
        props = new AdminSeedProperties();
        props.setEmail("admin@hakku.dev");
        props.setPassword("admin-secret");
        props.setNickname("관리자");
    }

    private AdminSeeder seeder() {
        return new AdminSeeder(userRepository, passwordEncoder, props);
    }

    @Test
    @DisplayName("email 미설정이면 ADMIN을 시드하지 않는다(운영 전용)")
    void doesNotSeedWhenEmailBlank() {
        props.setEmail("");

        seeder().run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 이메일이 이미 있으면 시드하지 않는다(멱등)")
    void doesNotSeedWhenAdminAlreadyExists() {
        when(userRepository.existsByEmail("admin@hakku.dev")).thenReturn(true);

        seeder().run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("email 설정 + 부재 시 ADMIN을 BCrypt 해시로 시드한다")
    void seedsAdminWhenConfiguredAndAbsent() {
        when(userRepository.existsByEmail("admin@hakku.dev")).thenReturn(false);

        seeder().run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getEmail()).isEqualTo("admin@hakku.dev");
        assertThat(saved.getNickname()).isEqualTo("관리자");
        assertThat(saved.getPasswordHash()).isNotEqualTo("admin-secret"); // 평문 저장 금지
        assertThat(passwordEncoder.matches("admin-secret", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("email은 설정됐지만 password가 비면 fail-fast (절반 설정 방지)")
    void failsFastWhenPasswordBlank() {
        props.setPassword("");

        assertThatThrownBy(() -> seeder().run(null))
                .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }
}
