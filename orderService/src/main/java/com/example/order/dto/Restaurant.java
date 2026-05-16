package com.example.order.dto;

import lombok.Data;

@Data
public class Restaurant {
    private Integer restaurantId;
    private boolean isOpen;
    private boolean isApproved;
}
