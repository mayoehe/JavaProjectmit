package com.canteen.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One dish/snack on the canteen's menu for the day. */
@Entity
@Table(name = "menu_items", indexes = {
    @Index(name = "idx_menu_category", columnList = "category"),
    @Index(name = "idx_menu_available", columnList = "available"),
    @Index(name = "idx_menu_date", columnList = "menuDate")
})
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

  /** Date this dish belongs to (daily menus). Null = served generally / today. */
  private java.time.LocalDate menuDate;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
    if (category == null) category = "General";
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

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
  public java.time.LocalDate getMenuDate() { return menuDate; }
  public void setMenuDate(java.time.LocalDate menuDate) { this.menuDate = menuDate; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
