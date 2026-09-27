package com.canteen.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reads `Authorization: Bearer <jwt>` and populates the SecurityContext. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwt;
  private final CanteenUserDetailsService userDetails;

  public JwtAuthFilter(JwtService jwt, CanteenUserDetailsService userDetails) {
    this.jwt = jwt;
    this.userDetails = userDetails;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String header = req.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      if (jwt.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
        try {
          UserDetails ud = userDetails.loadUserByUsername(jwt.extractStudentId(token));
          SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities()));
        } catch (Exception ignored) {
          // invalid user -> stay unauthenticated, downstream returns 401/403
        }
      }
    }
    chain.doFilter(req, res);
  }
}
