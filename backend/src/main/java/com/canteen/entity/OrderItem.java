package com.canteen.entity;

import jakarta.persistence.*;

/** One line of an order — price is copied from the menu at order time. */
@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private CanteenOrder order;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  private MenuItem menuItem;

  @Column(nullable = false)
  private int quantity;

  /** Unit price snapshot (Rs). */
  @Column(nullable = false)
  private double price;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public CanteenOrder getOrder() { return order; }
  public void setOrder(CanteenOrder order) { this.order = order; }
  public MenuItem getMenuItem() { return menuItem; }
  public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public double getPrice() { return price; }
  public void setPrice(double price) { this.price = price; }
}
