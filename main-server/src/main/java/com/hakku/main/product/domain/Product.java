package com.hakku.main.product.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 판매 상품 (PRD §3.3). 판매자는 회원 id 로 참조한다. 색상/스타일 태그는 추천 엔진(§3.5)의 피처로 쓰인다.
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    /** 가격(원). 음수 불가. */
    @Column(nullable = false)
    private long price;

    /** 주 색상 태그(예: "WINTER"). 추천 피처. */
    @Column(name = "key_color")
    private String keyColor;

    /** 보조 색상 태그. 추천 피처. */
    @Column(name = "sub_color")
    private String subColor;

    @Column(name = "category")
    private String category;

    @ElementCollection
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "color")
    private Set<String> colors = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "product_styles", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "style")
    private Set<String> styles = new HashSet<>();

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "purchase_url")
    private String purchaseUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
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

    /** 주어진 회원이 이 상품을 등록한 판매자인지 여부. */
    public boolean isOwnedBy(Long userId) {
        return this.sellerId.equals(userId);
    }
}
