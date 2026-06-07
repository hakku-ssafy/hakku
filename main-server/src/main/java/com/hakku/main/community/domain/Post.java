package com.hakku.main.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 커뮤니티 게시글 (PRD §3.4). 작성자는 회원 id 로 참조한다(낮은 결합도).
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PostBoard board;

    /** '학생증 자랑' 게시판에서 공유하는 이미지 URL (일반 글은 null). */
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
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

    /** 주어진 회원이 이 게시글의 작성자인지 여부. */
    public boolean isAuthor(Long userId) {
        return this.authorId.equals(userId);
    }
}
