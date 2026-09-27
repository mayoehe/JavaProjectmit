package com.canteen.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One dish/snack on the canteen's menu for the day. */
@Entity
@Table(name = "menu_items")
public class MenuItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private double price;

  @Column(length = 1000)
  private String description;

  private String category = "General"; // e.g. Breakfast, Lunch, Snacks, Beverages

  @Column(nullable = false)
  private boolean available = true;

  private String imageUrl;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public double getPrice() { return price; }
  public void setPrice(double price) { this.price = price; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public boolean isAvailable() { return available; }
  public void setAvailable(boolean available) { this.available = available; }
  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  public Instant getCreatedAt() { return createdAt; }
}
