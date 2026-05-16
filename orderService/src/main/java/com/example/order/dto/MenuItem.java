package com.example.order.dto;

import lombok.Data;

@Data
public class MenuItem {
    private Integer itemId;
    private String name;
    private double price;
    private Integer restaurantId;
}
