package com.hakku.main.community.repository;

import com.hakku.main.community.domain.Post;
import com.hakku.main.community.domain.PostBoard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 최신 게시글이 먼저 오도록 id 내림차순으로 조회한다. */
    List<Post> findAllByOrderByIdDesc();

    /** 특정 게시판의 게시글만 최신순(id 내림차순)으로 조회한다. */
    List<Post> findAllByBoardOrderByIdDesc(PostBoard board);
}
