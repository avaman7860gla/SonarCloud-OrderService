package com.example.order.dto;

import lombok.Data;

@Data
public class Notification {
    private Integer recipientId;
    private String type;
    private String title;
    private String message;
    private String channel;
    private Integer relatedId;
    private String relatedType;
}