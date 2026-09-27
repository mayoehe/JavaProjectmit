package com.canteen.config;

import com.canteen.security.JwtAuthFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthFilter jwtFilter;

  public SecurityConfig(JwtAuthFilter jwtFilter) {
    this.jwtFilter = jwtFilter;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  /**
   * CORS origins: comma-separated list, e.g. http://localhost:5173,http://localhost:3000.
   * Defaults allow local Vite dev + docker frontend.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors-origins:http://localhost:5173,http://localhost:3000}") String origins) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(List.of(origins.split(",")));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    c.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http,
      @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(corsCustomizer -> corsCustomizer.configurationSource(cors))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**", "/api/health", "/h2-console/**",
                "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/menu/**").authenticated()
            .anyRequest().authenticated())
        // allow H2 console frames in local dev
        .headers(h -> h.frameOptions(f -> f.sameOrigin()))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
