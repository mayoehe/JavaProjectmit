package com.canteen.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String studentId,
    @NotBlank String name,
    String email,
    @NotBlank String password) {}
