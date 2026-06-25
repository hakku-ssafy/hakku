package com.hakku.main.product;

import com.hakku.main.product.domain.Product;
import com.hakku.main.product.exception.ProductAccessDeniedException;
import com.hakku.main.product.exception.ProductNotFoundException;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 CRUD (PRD §3.3). 등록은 판매자(SELLER)/관리자만, 수정/삭제는 등록한 판매자 본인만 가능하다.
 */
@Service
public class ProductService {

    /** 어드민 목록 페이지 크기 상한(과도한 limit 방지). */
    private static final int MAX_ADMIN_PAGE_SIZE = 100;

    /** 탈퇴 등으로 판매자를 찾을 수 없을 때 노출할 닉네임. */
    private static final String UNKNOWN_SELLER = "알 수 없음";

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProductResponse create(Long sellerId, Role role, ProductCommand cmd) {
        if (role != Role.SELLER && role != Role.ADMIN) {
            throw new ProductAccessDeniedException("상품 등록은 판매자만 가능합니다.");
        }
        Product saved = productRepository.save(new Product(
                sellerId, cmd.name(), cmd.description(), cmd.price(), cmd.category(),
                cmd.keyColor(), cmd.subColor(), cmd.colors(), cmd.styles(), cmd.imageUrl(), cmd.purchaseUrl()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long productId) {
        return toResponse(findOrThrow(productId));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return toResponses(productRepository.findAllByOrderByIdDesc());
    }

    /** 어드민 상품 목록 — 커서 페이지네이션(활성/비활성 모두). hasMore 판단 위해 limit+1 을 조회한다. */
    @Transactional(readOnly = true)
    public AdminProductPageResponse listForAdmin(Long cursorId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_ADMIN_PAGE_SIZE);
        List<Product> rows = productRepository.findForAdminCursor(cursorId, safeLimit + 1);
        boolean hasMore = rows.size() > safeLimit;
        List<Product> page = hasMore ? rows.subList(0, safeLimit) : rows;
        Long nextCursor = hasMore && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        return new AdminProductPageResponse(toResponses(page), nextCursor, hasMore);
    }

    /** 어드민 인라인 수정 — 이름·카테고리·태그(styles)·활성화 여부만 변경한다. */
    @Transactional
    public ProductResponse editByAdmin(Long productId, String name, String category,
                                       Set<String> styles, boolean active) {
        Product product = findOrThrow(productId);
        product.editByAdmin(name, category, styles, active);
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long productId, Long requesterId, ProductCommand cmd) {
        Product product = findOrThrow(productId);
        requireOwner(product, requesterId);
        product.update(cmd.name(), cmd.description(), cmd.price(), cmd.category(),
                cmd.keyColor(), cmd.subColor(), cmd.colors(), cmd.styles(), cmd.imageUrl(), cmd.purchaseUrl());
        productRepository.save(product);
        return toResponse(product);
    }

    /** 단건 상품을 판매자 닉네임과 함께 응답으로 변환한다. */
    private ProductResponse toResponse(Product product) {
        return toResponses(List.of(product)).get(0);
    }

    /**
     * 상품 목록을 응답으로 변환하며 판매자 닉네임을 채운다.
     * 판매자 id 들을 한 번에 조회(findAllById)해 N+1 을 피한다.
     */
    private List<ProductResponse> toResponses(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        List<Long> sellerIds = products.stream().map(Product::getSellerId).distinct().toList();
        Map<Long, String> nicknames = userRepository.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
        return products.stream()
                .map(p -> ProductResponse.from(p,
                        nicknames.getOrDefault(p.getSellerId(), UNKNOWN_SELLER)))
                .toList();
    }

    @Transactional
    public void delete(Long productId, Long requesterId) {
        Product product = findOrThrow(productId);
        requireOwner(product, requesterId);
        productRepository.delete(product);
    }

    private Product findOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void requireOwner(Product product, Long requesterId) {
        if (!product.isOwnedBy(requesterId)) {
            throw new ProductAccessDeniedException("상품에 대한 권한이 없습니다: id=" + product.getId());
        }
    }
}
