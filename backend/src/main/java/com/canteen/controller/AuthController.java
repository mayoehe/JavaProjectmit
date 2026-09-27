package com.canteen.controller;

import com.canteen.dto.*;
import com.canteen.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  /** Student self-registration using college student ID. */
  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
    return auth.register(req);
  }

  /** Login for both students and admins (same endpoint, role in response). */
  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest req) {
    return auth.login(req);
  }
}
