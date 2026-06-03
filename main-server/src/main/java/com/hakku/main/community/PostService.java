package com.hakku.main.community;

import com.hakku.main.community.domain.Post;
import com.hakku.main.community.exception.PostAccessDeniedException;
import com.hakku.main.community.exception.PostNotFoundException;
import com.hakku.main.community.repository.PostRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글 CRUD. 수정/삭제는 작성자 본인만 가능하다 (PRD §3.4).
 */
@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostResponse create(Long authorId, String title, String content) {
        Post saved = postRepository.save(new Post(authorId, title, content));
        return PostResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long postId) {
        return PostResponse.from(findOrThrow(postId));
    }

    @Transactional(readOnly = true)
    public List<PostResponse> list() {
        return postRepository.findAllByOrderByIdDesc().stream()
                .map(PostResponse::from)
                .toList();
    }

    @Transactional
    public PostResponse update(Long postId, Long requesterId, String title, String content) {
        Post post = findOrThrow(postId);
        requireAuthor(post, requesterId);
        post.update(title, content);
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long postId, Long requesterId) {
        Post post = findOrThrow(postId);
        requireAuthor(post, requesterId);
        postRepository.delete(post);
    }

    private Post findOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
    }

    private void requireAuthor(Post post, Long requesterId) {
        if (!post.isAuthor(requesterId)) {
            throw new PostAccessDeniedException(post.getId());
        }
    }
}
