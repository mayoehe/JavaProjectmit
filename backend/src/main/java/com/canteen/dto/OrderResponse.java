package com.canteen.dto;

import com.canteen.entity.CanteenOrder;
import java.time.Instant;
import java.util.List;

/**
 * Safe JSON view of an order — breaks the JPA bidirectional cycle
 * (order ↔ items) and never exposes password hashes.
 * Build inside a transaction so the lazy student proxy can be read.
 */
public record OrderResponse(
    Long id,
    String status,
    double total,
    Instant createdAt,
    Instant updatedAt,
    StudentSummary student,
    List<ItemLine> items) {

  public record StudentSummary(Long id, String studentId, String name) {}
  public record ItemLine(Long menuItemId, String name, int quantity, double price) {}

  public static OrderResponse from(CanteenOrder o) {
    StudentSummary s = new StudentSummary(
        o.getStudent().getId(), o.getStudent().getStudentId(), o.getStudent().getName());
    List<ItemLine> lines = o.getItems().stream()
        .map(i -> new ItemLine(i.getMenuItem().getId(), i.getMenuItem().getName(),
            i.getQuantity(), i.getPrice()))
        .toList();
    return new OrderResponse(o.getId(), o.getStatus(), o.getTotal(),
        o.getCreatedAt(), o.getUpdatedAt(), s, lines);
  }
}
