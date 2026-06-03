package com.hakku.main.community.repository;

import com.hakku.main.community.domain.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 최신 게시글이 먼저 오도록 id 내림차순으로 조회한다. */
    List<Post> findAllByOrderByIdDesc();
}
