package com.canteen.service;

import com.canteen.dto.*;
import com.canteen.entity.*;
import com.canteen.repository.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Core ordering flow: menu -> order -> pickup/tab.
 * Marking an order UNCOLLECTED adds its total to the student's tab (once).
 */
@Service
public class OrderService {

  private final OrderRepository orders;
  private final MenuItemRepository menu;
  private final UserRepository users;

  public OrderService(OrderRepository orders, MenuItemRepository menu, UserRepository users) {
    this.orders = orders;
    this.menu = menu;
    this.users = users;
  }

  @Transactional
  public OrderResponse placeOrder(String studentId, PlaceOrderRequest req) {
    User student = users.findByStudentId(studentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown student"));
    if (req.items() == null || req.items().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
    }
    CanteenOrder order = new CanteenOrder();
    order.setStudent(student);
    double total = 0;
    for (OrderLineRequest line : req.items()) {
      MenuItem mi = menu.findById(line.menuItemId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Unknown menu item: " + line.menuItemId()));
      if (!mi.isAvailable()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mi.getName() + " is not available");
      }
      if (line.quantity() < 1) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity");
      }
      OrderItem oi = new OrderItem();
      oi.setOrder(order);
      oi.setMenuItem(mi);
      oi.setQuantity(line.quantity());
      oi.setPrice(mi.getPrice()); // snapshot price
      order.getItems().add(oi);
      total += mi.getPrice() * line.quantity();
    }
    order.setTotal(Math.round(total * 100.0) / 100.0);
    order.setStatus(CanteenOrder.PENDING);
    return OrderResponse.from(orders.save(order));
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> myOrders(String studentId) {
    User student = users.findByStudentId(studentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown student"));
    return orders.findByStudentOrderByCreatedAtDesc(student).stream().map(OrderResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> allOrders() {
    return orders.findAllByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
  }

  /**
   * Admin status transition. Side effect: first transition to UNCOLLECTED
   * adds order total to the student's tab balance.
   */
  @Transactional
  public OrderResponse updateStatus(Long orderId, String newStatus) {
    CanteenOrder order = orders.findById(orderId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    Set<String> allowed = Set.of(CanteenOrder.PENDING, CanteenOrder.READY,
        CanteenOrder.COLLECTED, CanteenOrder.UNCOLLECTED, CanteenOrder.CANCELLED);
    if (!allowed.contains(newStatus)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + newStatus);
    }
    boolean wasUncollected = CanteenOrder.UNCOLLECTED.equals(order.getStatus());
    order.setStatus(newStatus);
    if (CanteenOrder.UNCOLLECTED.equals(newStatus) && !wasUncollected) {
      User s = order.getStudent();
      s.setTabBalance(Math.round((s.getTabBalance() + order.getTotal()) * 100.0) / 100.0);
      users.save(s);
    }
    return OrderResponse.from(orders.save(order));
  }

  /** Today's summary for the admin dashboard. */
  @Transactional(readOnly = true)
  public Map<String, Object> summary() {
    List<CanteenOrder> all = orders.findAllByOrderByCreatedAtDesc();
    long pending = all.stream().filter(o -> CanteenOrder.PENDING.equals(o.getStatus())).count();
    long ready = all.stream().filter(o -> CanteenOrder.READY.equals(o.getStatus())).count();
    long collected = all.stream().filter(o -> CanteenOrder.COLLECTED.equals(o.getStatus())).count();
    long uncollected = all.stream().filter(o -> CanteenOrder.UNCOLLECTED.equals(o.getStatus())).count();
    double collectedRevenue = all.stream()
        .filter(o -> CanteenOrder.COLLECTED.equals(o.getStatus()))
        .mapToDouble(CanteenOrder::getTotal).sum();
    double tabOutstanding = all.stream()
        .filter(o -> CanteenOrder.UNCOLLECTED.equals(o.getStatus()))
        .mapToDouble(CanteenOrder::getTotal).sum();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("totalOrders", all.size());
    m.put("pending", pending);
    m.put("ready", ready);
    m.put("collected", collected);
    m.put("uncollected", uncollected);
    m.put("collectedRevenue", Math.round(collectedRevenue * 100.0) / 100.0);
    m.put("tabOutstanding", Math.round(tabOutstanding * 100.0) / 100.0);
    return m;
  }
}
