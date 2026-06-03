package com.hakku.main.product;

import com.hakku.main.product.domain.Product;
import com.hakku.main.product.exception.ProductAccessDeniedException;
import com.hakku.main.product.exception.ProductNotFoundException;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.user.domain.Role;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 CRUD (PRD §3.3). 등록은 판매자(SELLER)/관리자만, 수정/삭제는 등록한 판매자 본인만 가능하다.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(Long sellerId, Role role, ProductCommand cmd) {
        if (role != Role.SELLER && role != Role.ADMIN) {
            throw new ProductAccessDeniedException("상품 등록은 판매자만 가능합니다.");
        }
        Product saved = productRepository.save(new Product(
                sellerId, cmd.name(), cmd.description(), cmd.price(), cmd.category(),
                cmd.keyColor(), cmd.subColor(), cmd.colors(), cmd.styles(), cmd.imageUrl(), cmd.purchaseUrl()));
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long productId) {
        return ProductResponse.from(findOrThrow(productId));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return productRepository.findAllByOrderByIdDesc().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse update(Long productId, Long requesterId, ProductCommand cmd) {
        Product product = findOrThrow(productId);
        requireOwner(product, requesterId);
        product.update(cmd.name(), cmd.description(), cmd.price(), cmd.category(),
                cmd.keyColor(), cmd.subColor(), cmd.colors(), cmd.styles(), cmd.imageUrl(), cmd.purchaseUrl());
        return ProductResponse.from(product);
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
