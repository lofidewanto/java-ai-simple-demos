package com.example.demo.controller.web;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderWebController.class)
class OrderWebControllerTest {

    @Autowired
    MockMvc mockMvc;

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
    void redirectToOrders_ShouldRedirectToOrdersPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));
    }

    @Test
    void listOrders_ShouldShowOrdersList() throws Exception {
        Order o1 = createSampleOrder(1L, "Alice", "Headphones", 2, OrderStatus.SHIPPED);
        Order o2 = createSampleOrder(2L, "Bob", "Keyboard", 1, OrderStatus.SUBMITTED);
        when(orderService.getAllOrders()).thenReturn(Arrays.asList(o1, o2));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    void showNewOrderForm_ShouldShowForm() throws Exception {
        mockMvc.perform(get("/orders/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/form"))
                .andExpect(model().attributeExists("order"));
    }

    @Test
    void createOrder_ShouldRedirectToList() throws Exception {
        Order created = createSampleOrder(4L, "Dave", "Earbuds", 1, OrderStatus.SUBMITTED);
        when(orderService.createOrder(any(Order.class))).thenReturn(created);

        mockMvc.perform(post("/orders")
                        .param("customerName", "Dave")
                        .param("productName", "Earbuds")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));
    }

    @Test
    void showOrderDetail_WhenExists_ShouldShowDetail() throws Exception {
        Order order = createSampleOrder(1L, "Alice", "Headphones", 2, OrderStatus.SHIPPED);
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/detail"))
                .andExpect(model().attributeExists("order"));
    }

    @Test
    void showOrderDetail_WhenNotExists_ShouldRedirectWithError() throws Exception {
        when(orderService.getOrderById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));
    }

    @Test
    void transitionOrder_ShouldRedirectToDetail() throws Exception {
        Order updated = createSampleOrder(1L, "Alice", "Headphones", 2, OrderStatus.CHECKING_INVENTORY);
        when(orderService.transitionOrder(eq(1L), eq("CHECK_INVENTORY"))).thenReturn(updated);

        mockMvc.perform(post("/orders/1/transition")
                        .param("action", "CHECK_INVENTORY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));
    }
}
