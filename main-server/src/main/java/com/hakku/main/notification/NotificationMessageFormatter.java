package com.hakku.main.notification;

/** 알림 본문을 사용자 친화적인 문장으로 만든다. */
public final class NotificationMessageFormatter {

    private NotificationMessageFormatter() {
    }

    public static String comment(String actorNickname, String postTitlePreview) {
        return actorNickname + "님이 \"" + preview(postTitlePreview) + "\" 글에 댓글을 달았어요.";
    }

    public static String like(String actorNickname, String postTitlePreview) {
        return actorNickname + "님이 \"" + preview(postTitlePreview) + "\" 글에 좋아요를 눌렀어요.";
    }

    public static String diagnosisComplete() {
        return "퍼스널컬러 진단이 완료되었어요. 결과를 확인해보세요!";
    }

    private static String preview(String title) {
        if (title == null || title.isBlank()) {
            return "게시글";
        }
        String trimmed = title.strip();
        if (trimmed.length() <= 20) {
            return trimmed;
        }
        return trimmed.substring(0, 20) + "...";
    }
}
