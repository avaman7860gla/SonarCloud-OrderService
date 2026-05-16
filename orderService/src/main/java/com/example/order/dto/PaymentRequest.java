package com.example.order.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private Integer orderId;
    private Integer customerId;
    private double amount;
}
