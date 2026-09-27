package com.canteen.controller;

import com.canteen.entity.User;
import com.canteen.repository.UserRepository;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Profile (self) + student tabs (admin). */
@RestController
@CrossOrigin
public class UserController {

  private final UserRepository users;

  public UserController(UserRepository users) {
    this.users = users;
  }

  /** Current logged-in user's profile incl. tab balance. */
  @GetMapping("/api/users/me")
  public User me(Principal principal) {
    return users.findByStudentId(principal.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
  }

  /** All students with outstanding tabs — admin only. */
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/api/students/tabs")
  public List<User> tabs() {
    return users.findByTabBalanceGreaterThanOrderByTabBalanceDesc(0);
  }

  /** All students — admin only (for the tabs table). */
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/api/students")
  public List<User> students() {
    return users.findByRoleOrderByStudentIdAsc("STUDENT");
  }

  /**
   * Mark a student's tab as paid (reset to 0) — admin only.
   * Body: {"amount": 120} optional partial payment; omitted = full clear.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/api/students/{studentId}/tab")
  public User settleTab(@PathVariable String studentId, @RequestBody(required = false) Map<String, Number> body) {
    User u = users.findByStudentId(studentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    double pay = body == null || body.get("amount") == null ? u.getTabBalance()
        : body.get("amount").doubleValue();
    u.setTabBalance(Math.max(0, Math.round((u.getTabBalance() - pay) * 100.0) / 100.0));
    return users.save(u);
  }

  @GetMapping("/api/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "canteen-backend");
  }
}
