package com.hakku.main.order.repository;

import com.hakku.main.order.domain.Order;
import com.hakku.main.order.domain.OrderItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 주문 MyBatis 매퍼. SQL 은 {@code mapper/order/OrderMapper.xml} 에 정의한다.
 * 저장은 orders 1행 + order_items N행을 함께 기록한다.
 */
@Mapper
public interface OrderRepository {

    void insertOrder(Order order);

    void insertItem(OrderItem item);

    void updateStatus(Order order);

    List<Order> selectByUserId(@Param("userId") Long userId);

    Order selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Order selectById(@Param("id") Long id);

    default Optional<Order> findByIdAndUserId(Long id, Long userId) {
        return Optional.ofNullable(selectByIdAndUserId(id, userId));
    }

    default Optional<Order> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default List<Order> findByUserId(Long userId) {
        return selectByUserId(userId);
    }

    /** id 가 없으면 신규(주문+항목 insert), 있으면 상태 갱신. JPA save() 의미 보존. */
    default Order save(Order order) {
        if (order.getId() == null) {
            order.assignCreationTime(Instant.now());
            insertOrder(order);
            for (OrderItem item : order.getItems()) {
                item.assignOrderId(order.getId());
                insertItem(item);
            }
        } else {
            order.assignUpdateTime(Instant.now());
            updateStatus(order);
        }
        return order;
    }
}
