package com.hakku.main.order;

import com.hakku.main.order.domain.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 API. 모든 엔드포인트 인증 필요(본인 주문만). 인증 주체는 JWT subject = 회원 id.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@AuthenticationPrincipal String userId,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createFromCart(Long.valueOf(userId), request.toAddress());
    }

    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal String userId) {
        return orderService.listMyOrders(Long.valueOf(userId));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal String userId, @PathVariable Long id) {
        return orderService.get(Long.valueOf(userId), id);
    }

    public record CreateOrderRequest(
            @NotBlank String recipientName,
            @NotBlank String phone,
            @NotBlank String postalCode,
            @NotBlank String address1,
            String address2) {

        public ShippingAddress toAddress() {
            return new ShippingAddress(recipientName, phone, postalCode, address1, address2);
        }
    }
}
