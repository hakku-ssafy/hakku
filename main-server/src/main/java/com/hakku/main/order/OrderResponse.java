package com.hakku.main.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hakku.main.order.domain.Order;
import com.hakku.main.order.domain.OrderItem;
import java.util.List;

/** 주문 응답 DTO(주문 내역 화면용). 항목 스냅샷·배송지·합계·상태를 포함한다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(
        Long id,
        String status,
        String recipientName,
        String phone,
        String postalCode,
        String address1,
        String address2,
        long totalAmount,
        long createdAt,
        List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getRecipientName(),
                order.getPhone(),
                order.getPostalCode(),
                order.getAddress1(),
                order.getAddress2(),
                order.getTotalAmount(),
                order.getCreatedAt() != null ? order.getCreatedAt().toEpochMilli() : 0L,
                items);
    }

    public record OrderItemResponse(
            Long productId,
            String productName,
            long price,
            int quantity,
            long lineTotal) {

        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getLineTotal());
        }
    }
}
