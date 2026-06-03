package com.hakku.main.recommendation;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추천 API (PRD §3.5). 인증 회원의 프로필을 바탕으로 점수화·정렬된 추천 상품 목록을 반환한다.
 * 인증 주체(principal)는 JWT subject = 회원 id 문자열.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<RecommendationResponse> recommend(@AuthenticationPrincipal String userId) {
        return recommendationService.recommendFor(Long.valueOf(userId));
    }
}
