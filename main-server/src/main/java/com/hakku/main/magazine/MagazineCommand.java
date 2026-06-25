package com.hakku.main.magazine;

/** 매거진 발행/수정 입력. */
public record MagazineCommand(
        String kicker,
        String title,
        String subtitle,
        String content,
        String coverImageUrl,
        int displayOrder,
        boolean published) {
}
