package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Balance inquiry response")
public class BalanceResponse {

    @Schema(description = "ID of the wallet",
            example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID walletId;

    @Schema(description = "Current balance of the wallet", example = "250.00")
    private BigDecimal balance;
}
