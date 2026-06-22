package com.hakku.main.community.repository;

import com.hakku.main.community.domain.Comment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 댓글 MyBatis 매퍼. SQL 은 {@code mapper/community/CommentMapper.xml} 에 정의한다.
 */
@Mapper
public interface CommentRepository {

    /** 게시글의 댓글을 작성 순(id 오름차순)으로 조회한다. */
    List<Comment> findByPostIdOrderByIdAsc(@Param("postId") Long postId);

    long countByPostId(@Param("postId") Long postId);

    Comment selectById(@Param("id") Long id);

    void insert(Comment comment);

    void update(Comment comment);

    void deleteById(@Param("id") Long id);

    /** id 로 단건 조회한다. 서비스가 .orElseThrow() 로 쓰므로 Optional 로 감싼다. */
    default Optional<Comment> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    /** 엔티티를 삭제한다(JPA delete(entity) 의미 보존). */
    default void delete(Comment comment) {
        deleteById(comment.getId());
    }

    /** 댓글을 저장한다. id 가 없으면 신규(insert), 있으면 갱신(update). JPA save() 의미 보존. */
    default Comment save(Comment comment) {
        if (comment.getId() == null) {
            comment.assignCreationTime(Instant.now());
            insert(comment);
        } else {
            comment.assignUpdateTime(Instant.now());
            update(comment);
        }
        return comment;
    }
}
