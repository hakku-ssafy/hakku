package com.hakku.main.product.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 판매 상품 (PRD §3.3). 판매자는 회원 id 로 참조한다. 색상/스타일 태그는 추천 엔진(§3.5)의 피처로 쓰인다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    private Long id;

    private Long sellerId;

    private String name;

    private String description;

    /** 가격(원). 음수 불가. */
    private long price;

    /** 주 색상 태그(예: "WINTER"). 추천 피처. */
    private String keyColor;

    /** 보조 색상 태그. 추천 피처. */
    private String subColor;

    private String category;

    private Set<String> colors = new HashSet<>();

    private Set<String> styles = new HashSet<>();

    private String imageUrl;

    private String purchaseUrl;

    /** 활성화 여부. false 면 공개 목록·검색·추천에서 숨긴다(어드민에서만 노출). 기본 true. */
    private boolean active = true;

    private Instant createdAt;

    private Instant updatedAt;

    public Product(Long sellerId, String name, String description, long price,
                   String category, String keyColor, String subColor,
                   Set<String> colors, Set<String> styles, String imageUrl, String purchaseUrl) {
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.keyColor = keyColor;
        this.subColor = subColor;
        this.colors = colors == null ? new HashSet<>() : new HashSet<>(colors);
        this.styles = styles == null ? new HashSet<>() : new HashSet<>(styles);
        this.imageUrl = imageUrl;
        this.purchaseUrl = purchaseUrl;
        this.active = true;
    }

    /** 상품 정보를 갱신한다. */
    public void update(String name, String description, long price, String category,
                       String keyColor, String subColor, Set<String> colors,
                       Set<String> styles, String imageUrl, String purchaseUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.keyColor = keyColor;
        this.subColor = subColor;
        this.colors = colors == null ? new HashSet<>() : new HashSet<>(colors);
        this.styles = styles == null ? new HashSet<>() : new HashSet<>(styles);
        this.imageUrl = imageUrl;
        this.purchaseUrl = purchaseUrl;
    }

    /** 어드민 인라인 수정: 이름·카테고리·스타일 태그·활성화 여부만 변경(가격·설명 등 나머지는 보존). */
    public void editByAdmin(String name, String category, Set<String> styles, boolean active) {
        this.name = name;
        this.category = category;
        this.styles = styles == null ? new HashSet<>() : new HashSet<>(styles);
        this.active = active;
    }

    /** 주어진 회원이 이 상품을 등록한 판매자인지 여부. */
    public boolean isOwnedBy(Long userId) {
        return this.sellerId.equals(userId);
    }

    /** 영속 시각을 부여한다(MyBatis insert 직전 호출). JPA @CreationTimestamp 대체. createdAt/updatedAt 동시 설정. */
    public void assignCreationTime(Instant now) {
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 갱신 시각을 부여한다(MyBatis update 직전 호출). JPA @UpdateTimestamp 대체. */
    public void assignUpdateTime(Instant now) {
        this.updatedAt = now;
    }
}
