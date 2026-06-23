package com.hakku.main.community.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.main.community.domain.Comment;
import com.hakku.main.community.domain.Post;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * CommentRepository(MyBatis 매퍼) 통합 테스트.
 * H2 스키마는 Flyway 마이그레이션(실제 운영 스키마)으로 제공되며 FK 제약을 강제한다.
 * 따라서 댓글을 넣기 전에 참조 부모 행(users, posts)을 먼저 생성하고 생성된 실제 id 를 사용한다.
 */
@SpringBootTest
@Transactional
class CommentRepositoryMapperTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private Long postId;
    private Long authorId;

    @BeforeEach
    void setUp() {
        User author = userRepository.save(new User("comment-author@test.com", "댓글작성자", Role.NORMAL));
        authorId = author.getId();
        Post post = postRepository.save(new Post(authorId, "제목", "본문"));
        postId = post.getId();
    }

    @Test
    @DisplayName("save(신규) 후 단건/카운트 조회가 반영되고 타임스탬프가 채워진다")
    void insert_thenFindAndCount() {
        Comment saved = commentRepository.save(new Comment(postId, authorId, "첫 댓글"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(commentRepository.findById(saved.getId())).isPresent();
        assertThat(commentRepository.countByPostId(postId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("게시글 댓글을 id 오름차순으로 조회한다")
    void findByPostOrderByIdAsc() {
        commentRepository.save(new Comment(postId, authorId, "a"));
        commentRepository.save(new Comment(postId, authorId, "b"));

        List<Comment> list = commentRepository.findByPostIdOrderByIdAsc(postId);

        assertThat(list).extracting(Comment::getContent).containsExactly("a", "b");
    }

    @Test
    @DisplayName("save(갱신) 시 내용과 updatedAt 이 변경된다")
    void update_changesContent() {
        Comment saved = commentRepository.save(new Comment(postId, authorId, "old"));

        saved.update("new");
        commentRepository.save(saved);

        Optional<Comment> reloaded = commentRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getContent()).isEqualTo("new");
    }

    @Test
    @DisplayName("delete 후 존재하지 않는다")
    void delete_thenAbsent() {
        Comment saved = commentRepository.save(new Comment(postId, authorId, "tmp"));

        commentRepository.delete(saved);

        assertThat(commentRepository.findById(saved.getId())).isEmpty();
        assertThat(commentRepository.countByPostId(postId)).isEqualTo(0L);
    }
}
