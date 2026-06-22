package com.hakku.main.cart;

import com.hakku.main.cart.domain.CartItem;
import com.hakku.main.cart.exception.CartItemNotFoundException;
import com.hakku.main.cart.repository.CartItemRepository;
import com.hakku.main.product.domain.Product;
import com.hakku.main.product.exception.ProductNotFoundException;
import com.hakku.main.product.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장바구니 (PRD §3.3). 회원 본인의 장바구니만 다룬다.
 * 같은 상품을 다시 담으면 수량을 더하고, 항목 변경/삭제는 본인 항목만 가능하다.
 */
@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CartItemResponse addItem(Long userId, Long productId, int quantity) {
        Product product = requireProduct(productId);
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .map(existing -> {
                    existing.increaseQuantity(quantity);
                    return cartItemRepository.save(existing);
                })
                .orElseGet(() -> cartItemRepository.save(new CartItem(userId, productId, quantity)));
        return CartItemResponse.from(item, product);
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> list(Long userId) {
        return cartItemRepository.findByUserId(userId).stream()
                .map(item -> CartItemResponse.from(item, requireProduct(item.getProductId())))
                .toList();
    }

    @Transactional
    public CartItemResponse updateQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem item = requireOwnItem(userId, cartItemId);
        item.changeQuantity(quantity);
        cartItemRepository.save(item);
        return CartItemResponse.from(item, requireProduct(item.getProductId()));
    }

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        cartItemRepository.delete(requireOwnItem(userId, cartItemId));
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private CartItem requireOwnItem(Long userId, Long cartItemId) {
        return cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));
    }
}
