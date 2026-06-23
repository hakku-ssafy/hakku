package com.hakku.main.user.domain;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 (PRD §3.1). 이메일은 유니크. 퍼스널컬러는 AI 진단 전까지 null.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private Long id;

    private String email;

    private String nickname;

    /** BCrypt 해시. JWT 로그인용. (소셜/패스워드리스 확장 대비 nullable) */
    private String passwordHash;

    private Role role;

    /** AI 진단 결과. 미진단 시 null. */
    private PersonalColorType personalColor;

    private DiagnosisStatus diagnosisStatus = DiagnosisStatus.NONE;

    private String profileImageUrl;

    private String diagnosisImageUrl;

    private Set<String> preferredStyles = new HashSet<>();

    private Set<String> preferredColors = new HashSet<>();

    private boolean onboardingCompleted = false;

    private Instant createdAt;

    public User(String email, String nickname, Role role) {
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public User(String email, String nickname, String passwordHash, Role role) {
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /** AI 퍼스널컬러 진단 결과를 부여한다. */
    public void assignPersonalColor(PersonalColorType personalColor) {
        this.personalColor = personalColor;
    }

    /** 진단 요청을 시작한다 — NONE → PENDING. */
    public void startDiagnosis() {
        this.diagnosisStatus = DiagnosisStatus.PENDING;
    }

    /** 진단을 완료 처리한다 — PENDING → COMPLETED. */
    public void completeDiagnosis() {
        this.diagnosisStatus = DiagnosisStatus.COMPLETED;
    }

    /** 진단 상태를 초기화한다 — 실패 복구용. */
    public void resetDiagnosis() {
        this.diagnosisStatus = DiagnosisStatus.NONE;
    }

    /** AI 진단 결과 이미지 URL을 저장한다. */
    public void assignDiagnosisImage(String diagnosisImageUrl) {
        this.diagnosisImageUrl = diagnosisImageUrl;
    }

    /** 프로필(닉네임/이미지/선호 스타일·컬러)을 갱신한다. */
    public void updateProfile(String nickname, String profileImageUrl,
                             Set<String> preferredStyles, Set<String> preferredColors) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.preferredStyles = preferredStyles == null ? new HashSet<>() : new HashSet<>(preferredStyles);
        this.preferredColors = preferredColors == null ? new HashSet<>() : new HashSet<>(preferredColors);
    }

    /** 온보딩(컬러 취향 설정)을 완료 처리한다. */
    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    /** 영속 시각을 부여한다(MyBatis insert 직전 호출). JPA @CreationTimestamp 대체. createdAt 만 존재. */
    public void assignCreationTime(Instant now) {
        this.createdAt = now;
    }
}
