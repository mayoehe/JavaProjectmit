package com.canteen.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Minimal JWT helper (HS256). Secret must be >= 32 chars. */
@Component
public class JwtService {

  private final SecretKey key;
  private final long expirationMs;

  public JwtService(
      @Value("${jwt.secret:change_me_to_a_very_long_random_string_min_32_chars}") String secret,
      @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
    if (secret.length() < 32) secret = secret + "0".repeat(32 - secret.length());
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String generate(String studentId, String role) {
    Date now = new Date();
    return Jwts.builder()
        .subject(studentId)
        .claim("role", role)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(key)
        .compact();
  }

  public String extractStudentId(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }

  public boolean isValid(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
