package com.example.order.service;

import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderServiceImpl service;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setOrderId(1);
        order.setCustomerId(10);
        order.setRestaurantId(5);
        order.setOrderStatus("PLACED");
        order.setTotalAmount(25.50);
    }

    @Test
    void testGetOrderById() {
        when(repository.findById(1)).thenReturn(Optional.of(order));
        Order found = service.getOrderById(1);
        assertNotNull(found);
        assertEquals(1, found.getOrderId());
    }

    @Test
    void testUpdateOrderStatus() {
        when(repository.findById(1)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenReturn(order);
        
        String result = service.updateStatus(1, "DELIVERED");
        assertEquals("Updated", result);
        assertEquals("DELIVERED", order.getOrderStatus());
        verify(repository, times(1)).save(order);
    }

    @Test
    void testGetOrdersByCustomer() {
        when(repository.findByCustomerId(10)).thenReturn(Arrays.asList(order));
        List<Order> list = service.getOrdersByCustomer(10);
        assertEquals(1, list.size());
        assertEquals(10, list.get(0).getCustomerId());
    }

    @Test
    void testGetOrdersByRestaurant() {
        when(repository.findByRestaurantId(5)).thenReturn(Arrays.asList(order));
        List<Order> list = service.getOrdersByRestaurant(5);
        assertEquals(1, list.size());
        assertEquals(5, list.get(0).getRestaurantId());
    }
}
