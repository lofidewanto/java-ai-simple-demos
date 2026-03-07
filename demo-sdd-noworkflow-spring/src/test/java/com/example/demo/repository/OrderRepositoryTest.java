package com.example.demo.repository;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class OrderRepositoryTest {

    @Autowired
    OrderRepository orderRepository;

    @Test
    void saveOrder_ShouldPersistWithSubmittedStatus() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setStatus(OrderStatus.SUBMITTED);

        Order saved = orderRepository.save(order);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(OrderStatus.SUBMITTED, saved.getStatus());
    }

    @Test
    void findById_WhenExists_ShouldReturnOrder() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setStatus(OrderStatus.SUBMITTED);
        Order saved = orderRepository.save(order);

        Optional<Order> found = orderRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getCustomerName());
        assertEquals("Headphones", found.get().getProductName());
        assertEquals(2, found.get().getQuantity());
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        Optional<Order> found = orderRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void findAllByOrderByCreatedAtDesc_ShouldReturnNewestFirst() throws InterruptedException {
        Order o1 = new Order("Alice", "Headphones", 2);
        o1.setStatus(OrderStatus.SUBMITTED);
        orderRepository.save(o1);

        Thread.sleep(100); // ensure distinct timestamps

        Order o2 = new Order("Bob", "Keyboard", 1);
        o2.setStatus(OrderStatus.SUBMITTED);
        orderRepository.save(o2);

        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        assertEquals(2, orders.size());
        assertTrue(orders.get(0).getCreatedAt().isAfter(orders.get(1).getCreatedAt())
                || orders.get(0).getCreatedAt().isEqual(orders.get(1).getCreatedAt()));
    }

    @Test
    void updateOrder_ShouldPersistStatusChange() {
        Order order = new Order("Alice", "Headphones", 2);
        order.setStatus(OrderStatus.SUBMITTED);
        Order saved = orderRepository.save(order);

        saved.setStatus(OrderStatus.CHECKING_INVENTORY);
        orderRepository.saveAndFlush(saved);

        Order updated = orderRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OrderStatus.CHECKING_INVENTORY, updated.getStatus());
    }
}
