package com.hakku.main.recommendation;

import com.hakku.main.product.domain.Product;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.recommendation.domain.ProductFeatures;
import com.hakku.main.recommendation.domain.RecommendationScore;
import com.hakku.main.recommendation.domain.RecommendationScoreCalculator;
import com.hakku.main.recommendation.domain.UserPreferenceProfile;
import com.hakku.main.review.repository.ReviewRepository;
import com.hakku.main.review.repository.ReviewRepository.ProductRatingAverage;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.exception.UserNotFoundException;
import com.hakku.main.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콘텐츠 기반 추천 목록 생성 (PRD §3.5).
 *
 * <p>회원 프로필(퍼스널컬러·선호 스타일)과 상품 피처(색상·스타일·리뷰 평점)를 모아 점수기
 * ({@link RecommendationScoreCalculator})로 점수를 매기고 총점 내림차순으로 정렬한다.
 *
 * <p>퍼스널컬러 매칭은 <b>계절 단위</b>로 한다: 회원의 16종 진단 타입을 그 계절명(예:
 * {@code LIGHT_SUMMER → "SUMMER"})으로 환원해 상품의 계절 단위 색상 태그(예: {@code "SUMMER"})와
 * 비교한다. 상품은 계절 단위로 태깅되어 있으므로(§3.3) 이 환원이 자연스러운 매칭 단위다.
 */
@Service
public class RecommendationService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final RecommendationScoreCalculator calculator = new RecommendationScoreCalculator();

    public RecommendationService(UserRepository userRepository,
                                 ProductRepository productRepository,
                                 ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    /** 회원에게 추천 상품을 총점 내림차순으로 반환한다. 동점은 입력 순서(최신 상품 우선)를 유지한다. */
    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommendFor(Long userId) {
        UserPreferenceProfile profile = profileOf(findUserOrThrow(userId));
        Map<Long, Double> reviewAverages = reviewAverages();

        return productRepository.findAllByOrderByIdDesc().stream()
                .map(product -> {
                    RecommendationScore score = calculator.score(profile, featuresOf(product, reviewAverages));
                    return RecommendationResponse.of(product, score);
                })
                .sorted(Comparator.comparingDouble(RecommendationResponse::score).reversed())
                .toList();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserPreferenceProfile profileOf(User user) {
        // 미진단 회원은 퍼스널컬러 신호가 없다(null). 행동 이력 추적(recentActionTags)은 추후 페이즈 → 빈 집합.
        String personalColor = user.getPersonalColor() == null
                ? null
                : user.getPersonalColor().season().name();
        return new UserPreferenceProfile(personalColor, user.getPreferredStyles(), Set.of());
    }

    private ProductFeatures featuresOf(Product product, Map<Long, Double> reviewAverages) {
        // popularity(조회/판매량 기반)는 신호 수집 후 도입 예정 → 현재 0.0.
        double reviewScore = reviewAverages.getOrDefault(product.getId(), 0.0);
        return new ProductFeatures(
                product.getKeyColor(),
                product.getSubColor(),
                product.getStyles(),
                0.0,
                reviewScore);
    }

    private Map<Long, Double> reviewAverages() {
        return reviewRepository.findAverageRatingByProduct().stream()
                .collect(Collectors.toMap(
                        ProductRatingAverage::getProductId,
                        ProductRatingAverage::getAverage));
    }
}
