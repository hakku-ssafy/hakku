package com.hakku.main.magazine;

import com.hakku.main.magazine.domain.Magazine;
import java.time.Instant;

public record MagazineResponse(
        Long id,
        String kicker,
        String title,
        String subtitle,
        String content,
        String coverImageUrl,
        int displayOrder,
        boolean published,
        Instant createdAt,
        Instant updatedAt) {

    public static MagazineResponse from(Magazine magazine) {
        return new MagazineResponse(
                magazine.getId(),
                magazine.getKicker(),
                magazine.getTitle(),
                magazine.getSubtitle(),
                magazine.getContent(),
                magazine.getCoverImageUrl(),
                magazine.getDisplayOrder(),
                magazine.isPublished(),
                magazine.getCreatedAt(),
                magazine.getUpdatedAt());
    }
}
