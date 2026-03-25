package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

/**
 * Request body for POST /transfers.
 * Carries the source wallet, destination wallet, and the amount to move.
 */
@Data
@Schema(description = "Request body for a peer-to-peer wallet transfer")
public class TransferRequest {

    @NotNull(message = "Source wallet ID is required")
    @Schema(description = "ID of the wallet to transfer funds from",
            example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID fromWalletId;

    @NotNull(message = "Destination wallet ID is required")
    @Schema(description = "ID of the wallet to transfer funds to",
            example = "b1f8e32-7c9b-46e2-8d1a-4f5a6b7c8d9e")
    private UUID toWalletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(description = "Amount to transfer — must be positive",
            example = "100.00")
    private BigDecimal amount;
}
