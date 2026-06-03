package com.hakku.main.recommendation;

import com.hakku.main.product.ProductResponse;
import com.hakku.main.product.domain.Product;
import com.hakku.main.recommendation.domain.RecommendationScore;

/**
 * 추천 응답 DTO. 추천 상품과 함께 총점({@code score})과 점수 구성({@code breakdown})을 노출해
 * "왜 추천되었는가"를 설명 가능하게 한다(§3.5).
 */
public record RecommendationResponse(
        ProductResponse product,
        double score,
        ScoreBreakdown breakdown) {

    /** 추천 점수의 항목별 기여도. */
    public record ScoreBreakdown(
            double personalColor,
            double preferredColor,
            double style,
            double recentAction,
            double popularity,
            double review) {

        static ScoreBreakdown from(RecommendationScore score) {
            return new ScoreBreakdown(
                    score.personalColor(),
                    score.preferredColor(),
                    score.style(),
                    score.recentAction(),
                    score.popularity(),
                    score.review());
        }
    }

    static RecommendationResponse of(Product product, RecommendationScore score) {
        return new RecommendationResponse(
                ProductResponse.from(product),
                score.total(),
                ScoreBreakdown.from(score));
    }
}
