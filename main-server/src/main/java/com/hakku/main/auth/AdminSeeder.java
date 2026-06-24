package com.hakku.main.auth;

import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 시작 시 초기 관리자(ADMIN)를 시드한다. 공개 회원가입은 ADMIN 을 거부하므로(자가 권한 상승 차단),
 * 운영 환경의 최초 관리자는 이 시더로만 생성된다.
 *
 * <ul>
 *   <li>{@code app.admin.email} 이 비면 시드를 건너뛴다(운영 전용 — 대부분의 테스트/로컬에서 no-op).</li>
 *   <li>이메일은 설정됐는데 비밀번호가 비면 fail-fast(절반만 설정된 관리자 방지).</li>
 *   <li>같은 이메일이 이미 있으면 건너뛴다(재시작 멱등).</li>
 * </ul>
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties properties;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AdminSeedProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.getEmail();
        if (email == null || email.isBlank()) {
            return; // 미설정 → 시드 안 함
        }
        String password = properties.getPassword();
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "app.admin.email 이 설정됐지만 app.admin.password(APP_ADMIN_PASSWORD)가 비어 있습니다.");
        }
        if (userRepository.existsByEmail(email)) {
            return; // 멱등 — 이미 존재
        }
        User admin = new User(email, properties.getNickname(),
                passwordEncoder.encode(password), Role.ADMIN);
        userRepository.save(admin);
        log.info("초기 관리자 계정을 시드했습니다: {}", email); // 비밀번호는 로깅하지 않는다
    }
}
