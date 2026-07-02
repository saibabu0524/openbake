package com.openbake.server.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartValidateRequest {
    @NotEmpty
    @Valid
    private List<CartItemValidateRequest> items;
}
