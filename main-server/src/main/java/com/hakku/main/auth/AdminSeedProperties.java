package com.hakku.main.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 초기 관리자 시드 설정 ({@code app.admin.*}). 운영 전용 — {@code email} 이 비면 시드를 건너뛴다.
 *
 * <p>비밀번호는 반드시 환경변수({@code APP_ADMIN_PASSWORD})로 주입한다(소스/로그에 평문 금지).
 * Spring JavaBean 바인딩을 위해 가변 필드 + setter 를 둔다(@ConfigurationProperties 요구사항).
 */
@Component
@ConfigurationProperties(prefix = "app.admin")
public class AdminSeedProperties {

    private String email = "";
    private String password = "";
    private String nickname = "관리자";

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
