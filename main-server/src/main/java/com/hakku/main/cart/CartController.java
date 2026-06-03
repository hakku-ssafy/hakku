package com.hakku.main.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장바구니 API. 모든 엔드포인트는 인증 필요하며 회원 본인의 장바구니만 다룬다.
 * 인증 주체(principal)는 JWT subject = 회원 id 문자열.
 */
@RestController
@RequestMapping("/api/cart/items")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse add(@AuthenticationPrincipal String userId,
                                @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(Long.valueOf(userId), request.productId(), request.quantity());
    }

    @GetMapping
    public List<CartItemResponse> list(@AuthenticationPrincipal String userId) {
        return cartService.list(Long.valueOf(userId));
    }

    @PutMapping("/{id}")
    public CartItemResponse update(@AuthenticationPrincipal String userId,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateQuantityRequest request) {
        return cartService.updateQuantity(Long.valueOf(userId), id, request.quantity());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal String userId, @PathVariable Long id) {
        cartService.removeItem(Long.valueOf(userId), id);
    }

    public record AddCartItemRequest(
            @NotNull Long productId,
            @Positive int quantity) {
    }

    public record UpdateQuantityRequest(@Positive int quantity) {
    }
}
