package com.hakku.main.auth;

import com.hakku.main.user.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API (JWT-only): 회원가입 / 로그인 / 토큰 재발급 / 로그아웃.
 *
 * <p>액세스 토큰(15분)은 응답 본문으로, 리프레시 토큰(24시간)은 httpOnly 쿠키로 내려간다.
 * 액세스 토큰 만료 시 프론트는 {@code /refresh} 로 쿠키를 보내 새 액세스 토큰을 받는다(무중단 세션).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;

    public AuthController(AuthService authService, RefreshTokenCookie refreshTokenCookie) {
        this.authService = authService;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        Long userId = authService.signup(
                request.email(), request.password(), request.nickname(), request.role());
        return new SignupResponse(userId);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthTokens tokens = authService.login(request.email(), request.password());
        ResponseCookie cookie = refreshTokenCookie.create(tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(tokens.accessToken(), "Bearer"));
    }

    /** 리프레시 쿠키로 새 액세스 토큰 재발급. 쿠키가 없거나 무효면 401(서비스에서 예외). */
    @PostMapping("/refresh")
    public TokenResponse refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        String accessToken = authService.refreshAccessToken(refreshToken);
        return new TokenResponse(accessToken, "Bearer");
    }

    /** 리프레시 쿠키를 즉시 만료시킨다(서버측 stateless 라 토큰 무효화는 쿠키 제거로 한정). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cleared = refreshTokenCookie.clear();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .build();
    }

    public record SignupRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String nickname,
            @NotNull Role role) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record SignupResponse(Long userId) {
    }

    public record TokenResponse(String accessToken, String tokenType) {
    }
}
