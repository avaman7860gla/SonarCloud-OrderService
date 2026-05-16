package com.example.order.dto;

import java.util.List;

import lombok.Data;

@Data
public class Cart {
    private Integer cartId;
    private Integer customerId;
    private Integer restaurantId;
    private List<CartItem> items;
}
