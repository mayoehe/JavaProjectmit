package com.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * A user is either a STUDENT (identified by college studentId / roll number)
 * or an ADMIN (canteen staff). studentId is the login identifier for both.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "studentId"))
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Unique college roll / enrollment number, or e.g. "admin" for staff. */
  @Column(nullable = false, unique = true)
  private String studentId;

  @Column(nullable = false)
  private String name;

  private String email;

  /** BCrypt hash — never the raw password, never serialized to JSON. */
  @JsonIgnore
  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String role = "STUDENT"; // STUDENT | ADMIN

  /** Outstanding tab balance (Rs). Increased when an order goes UNCOLLECTED. */
  @Column(nullable = false)
  private double tabBalance = 0.0;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  // --- getters / setters ---
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getStudentId() { return studentId; }
  public void setStudentId(String studentId) { this.studentId = studentId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public double getTabBalance() { return tabBalance; }
  public void setTabBalance(double tabBalance) { this.tabBalance = tabBalance; }
  public Instant getCreatedAt() { return createdAt; }
}
