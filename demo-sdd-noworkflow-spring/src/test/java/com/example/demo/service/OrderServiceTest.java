package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService orderService;

    @Test
    void getAllOrders_ShouldReturnAllOrders() {
        Order o1 = new Order("Alice", "Headphones", 2);
        Order o2 = new Order("Bob", "Keyboard", 1);
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Arrays.asList(o1, o2));

        List<Order> result = orderService.getAllOrders();

        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getOrderById_WhenExists_ShouldReturnOrder() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.getOrderById(1L);

        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getCustomerName());
    }

    @Test
    void getOrderById_WhenNotExists_ShouldReturnEmpty() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.getOrderById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void createOrder_ShouldSetSubmittedStatusAndSave() {
        Order order = new Order("Alice", "Headphones", 2);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(order);

        assertEquals(OrderStatus.SUBMITTED, result.getStatus());
        assertNotNull(result.getNotes());
        assertTrue(result.getNotes().contains("Order submitted by customer"));
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void transitionOrder_CheckInventory_ShouldSucceed() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setId(1L);
        order.setStatus(OrderStatus.SUBMITTED);
        order.setNotes("initial");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.transitionOrder(1L, "CHECK_INVENTORY");

        assertEquals(OrderStatus.CHECKING_INVENTORY, result.getStatus());
        assertTrue(result.getNotes().contains("CHECKING_INVENTORY"));
    }

    @Test
    void transitionOrder_MarkAvailable_ShouldSucceed() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setId(1L);
        order.setStatus(OrderStatus.CHECKING_INVENTORY);
        order.setNotes("initial");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.transitionOrder(1L, "MARK_AVAILABLE");

        assertEquals(OrderStatus.PAYMENT_COLLECTED, result.getStatus());
        assertTrue(result.getNotes().contains("PAYMENT_COLLECTED"));
    }

    @Test
    void transitionOrder_MarkUnavailable_ShouldSucceed() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setId(1L);
        order.setStatus(OrderStatus.CHECKING_INVENTORY);
        order.setNotes("initial");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.transitionOrder(1L, "MARK_UNAVAILABLE");

        assertEquals(OrderStatus.CUSTOMER_NOTIFIED, result.getStatus());
        assertTrue(result.getNotes().contains("CUSTOMER_NOTIFIED"));
    }

    @Test
    void transitionOrder_Ship_ShouldSucceed() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setId(1L);
        order.setStatus(OrderStatus.PAYMENT_COLLECTED);
        order.setNotes("initial");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.transitionOrder(1L, "SHIP");

        assertEquals(OrderStatus.SHIPPED, result.getStatus());
        assertTrue(result.getNotes().contains("SHIPPED"));
    }

    @Test
    void transitionOrder_InvalidAction_ShouldThrowIllegalStateException() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setId(1L);
        order.setStatus(OrderStatus.SUBMITTED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> orderService.transitionOrder(1L, "SHIP"));
    }

    @Test
    void transitionOrder_TerminalState_ShouldThrowIllegalStateException() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setId(1L);
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> orderService.transitionOrder(1L, "SHIP"));
    }

    @Test
    void transitionOrder_WhenNotFound_ShouldThrowRuntimeException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> orderService.transitionOrder(999L, "CHECK_INVENTORY"));
    }
}
