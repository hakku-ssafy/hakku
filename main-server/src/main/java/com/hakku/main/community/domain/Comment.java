package com.hakku.main.community.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 댓글 (PRD §3.4). 게시글/작성자는 id 로 참조한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    private Long id;

    private Long postId;

    private Long authorId;

    private String content;

    private Instant createdAt;

    private Instant updatedAt;

    public Comment(Long postId, Long authorId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
    }

    /** 내용을 갱신한다. */
    public void update(String content) {
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

    /** 주어진 회원이 이 댓글의 작성자인지 여부. */
    public boolean isAuthor(Long userId) {
        return this.authorId.equals(userId);
    }
}
