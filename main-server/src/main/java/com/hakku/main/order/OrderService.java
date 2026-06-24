package com.hakku.main.order;

import com.hakku.main.cart.domain.CartItem;
import com.hakku.main.cart.repository.CartItemRepository;
import com.hakku.main.order.domain.Order;
import com.hakku.main.order.domain.OrderItem;
import com.hakku.main.order.domain.OrderStatus;
import com.hakku.main.order.domain.ShippingAddress;
import com.hakku.main.order.exception.EmptyCartException;
import com.hakku.main.order.exception.OrderNotFoundException;
import com.hakku.main.order.repository.OrderRepository;
import com.hakku.main.product.domain.Product;
import com.hakku.main.product.exception.ProductNotFoundException;
import com.hakku.main.product.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문. 회원 본인의 장바구니를 스냅샷해 주문을 만들고, 결제 승인 시 PAID 로 전이한다.
 * 결제 자체는 payment-server 가 담당하며 referenceId 로 이 주문 id 를 참조한다.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse createFromCart(Long userId, ShippingAddress address) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }
        List<OrderItem> items = cartItems.stream()
                .map(this::toOrderItem)
                .toList();
        Order order = new Order(userId, address, items);
        orderRepository.save(order);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMyOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return OrderResponse.from(order);
    }

    /**
     * 결제 승인 이벤트 수신 시 호출. 주문을 PAID 로 전이하고 장바구니를 비운다.
     * 이미 종료 상태면 멱등 no-op(이벤트 at-least-once 대비).
     */
    @Transactional
    public void markPaid(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() != OrderStatus.CREATED) {
                return;
            }
            order.markPaid();
            orderRepository.save(order);
            cartItemRepository.deleteByUserId(order.getUserId());
        });
    }

    private OrderItem toOrderItem(CartItem cartItem) {
        Product product = productRepository.findById(cartItem.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(cartItem.getProductId()));
        // productId 의 정본은 장바구니 항목이 가리키는 id 다(상품 스냅샷의 name/price 만 박제).
        return new OrderItem(cartItem.getProductId(), product.getName(), product.getPrice(),
                cartItem.getQuantity());
    }
}
