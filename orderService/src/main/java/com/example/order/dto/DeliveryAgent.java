package com.example.order.dto;

import lombok.Data;

@Data
public class DeliveryAgent {

    private Integer agentId;
    private Integer userId;

    private String fullName;
    private String phone;

    private String vehicleType;
    private String vehicleNumber;

    private Double currentLatitude;
    private Double currentLongitude;

    private Boolean isAvailable;
    private Boolean isVerified;

    private Double avgRating;
    private Integer totalDeliveries;
}