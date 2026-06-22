package com.hakku.main.community.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 커뮤니티 게시글 (PRD §3.4). 작성자는 회원 id 로 참조한다(낮은 결합도).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    private Long id;

    private Long authorId;

    private String title;

    private String content;

    private PostBoard board;

    /** '학생증 자랑' 게시판에서 공유하는 이미지 URL (일반 글은 null). */
    private String imageUrl;

    private Instant createdAt;

    private Instant updatedAt;

    public Post(Long authorId, String title, String content) {
        this(authorId, title, content, PostBoard.GENERAL, null);
    }

    public Post(Long authorId, String title, String content, PostBoard board, String imageUrl) {
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.board = board != null ? board : PostBoard.GENERAL;
        this.imageUrl = imageUrl;
    }

    /** 제목/본문을 갱신한다. */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
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

    /** 주어진 회원이 이 게시글의 작성자인지 여부. */
    public boolean isAuthor(Long userId) {
        return this.authorId.equals(userId);
    }
}
