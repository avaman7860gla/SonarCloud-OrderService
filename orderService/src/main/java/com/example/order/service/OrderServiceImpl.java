package com.example.order.service;

import com.example.order.entity.*;
import com.example.order.repository.OrderRepository;
import com.example.order.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.example.order.dto.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repo;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Order placeOrder(Integer customerId, String paymentMode, String deliveryAddress, String token) {

        // 🔹 1. Get Cart
        String cartUrl = "http://localhost:8084/cart/" + customerId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Cart> response = restTemplate.exchange(
                cartUrl,
                HttpMethod.GET,
                entity,
                Cart.class);

        Cart cart = response.getBody();

        if (cart == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // 🔹 2. Validate Restaurant
        String restUrl = "http://localhost:8082/restaurants/" + cart.getRestaurantId();
        ResponseEntity<Restaurant> restRes = restTemplate.exchange(
                restUrl,
                HttpMethod.GET,
                entity,
                Restaurant.class);

        Restaurant restaurant = restRes.getBody();

        if (restaurant == null || !restaurant.isOpen() || !restaurant.isApproved()) {
            throw new RuntimeException("Restaurant not available");
        }

        // 🔹 3. Build Order
        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        for (CartItem item : cart.getItems()) {

            String menuUrl = "http://localhost:8083/menu/item/" + item.getMenuItemId();

            ResponseEntity<MenuItem> menuRes = restTemplate.exchange(
                    menuUrl,
                    HttpMethod.GET,
                    entity,
                    MenuItem.class);

            MenuItem menu = menuRes.getBody();

            if (menu == null) {
                throw new RuntimeException("Menu item not found: " + item.getMenuItemId());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItemId(menu.getItemId());
            orderItem.setName(menu.getName());
            orderItem.setPrice(menu.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setCustomization(item.getCustomization());

            total += menu.getPrice() * item.getQuantity();
            orderItems.add(orderItem);
        }

        // 🔹 4. Create Order
        Order order = new Order();

        order.setCustomerId(customerId);
        order.setRestaurantId(cart.getRestaurantId());
        order.setItems(orderItems);

        order.setTotalAmount(total);

        double discount = total * 0.1;
        order.setDiscount(discount);
        order.setFinalAmount(total - discount);

        order.setOrderStatus("PLACED");
        order.setModeOfPayment(paymentMode);

        order.setOrderDate(LocalDateTime.now());
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(30));
        order.setDeliveryAddress(
                deliveryAddress != null && !deliveryAddress.isEmpty() ? deliveryAddress : "User Address");

        Order saved = repo.save(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(saved.getOrderId());
        }

        // 🔔 5A. SEND NOTIFICATION VIA RABBITMQ
        try {
            Notification n = new Notification();
            n.setRecipientId(customerId);
            n.setType("ORDER");
            n.setTitle("Order Placed");
            n.setMessage("Your order #" + saved.getOrderId() + " has been placed successfully");
            n.setChannel("APP");
            n.setRelatedId(saved.getOrderId());
            n.setRelatedType("ORDER");

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, n);
            System.out.println("Notification sent to RabbitMQ for order placement");

        } catch (Exception e) {
            System.out.println("RabbitMQ Notification failed: " + e.getMessage());
        }

        // 🔥 5. DELIVERY AUTO ASSIGNMENT (COD ONLY)
        if (paymentMode.equalsIgnoreCase("COD")) {
            try {
                String deliveryUrl = "http://localhost:8087/delivery/nearby?lat=28.6&lon=77.2";

                ResponseEntity<DeliveryAgent[]> deliveryRes = restTemplate.exchange(
                        deliveryUrl,
                        HttpMethod.GET,
                        entity,
                        DeliveryAgent[].class);

                DeliveryAgent[] agents = deliveryRes.getBody();

                if (agents != null && agents.length > 0) {
                    DeliveryAgent agent = agents[0];
                    String assignUrl = "http://localhost:8087/delivery/assign?agentId=" + agent.getAgentId() + "&orderId=" + saved.getOrderId();
                    restTemplate.exchange(assignUrl, HttpMethod.POST, entity, String.class);

                    saved.setDeliveryAgentId(agent.getAgentId());
                    repo.save(saved);

                    // Notification via RabbitMQ
                    Notification n = new Notification();
                    n.setRecipientId(customerId);
                    n.setType("DELIVERY");
                    n.setTitle("Delivery Assigned");
                    n.setMessage("A delivery partner has been assigned to your order.");
                    n.setChannel("APP");
                    n.setRelatedId(saved.getOrderId());
                    n.setRelatedType("ORDER");

                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, n);
                }
            } catch (Exception e) {
                System.out.println("Delivery assignment failed: " + e.getMessage());
            }
        }

        // 🔥 6. PAYMENT SERVICE
        if (!paymentMode.equalsIgnoreCase("COD")) {
            try {
                String paymentUrl = "http://localhost:8086/payment/create-order"
                        + "?orderId=" + saved.getOrderId()
                        + "&customerId=" + customerId
                        + "&amount=" + saved.getFinalAmount();
                
                restTemplate.exchange(paymentUrl, HttpMethod.POST, entity, String.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initiate payment: " + e.getMessage());
            }
        }

        // 🔹 7. CLEAR CART
        restTemplate.delete("http://localhost:8084/cart/clear/" + customerId);

        return saved;
    }

    @Override
    public Order getOrderById(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Override
    public List<Order> getOrdersByCustomer(Integer customerId) {
        return repo.findByCustomerId(customerId);
    }

    @Override
    public List<Order> getOrdersByRestaurant(Integer restaurantId) {
        return repo.findByRestaurantId(restaurantId);
    }

    @Override
    public List<Order> getActiveOrders() {
        return repo.findByOrderStatusIn(Arrays.asList("PLACED", "PREPARING", "READY", "PICKED_UP"));
    }

    @Override
    public List<Order> getAllOrders() {
        return repo.findAll();
    }

    @Override
    public String updateStatus(Integer orderId, String status) {
        Order order = getOrderById(orderId);
        order.setOrderStatus(status);
        repo.save(order);

        // 🔔 STATUS UPDATE NOTIFICATION VIA RABBITMQ
        if (status.equalsIgnoreCase("DELIVERED") || status.equalsIgnoreCase("PREPARING")
                || status.equalsIgnoreCase("READY") || status.equalsIgnoreCase("CANCELLED")
                || status.equalsIgnoreCase("PAID")) {
            try {
                Notification n = new Notification();
                n.setRecipientId(order.getCustomerId());
                n.setType(status.equalsIgnoreCase("DELIVERED") ? "DELIVERY" : "ORDER");
                
                String title = "Order " + status;
                String message = "Your order #" + order.getOrderId() + " is now " + status;
                
                if(status.equalsIgnoreCase("CANCELLED")) {
                    title = "Order Cancelled";
                    message = "We're sorry, your order #" + order.getOrderId() + " has been cancelled by the restaurant.";
                } else if(status.equalsIgnoreCase("PREPARING")) {
                    title = "Order Accepted";
                    message = "Your order #" + order.getOrderId() + " has been accepted and is now being prepared.";
                } else if(status.equalsIgnoreCase("PAID")) {
                    title = "Payment Successful";
                    message = "Your payment for order #" + order.getOrderId() + " was successful.";
                }

                n.setTitle(title);
                n.setMessage(message);
                n.setChannel("APP");
                n.setRelatedId(order.getOrderId());
                n.setRelatedType("ORDER");

                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, n);
                System.out.println("Status Update Notification sent to RabbitMQ: " + status);
            } catch (Exception e) {
                System.out.println("RabbitMQ Status Notification failed: " + e.getMessage());
            }
        }

        return "Updated";
    }

    @Override
    public void assignDeliveryAgent(Integer orderId, Integer agentId) {
        Order order = getOrderById(orderId);
        order.setDeliveryAgentId(agentId);
        repo.save(order);
    }

    @Override
    public void cancelOrder(Integer orderId) {
        repo.deleteById(orderId);
    }

    @Override
    public Order reorderFromHistory(Integer orderId) {
        Order old = getOrderById(orderId);
        old.setOrderId(null);
        old.setOrderStatus("PLACED");
        old.setOrderDate(LocalDateTime.now());
        return repo.save(old);
    }

    @Override
    public int getOrderCount(Integer restaurantId) {
        return repo.countByRestaurantId(restaurantId);
    }
}