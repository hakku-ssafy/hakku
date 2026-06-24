package com.hakku.main.curation;

/** 큐레이션 카드 생성/수정 입력. */
public record CurationCardCommand(
        String kicker,
        String title,
        String subtitle,
        String body,
        String imageUrl,
        String linkUrl,
        int displayOrder,
        boolean active) {
}
