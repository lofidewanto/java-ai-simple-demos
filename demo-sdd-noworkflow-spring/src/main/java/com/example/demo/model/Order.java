package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Schema(description = "Order in the order processing workflow")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    @Schema(description = "Full name of the customer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerName;

    @Column(name = "product_name", nullable = false)
    @Schema(description = "Name of the product being ordered", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productName;

    @Column(nullable = false)
    @Schema(description = "Number of units ordered", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Schema(description = "Current workflow state of the order", accessMode = Schema.AccessMode.READ_ONLY)
    private OrderStatus status;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "System-managed audit trail of state transitions", accessMode = Schema.AccessMode.READ_ONLY)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Timestamp when the order was created", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Timestamp when the order was last updated", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    // Required by JPA spec
    public Order() {}

    // Convenience constructor for programmatic creation (DataInitializer, tests)
    public Order(String customerName, String productName, Integer quantity) {
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.SUBMITTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // -- Getters and Setters --

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
