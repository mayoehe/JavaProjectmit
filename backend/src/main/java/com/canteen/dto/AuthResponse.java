package com.canteen.dto;

public record AuthResponse(String token, String studentId, String name, String role, double tabBalance) {}
