package com.hakku.main.curation.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메인 캐러셀/프로모션 큐레이션 카드 (어드민 관리).
 *
 * <p>{@code linkUrl} 이 있으면 카드를 누를 때 그 링크로 이동하고, 없으면 카드 자체 본문
 * ({@code body})을 보여주는 큐레이션 매거진 상세로 이동한다(이동 분기는 프론트가 처리).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurationCard {

    private Long id;

    /** 작은 라벨 (예: NEW ARRIVAL). */
    private String kicker;

    private String title;

    /** 부제/문구. */
    private String subtitle;

    /** 매거진 상세 본문 (linkUrl 미설정 시 노출). */
    private String body;

    private String imageUrl;

    /** 외부/내부 이동 링크. null 이면 매거진 상세(/magazine/:id)로 이동. */
    private String linkUrl;

    /** 캐러셀 정렬 순서(오름차순). */
    private int displayOrder;

    /** 노출 여부. 공개 목록은 active=true 만 반환한다. */
    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    public CurationCard(String kicker, String title, String subtitle, String body,
                        String imageUrl, String linkUrl, int displayOrder, boolean active) {
        this.kicker = kicker;
        this.title = title;
        this.subtitle = subtitle;
        this.body = body;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    /** 카드 정보를 갱신한다. */
    public void update(String kicker, String title, String subtitle, String body,
                       String imageUrl, String linkUrl, int displayOrder, boolean active) {
        this.kicker = kicker;
        this.title = title;
        this.subtitle = subtitle;
        this.body = body;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.displayOrder = displayOrder;
        this.active = active;
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
