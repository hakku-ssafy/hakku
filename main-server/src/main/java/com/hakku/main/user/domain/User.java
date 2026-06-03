package com.hakku.main.user.domain;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 회원 (PRD §3.1). 이메일은 유니크. 퍼스널컬러는 AI 진단 전까지 null.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    /** BCrypt 해시. JWT 로그인용. (소셜/패스워드리스 확장 대비 nullable) */
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** AI 진단 결과. 미진단 시 null. */
    @Enumerated(EnumType.STRING)
    @Column(name = "personal_color")
    private PersonalColorType personalColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_status", nullable = false)
    @ColumnDefault("'NONE'")
    private DiagnosisStatus diagnosisStatus = DiagnosisStatus.NONE;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "diagnosis_image_url")
    private String diagnosisImageUrl;

    @ElementCollection
    @CollectionTable(name = "user_preferred_styles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "style")
    private Set<String> preferredStyles = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "user_preferred_colors", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "color")
    private Set<String> preferredColors = new HashSet<>();

    @Column(name = "onboarding_completed", nullable = false)
    @ColumnDefault("false")
    private boolean onboardingCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
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
}
