package com.hakku.main.curation;

import com.hakku.main.curation.domain.CurationCard;
import java.time.Instant;

public record CurationCardResponse(
        Long id,
        String kicker,
        String title,
        String subtitle,
        String body,
        String imageUrl,
        String linkUrl,
        int displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static CurationCardResponse from(CurationCard card) {
        return new CurationCardResponse(
                card.getId(),
                card.getKicker(),
                card.getTitle(),
                card.getSubtitle(),
                card.getBody(),
                card.getImageUrl(),
                card.getLinkUrl(),
                card.getDisplayOrder(),
                card.isActive(),
                card.getCreatedAt(),
                card.getUpdatedAt());
    }
}
