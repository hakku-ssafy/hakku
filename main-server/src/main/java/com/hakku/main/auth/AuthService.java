package com.hakku.main.auth;

import com.hakku.main.auth.exception.AdminSignupForbiddenException;
import com.hakku.main.auth.exception.EmailAlreadyExistsException;
import com.hakku.main.auth.exception.InvalidCredentialsException;
import com.hakku.main.auth.exception.InvalidRefreshTokenException;
import com.hakku.main.auth.jwt.JwtTokenProvider;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입/로그인 (JWT-only 인증). 비밀번호는 BCrypt 해시로만 저장한다.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public Long signup(String email, String rawPassword, String nickname, Role role) {
        if (role == Role.ADMIN) {
            // 공개 회원가입으로 ADMIN 자가 부여 차단. 관리자는 AdminSeeder/프로비저닝으로만 생성한다.
            throw new AdminSignupForbiddenException();
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = new User(email, nickname, passwordHash, role);
        if (role == Role.SELLER) {
            user.completeOnboarding();
        }
        return userRepository.save(user).getId();
    }

    @Transactional(readOnly = true)
    public AuthTokens login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String subject = String.valueOf(user.getId());
        return new AuthTokens(
                jwtTokenProvider.createAccessToken(subject, user.getRole()),
                jwtTokenProvider.createRefreshToken(subject, user.getRole()));
    }

    /**
     * 리프레시 토큰으로 새 액세스 토큰을 발급한다 (stateless — 저장 조회 없이 토큰 자체만 신뢰).
     * 토큰이 없거나 유효하지 않으면 {@link InvalidRefreshTokenException}(→ 401).
     */
    public String refreshAccessToken(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateRefresh(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }
        String subject = jwtTokenProvider.getSubject(refreshToken);
        Role role = jwtTokenProvider.getRole(refreshToken);
        return jwtTokenProvider.createAccessToken(subject, role);
    }

    /** 로그인 결과 토큰 쌍. accessToken 은 응답 본문, refreshToken 은 httpOnly 쿠키로 내려간다. */
    public record AuthTokens(String accessToken, String refreshToken) {
    }
}
