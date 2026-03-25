package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body returned by POST /transfers on success.
 * Carries the IDs of both wallets, the transferred amount, the resulting
 * balances of both wallets, and the timestamp of the operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response body after a successful peer-to-peer transfer")
public class TransferResponse {

    @Schema(description = "ID of the wallet funds were transferred from")
    private UUID fromWalletId;

    @Schema(description = "ID of the wallet funds were transferred to")
    private UUID toWalletId;

    @Schema(description = "Amount that was transferred", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Balance of the source wallet after the transfer", example = "400.00")
    private BigDecimal fromWalletBalance;

    @Schema(description = "Balance of the destination wallet after the transfer", example = "600.00")
    private BigDecimal toWalletBalance;

    @Schema(description = "Timestamp when the transfer was executed")
    private LocalDateTime timestamp;
}