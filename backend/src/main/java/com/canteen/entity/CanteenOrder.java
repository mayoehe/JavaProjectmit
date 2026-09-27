package com.canteen.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A student pre-order. Total is snapshotted at order time. */
@Entity
@Table(name = "orders")
public class CanteenOrder {

  public static final String PENDING = "PENDING";
  public static final String READY = "READY";
  public static final String COLLECTED = "COLLECTED";
  public static final String UNCOLLECTED = "UNCOLLECTED";
  public static final String CANCELLED = "CANCELLED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private User student;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  @Column(nullable = false)
  private double total = 0.0;

  @Column(nullable = false)
  private String status = PENDING;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  private Instant updatedAt = Instant.now();

  @PreUpdate
  void touch() { updatedAt = Instant.now(); }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public User getStudent() { return student; }
  public void setStudent(User student) { this.student = student; }
  public List<OrderItem> getItems() { return items; }
  public void setItems(List<OrderItem> items) { this.items = items; }
  public double getTotal() { return total; }
  public void setTotal(double total) { this.total = total; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
