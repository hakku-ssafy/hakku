package com.hakku.main.magazine.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매거진 (어드민 발행).
 *
 * <p>홈 메인 캐러셀의 슬라이드(커버: {@code kicker}/{@code title}/{@code subtitle}/{@code coverImageUrl})와
 * 마크다운 본문({@code content})을 하나로 통합한다. 슬라이드를 누르면 {@code /magazine/:id} 상세에서
 * 마크다운 본문(여러 사진·글, 상품 링크 임베드)을 렌더링한다. 공개 노출은 {@code published=true} 만.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Magazine {

    private Long id;

    /** 작은 라벨 (예: EDITORIAL). */
    private String kicker;

    private String title;

    /** 부제/문구. */
    private String subtitle;

    /** 마크다운 본문 (사진·글, 내부 상품 링크 임베드). */
    private String content;

    /** 커버 이미지(캐러셀 슬라이드 배경). */
    private String coverImageUrl;

    /** 캐러셀 정렬 순서(오름차순). */
    private int displayOrder;

    /** 발행 여부. 공개 목록은 published=true 만 반환한다. */
    private boolean published;

    private Instant createdAt;

    private Instant updatedAt;

    public Magazine(String kicker, String title, String subtitle, String content,
                    String coverImageUrl, int displayOrder, boolean published) {
        this.kicker = kicker;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.coverImageUrl = coverImageUrl;
        this.displayOrder = displayOrder;
        this.published = published;
    }

    /** 매거진 정보를 갱신한다. */
    public void update(String kicker, String title, String subtitle, String content,
                       String coverImageUrl, int displayOrder, boolean published) {
        this.kicker = kicker;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.coverImageUrl = coverImageUrl;
        this.displayOrder = displayOrder;
        this.published = published;
    }

    /** 영속 시각 부여(insert 직전). createdAt/updatedAt 동시 설정. */
    public void assignCreationTime(Instant now) {
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 갱신 시각 부여(update 직전). */
    public void assignUpdateTime(Instant now) {
        this.updatedAt = now;
    }
}
