package com.hakku.main.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품 리뷰 API. 목록(GET)은 공개, 작성/수정/삭제는 인증 필요하며 수정·삭제는 작성자 본인만.
 * 인증 주체(principal)는 JWT subject = 회원 id 문자열.
 */
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal String userId,
                                 @PathVariable Long productId,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(productId, Long.valueOf(userId), request.rating(), request.content());
    }

    @GetMapping("/api/products/{productId}/reviews")
    public List<ReviewResponse> list(@PathVariable Long productId) {
        return reviewService.listByProduct(productId);
    }

    @PutMapping("/api/reviews/{id}")
    public ReviewResponse update(@AuthenticationPrincipal String userId,
                                 @PathVariable Long id,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviewService.update(id, Long.valueOf(userId), request.rating(), request.content());
    }

    @DeleteMapping("/api/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String userId, @PathVariable Long id) {
        reviewService.delete(id, Long.valueOf(userId));
    }

    public record ReviewRequest(
            @Min(1) @Max(5) int rating,
            @NotBlank String content) {
    }
}
