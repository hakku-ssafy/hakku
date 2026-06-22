package com.hakku.main.community.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.main.community.domain.Post;
import com.hakku.main.community.domain.PostLike;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostLikeRepository(MyBatis 매퍼) 통합 테스트. 실제 SQL 실행을 검증한다.
 * H2 스키마는 Flyway 마이그레이션(실제 운영 스키마)으로 제공되며 FK 제약을 강제한다.
 * 따라서 자식 행을 넣기 전에 참조 부모 행(users, posts)을 먼저 생성하고 생성된 실제 id 를 사용한다.
 */
@SpringBootTest
@Transactional
class PostLikeRepositoryMapperTest {

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private Long postId;
    private Long userId;

    @BeforeEach
    void setUp() {
        User author = userRepository.save(new User("postlike-author@test.com", "글쓴이", Role.NORMAL));
        User liker = userRepository.save(new User("postlike-user@test.com", "좋아요누른이", Role.NORMAL));
        userId = liker.getId();
        Post post = postRepository.save(new Post(author.getId(), "제목", "본문"));
        postId = post.getId();
    }

    @Test
    @DisplayName("save 후 존재/카운트가 반영된다")
    void save_thenExistsAndCount() {
        postLikeRepository.save(new PostLike(postId, userId));

        assertThat(postLikeRepository.existsByPostIdAndUserId(postId, userId)).isTrue();
        assertThat(postLikeRepository.existsByPostIdAndUserId(postId, 999L)).isFalse();
        assertThat(postLikeRepository.countByPostId(postId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("save 시 createdAt 이 객체에 채워진다")
    void save_assignsCreatedAt() {
        PostLike like = postLikeRepository.save(new PostLike(postId, userId));

        assertThat(like.getCreatedAt()).isNotNull();
        assertThat(like.getId()).isNotNull();
    }

    @Test
    @DisplayName("deleteByPostIdAndUserId 후 존재하지 않는다")
    void delete_thenAbsent() {
        postLikeRepository.save(new PostLike(postId, userId));

        postLikeRepository.deleteByPostIdAndUserId(postId, userId);

        assertThat(postLikeRepository.existsByPostIdAndUserId(postId, userId)).isFalse();
        assertThat(postLikeRepository.countByPostId(postId)).isEqualTo(0L);
    }
}
