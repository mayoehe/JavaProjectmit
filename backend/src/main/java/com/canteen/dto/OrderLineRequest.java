package com.canteen.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderLineRequest(@NotNull Long menuItemId, @Min(1) int quantity) {}
