package com.example.order.controller;

import com.example.order.entity.Order;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping("/place/{customerId}")
    public Order placeOrder(@PathVariable Integer customerId, 
                            @RequestParam String paymentMode, 
                            @RequestBody(required = false) java.util.Map<String, String> payload,
                            @RequestHeader("Authorization") String token) {
        String deliveryAddress = (payload != null && payload.containsKey("deliveryAddress")) ? payload.get("deliveryAddress") : null;
        return service.placeOrder(customerId, paymentMode, deliveryAddress, token);
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Integer id) {
        return service.getOrderById(id);
    }

    @GetMapping("/customer/{id}")
    public List<Order> getByCustomer(@PathVariable Integer id) {
        return service.getOrdersByCustomer(id);
    }

    @GetMapping("/restaurant/{id}")
    public List<Order> getByRestaurant(@PathVariable Integer id) {
        return service.getOrdersByRestaurant(id);
    }

    @GetMapping("/active")
    public List<Order> activeOrders() {
        return service.getActiveOrders();
    }

    @GetMapping("/all")
    public List<Order> allOrders() {
        return service.getAllOrders();
    }

    @PutMapping("/status")
    public String updateStatus(@RequestParam Integer id,
                               @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    @PutMapping("/assign")
    public void assignAgent(@RequestParam Integer orderId,
                            @RequestParam Integer agentId) {
        service.assignDeliveryAgent(orderId, agentId);
    }

    @DeleteMapping("/{id}")
    public void cancel(@PathVariable Integer id) {
        service.cancelOrder(id);
    }

    @PostMapping("/reorder/{id}")
    public Order reorder(@PathVariable Integer id) {
        return service.reorderFromHistory(id);
    }

    @GetMapping("/count/{restaurantId}")
    public int count(@PathVariable Integer restaurantId) {
        return service.getOrderCount(restaurantId);
    }
}