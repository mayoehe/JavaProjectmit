package com.canteen.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Returns clear JSON error messages so the Admin UI can show validation feedback. */
@RestControllerAdvice
public class ApiErrorHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handle(ResponseStatusException ex) {
    String msg = ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason();
    return ResponseEntity.status(ex.getStatusCode())
        .body(Map.of("message", msg, "status", ex.getStatusCode().value()));
  }
}
