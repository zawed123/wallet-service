package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that manages peer-to-peer wallet transfer operations.
 * Transfers are treated as a separate resource since they involve interaction
 * between two wallets (debit and credit), making them conceptually different
 * from single-wallet operations.
 * This design ensures better separation of concerns and aligns with RESTful principles
 * by exposing transfers via a dedicated endpoint: POST /transfers.
 */
@RestController
@RequestMapping("/transfers")
@Tag(name = "Transfers", description = "Peer-to-peer fund transfers between wallets")
public class TransferController {

    private final WalletService walletService;

    public TransferController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(
            summary = "Perform wallet transfer",
            description = "Transfers a specified amount from one wallet to another within a single transaction. "
                    + "The source wallet is debited and the destination wallet is credited accordingly. "
                    + "Transaction entries are created for both sides (debit and credit) to maintain history. "
                    + "In case of any failure during processing, the entire operation is rolled back to avoid partial updates.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Transfer completed successfully with updated wallet balances",
                            content = @Content(schema = @Schema(implementation = TransferResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid transfer request — could be due to insufficient funds, "
                                    + "invalid amount, or transferring within the same wallet"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Either the source or destination wallet was not found"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request) {
        TransferResponse response = walletService.transfer(request);
        return ResponseEntity.ok(response);
    }
}