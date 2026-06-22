package com.hakku.main.community.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.main.community.domain.Post;
import com.hakku.main.community.domain.PostBoard;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostRepository(MyBatis 매퍼) 통합 테스트.
 * H2 스키마는 Flyway 마이그레이션(실제 운영 스키마)으로 제공되며 FK 제약을 강제한다.
 * 따라서 게시글을 넣기 전에 참조 부모 행(users)을 먼저 생성하고 생성된 실제 id 를 작성자로 사용한다.
 */
@SpringBootTest
@Transactional
class PostRepositoryMapperTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private Long authorId;

    @BeforeEach
    void setUp() {
        authorId = userRepository.save(new User("post-author@test.com", "글쓴이", Role.NORMAL)).getId();
    }

    @Test
    @DisplayName("save(신규) 후 단건 조회되고 enum board/타임스탬프가 채워진다")
    void insert_thenFind() {
        Post saved = postRepository.save(
                new Post(authorId, "제목", "본문", PostBoard.STUDENT_ID, "http://img"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<Post> found = postRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getBoard()).isEqualTo(PostBoard.STUDENT_ID);
        assertThat(found.get().getImageUrl()).isEqualTo("http://img");
    }

    @Test
    @DisplayName("전체/게시판/작성자별 목록을 id 내림차순으로 조회한다")
    void findLists() {
        Post general = postRepository.save(new Post(authorId, "g", "본문"));
        Post studentId = postRepository.save(
                new Post(authorId, "s", "본문", PostBoard.STUDENT_ID, null));

        // 다른 테스트가 남긴 행이 있을 수 있으므로(@SpringBootTest API 테스트 비롤백) 상대 순서만 검증한다.
        assertThat(postRepository.findAllByOrderByIdDesc())
                .extracting(Post::getId)
                .containsSubsequence(studentId.getId(), general.getId());
        assertThat(postRepository.findAllByBoardOrderByIdDesc(PostBoard.STUDENT_ID))
                .extracting(Post::getId)
                .contains(studentId.getId());
        assertThat(postRepository.findAllByAuthorIdOrderByIdDesc(authorId))
                .extracting(Post::getId)
                .containsSubsequence(studentId.getId(), general.getId());
    }

    @Test
    @DisplayName("save(갱신) 시 제목/본문이 변경된다")
    void update_changesTitleAndContent() {
        Post saved = postRepository.save(new Post(authorId, "old", "oldBody"));

        saved.update("new", "newBody");
        postRepository.save(saved);

        Optional<Post> reloaded = postRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getTitle()).isEqualTo("new");
        assertThat(reloaded.get().getContent()).isEqualTo("newBody");
    }

    @Test
    @DisplayName("delete 후 존재하지 않는다")
    void delete_thenAbsent() {
        Post saved = postRepository.save(new Post(authorId, "tmp", "본문"));

        postRepository.delete(saved);

        assertThat(postRepository.findById(saved.getId())).isEmpty();
    }
}
