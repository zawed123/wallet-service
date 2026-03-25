package com.rs.payments.wallet.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request body carrying a monetary amount for deposit or withdrawal")
public class AmountRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(description = "Amount to deposit or withdraw — must be positive", example = "100.00")
    private BigDecimal amount;
}
