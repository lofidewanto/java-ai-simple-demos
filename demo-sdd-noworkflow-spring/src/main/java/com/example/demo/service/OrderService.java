package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Order createOrder(Order order) {
        order.setStatus(OrderStatus.SUBMITTED);
        order.setNotes(timestamp() + " — Order submitted by customer");
        return orderRepository.save(order);
    }

    public Order transitionOrder(Long id, String action) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        OrderStatus current = order.getStatus();
        OrderStatus next;

        switch (current) {
            case SUBMITTED:
                if ("CHECK_INVENTORY".equals(action)) {
                    next = OrderStatus.CHECKING_INVENTORY;
                } else {
                    throw new IllegalStateException(
                        "Invalid action '" + action + "' for status " + current);
                }
                break;

            case CHECKING_INVENTORY:
                if ("MARK_AVAILABLE".equals(action)) {
                    next = OrderStatus.PAYMENT_COLLECTED;
                } else if ("MARK_UNAVAILABLE".equals(action)) {
                    next = OrderStatus.CUSTOMER_NOTIFIED;
                } else {
                    throw new IllegalStateException(
                        "Invalid action '" + action + "' for status " + current);
                }
                break;

            case PAYMENT_COLLECTED:
                if ("SHIP".equals(action)) {
                    next = OrderStatus.SHIPPED;
                } else {
                    throw new IllegalStateException(
                        "Invalid action '" + action + "' for status " + current);
                }
                break;

            case SHIPPED:
            case CUSTOMER_NOTIFIED:
                throw new IllegalStateException(
                    "Order is in a terminal state: " + current);

            default:
                throw new IllegalStateException("Unknown status: " + current);
        }

        order.setStatus(next);
        appendAuditLine(order, "Status changed to " + next.name());
        return orderRepository.save(order);
    }

    private void appendAuditLine(Order order, String message) {
        String line = timestamp() + " — " + message;
        String existing = order.getNotes();
        order.setNotes((existing == null || existing.isBlank()) ? line : existing + "\n" + line);
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
