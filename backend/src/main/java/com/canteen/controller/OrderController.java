package com.canteen.controller;

import com.canteen.dto.OrderResponse;
import com.canteen.dto.PlaceOrderRequest;
import com.canteen.service.OrderService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

  private final OrderService orders;

  public OrderController(OrderService orders) {
    this.orders = orders;
  }

  /** Student places a pre-order from the cart. Payment default: pay on pickup. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse place(Principal principal, @Valid @RequestBody PlaceOrderRequest req) {
    return orders.placeOrder(principal.getName(), req);
  }

  /** Student's own orders (active + history). */
  @GetMapping("/my")
  public List<OrderResponse> mine(Principal principal) {
    return orders.myOrders(principal.getName());
  }

  /** All orders — admin only. */
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public List<OrderResponse> all() {
    return orders.allOrders();
  }

  /**
   * Admin updates status: PENDING -> READY -> COLLECTED, or UNCOLLECTED
   * (adds total to student's tab). Body: {"status":"READY"}
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{id}/status")
  public OrderResponse setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
    return orders.updateStatus(id, body.get("status"));
  }

  /** Dashboard numbers — admin only. */
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/summary")
  public Map<String, Object> summary() {
    return orders.summary();
  }
}
