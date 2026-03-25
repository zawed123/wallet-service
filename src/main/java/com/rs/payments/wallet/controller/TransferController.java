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
 * Handles peer-to-peer fund transfers between wallets.
 * Separated from WalletController because transfers are a distinct
 * resource concept (they involve two wallets, not one) and deserve
 * their own URL namespace: POST /transfers.
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
            summary = "Transfer funds between two wallets",
            description = "Atomically moves the specified amount from the source wallet to the "
                    + "destination wallet. "
                    + "Records a TRANSFER_OUT transaction on the source and a TRANSFER_IN on the "
                    + "destination. "
                    + "If anything fails the entire operation rolls back — no partial state is persisted.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Transfer successful — returns both updated balances",
                            content = @Content(schema = @Schema(implementation = TransferResponse.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Amount is invalid, source balance is insufficient, "
                                    + "or fromWalletId equals toWalletId"),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Source or destination wallet does not exist")
            }
    )
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request) {
        TransferResponse response = walletService.transfer(request);
        return ResponseEntity.ok(response);
    }
}