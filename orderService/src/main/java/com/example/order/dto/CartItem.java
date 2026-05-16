package com.example.order.dto;

import lombok.Data;

@Data
public class CartItem {
    private Integer itemId;
    private Integer menuItemId;
    private int quantity;
    private String customization;
}
