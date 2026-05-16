package com.example.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderItemId;

    private Integer orderId;

    private Integer menuItemId;

    private String name;
    private double price;

    private int quantity;

    private String customization;
}