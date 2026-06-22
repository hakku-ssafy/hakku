package com.hakku.main.community.repository;

import com.hakku.main.community.domain.Post;
import com.hakku.main.community.domain.PostBoard;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 게시글 MyBatis 매퍼. SQL 은 {@code mapper/community/PostMapper.xml} 에 정의한다.
 */
@Mapper
public interface PostRepository {

    /** 최신 게시글이 먼저 오도록 id 내림차순으로 조회한다. */
    List<Post> findAllByOrderByIdDesc();

    /** 특정 게시판의 게시글만 최신순(id 내림차순)으로 조회한다. */
    List<Post> findAllByBoardOrderByIdDesc(@Param("board") PostBoard board);

    /** 특정 작성자의 게시글만 최신순(id 내림차순)으로 조회한다(마이페이지/프로필용). */
    List<Post> findAllByAuthorIdOrderByIdDesc(@Param("authorId") Long authorId);

    Post selectById(@Param("id") Long id);

    void insert(Post post);

    void update(Post post);

    void deleteById(@Param("id") Long id);

    /** id 로 단건 조회한다. 서비스가 .orElseThrow() 로 쓰므로 Optional 로 감싼다. */
    default Optional<Post> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    /** 엔티티를 삭제한다(JPA delete(entity) 의미 보존). */
    default void delete(Post post) {
        deleteById(post.getId());
    }

    /** 게시글을 저장한다. id 가 없으면 신규(insert), 있으면 갱신(update). JPA save() 의미 보존. */
    default Post save(Post post) {
        if (post.getId() == null) {
            post.assignCreationTime(Instant.now());
            insert(post);
        } else {
            post.assignUpdateTime(Instant.now());
            update(post);
        }
        return post;
    }
}
