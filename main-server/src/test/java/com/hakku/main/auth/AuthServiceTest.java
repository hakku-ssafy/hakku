package com.hakku.main.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.auth.exception.EmailAlreadyExistsException;
import com.hakku.main.auth.exception.InvalidCredentialsException;
import com.hakku.main.auth.jwt.JwtTokenProvider;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("회원가입: 비밀번호를 해시해 저장하고 회원 id를 반환한다")
    void signupHashesAndSaves() {
        when(userRepository.existsByEmail("new@hakku.dev")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 7L);
            return u;
        });

        Long id = authService.signup("new@hakku.dev", "secret123", "신규", Role.NORMAL);

        assertThat(id).isEqualTo(7L);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123"); // 평문 저장 금지
        assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("회원가입: 중복 이메일이면 예외 + 저장하지 않음")
    void signupRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("dup@hakku.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup("dup@hakku.dev", "x", "닉", Role.NORMAL))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인: 비밀번호 일치 시 회원 id와 role로 JWT 발급")
    void loginIssuesToken() {
        var user = new User("user@hakku.dev", "유저", passwordEncoder.encode("pw"), Role.SELLER);
        ReflectionTestUtils.setField(user, "id", 42L);
        when(userRepository.findByEmail("user@hakku.dev")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken("42", Role.SELLER)).thenReturn("jwt-token");

        String token = authService.login("user@hakku.dev", "pw");

        assertThat(token).isEqualTo("jwt-token");
        verify(jwtTokenProvider).createAccessToken("42", Role.SELLER);
    }

    @Test
    @DisplayName("로그인: 비밀번호 불일치 시 예외")
    void loginRejectsWrongPassword() {
        var user = new User("user@hakku.dev", "유저", passwordEncoder.encode("pw"), Role.NORMAL);
        when(userRepository.findByEmail("user@hakku.dev")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("user@hakku.dev", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtTokenProvider, never()).createAccessToken(any(), any());
    }

    @Test
    @DisplayName("로그인: 없는 이메일이면 예외")
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@hakku.dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@hakku.dev", "pw"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
