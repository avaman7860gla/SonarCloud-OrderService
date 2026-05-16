package com.example.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;

    private Integer customerId;
    private Integer restaurantId;
    private Integer deliveryAgentId;

    private double totalAmount;
    private double discount;
    private double finalAmount;

    private String modeOfPayment;

    private String orderStatus; // PLACED, CONFIRMED, PREPARING, DELIVERED

    private LocalDateTime orderDate;
    private LocalDateTime estimatedDelivery;

    private String deliveryAddress;
    private String specialInstructions;

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> items;
}