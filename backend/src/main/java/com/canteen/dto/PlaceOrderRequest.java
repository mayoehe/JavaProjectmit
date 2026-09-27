package com.canteen.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PlaceOrderRequest(@NotEmpty List<OrderLineRequest> items) {}
