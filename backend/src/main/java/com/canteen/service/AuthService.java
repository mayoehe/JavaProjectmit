package com.canteen.service;

import com.canteen.dto.*;
import com.canteen.entity.User;
import com.canteen.repository.UserRepository;
import com.canteen.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Registration + login. Students self-register; admin is seeded at startup. */
@Service
public class AuthService {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  public AuthResponse register(RegisterRequest req) {
    String sid = req.studentId().trim();
    if (users.existsByStudentId(sid)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Student ID already registered");
    }
    User u = new User();
    u.setStudentId(sid);
    u.setName(req.name().trim());
    u.setEmail(req.email() == null ? null : req.email().trim());
    u.setPasswordHash(encoder.encode(req.password()));
    u.setRole("STUDENT");
    users.save(u);
    return new AuthResponse(jwt.generate(u.getStudentId(), u.getRole()),
        u.getStudentId(), u.getName(), u.getRole(), u.getTabBalance());
  }

  public AuthResponse login(LoginRequest req) {
    User u = users.findByStudentId(req.studentId().trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!encoder.matches(req.password(), u.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    return new AuthResponse(jwt.generate(u.getStudentId(), u.getRole()),
        u.getStudentId(), u.getName(), u.getRole(), u.getTabBalance());
  }
}
