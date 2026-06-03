package com.hakku.main.community.repository;

import com.hakku.main.community.domain.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 게시글의 댓글을 작성 순(id 오름차순)으로 조회한다. */
    List<Comment> findByPostIdOrderByIdAsc(Long postId);

    long countByPostId(Long postId);
}
