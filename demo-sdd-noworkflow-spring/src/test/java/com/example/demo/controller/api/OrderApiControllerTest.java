package com.example.demo.controller.api;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderApiController.class)
class OrderApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    OrderService orderService;

    private Order createSampleOrder(Long id, String customer, String product, int qty, OrderStatus status) {
        Order order = new Order(customer, product, qty);
        order.setId(id);
        order.setStatus(status);
        order.setNotes("test notes");
        order.setCreatedAt(LocalDateTime.of(2026, 3, 7, 10, 0));
        order.setUpdatedAt(LocalDateTime.of(2026, 3, 7, 10, 0));
        return order;
    }

    @Test
    void getAllOrders_ShouldReturnListOfOrders() throws Exception {
        Order o1 = createSampleOrder(1L, "Alice", "Headphones", 2, OrderStatus.SHIPPED);
        Order o2 = createSampleOrder(2L, "Bob", "Keyboard", 1, OrderStatus.SUBMITTED);
        when(orderService.getAllOrders()).thenReturn(Arrays.asList(o1, o2));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerName").value("Alice"));
    }

    @Test
    void getOrderById_WhenExists_ShouldReturnOrder() throws Exception {
        Order order = createSampleOrder(1L, "Alice", "Headphones", 2, OrderStatus.SHIPPED);
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void getOrderById_WhenNotExists_ShouldReturn404() throws Exception {
        when(orderService.getOrderById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_ShouldReturnCreatedOrder() throws Exception {
        Order created = createSampleOrder(4L, "Dave", "Earbuds", 1, OrderStatus.SUBMITTED);
        when(orderService.createOrder(any(Order.class))).thenReturn(created);

        String requestBody = """
                {"customerName":"Dave","productName":"Earbuds","quantity":1}
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.customerName").value("Dave"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void transitionOrder_ValidAction_ShouldReturnUpdatedOrder() throws Exception {
        Order updated = createSampleOrder(1L, "Alice", "Headphones", 2, OrderStatus.CHECKING_INVENTORY);
        when(orderService.transitionOrder(eq(1L), eq("CHECK_INVENTORY"))).thenReturn(updated);

        String requestBody = """
                {"action":"CHECK_INVENTORY"}
                """;

        mockMvc.perform(post("/api/orders/1/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKING_INVENTORY"));
    }

    @Test
    void transitionOrder_InvalidAction_ShouldReturn409() throws Exception {
        when(orderService.transitionOrder(eq(1L), eq("SHIP")))
                .thenThrow(new IllegalStateException("Invalid action"));

        String requestBody = """
                {"action":"SHIP"}
                """;

        mockMvc.perform(post("/api/orders/1/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void transitionOrder_OrderNotFound_ShouldReturn404() throws Exception {
        when(orderService.transitionOrder(eq(999L), eq("CHECK_INVENTORY")))
                .thenThrow(new RuntimeException("Order not found"));

        String requestBody = """
                {"action":"CHECK_INVENTORY"}
                """;

        mockMvc.perform(post("/api/orders/999/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void transitionOrder_TerminalState_ShouldReturn409() throws Exception {
        when(orderService.transitionOrder(eq(1L), eq("SHIP")))
                .thenThrow(new IllegalStateException("Order is in a terminal state"));

        String requestBody = """
                {"action":"SHIP"}
                """;

        mockMvc.perform(post("/api/orders/1/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }
}
