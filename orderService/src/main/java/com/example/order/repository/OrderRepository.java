package com.example.order.repository;

import com.example.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByCustomerId(Integer customerId);

    List<Order> findByRestaurantId(Integer restaurantId);

    List<Order> findByOrderStatus(String status);
    List<Order> findByOrderStatusIn(List<String> statuses);

    List<Order> findByDeliveryAgentId(Integer agentId);

    List<Order> findByOrderId(Integer orderId);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    int countByRestaurantId(Integer restaurantId);
}