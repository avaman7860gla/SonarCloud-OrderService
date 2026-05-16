package com.example.order.service;

import com.example.order.entity.Order;

import java.util.List;

public interface OrderService {

    Order placeOrder(Integer customerId, String paymentMode, String deliveryAddress, String token);

    Order getOrderById(Integer id);

    List<Order> getOrdersByCustomer(Integer customerId);

    List<Order> getOrdersByRestaurant(Integer restaurantId);

    List<Order> getActiveOrders();
    List<Order> getAllOrders();

    String updateStatus(Integer orderId, String status);

    void assignDeliveryAgent(Integer orderId, Integer agentId);

    void cancelOrder(Integer orderId);

    Order reorderFromHistory(Integer orderId);

    int getOrderCount(Integer restaurantId);
}