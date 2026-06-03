package com.hakku.main.auth;

import com.hakku.main.user.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API (JWT-only): 회원가입 / 로그인.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        Long userId = authService.signup(
                request.email(), request.password(), request.nickname(), request.role());
        return new SignupResponse(userId);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        String accessToken = authService.login(request.email(), request.password());
        return new TokenResponse(accessToken, "Bearer");
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
