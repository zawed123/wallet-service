package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.AmountRequest;
import com.rs.payments.wallet.dto.BalanceResponse;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallet Management", description = "APIs for managing user wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(
            summary = "Create a new wallet for a user",
            description = "Creates a new wallet for the specified user ID with a zero balance.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Wallet created successfully"),
                    @ApiResponse(responseCode = "400", description = "User already has wallet"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PostMapping
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWalletForUser(request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @Operation(
            summary = "Add money to wallet",
            description = "Credits the given amount to the specified wallet and updates its balance within a single transaction. "
                    + "A corresponding deposit entry is recorded for tracking purposes. "
                    + "The amount must be a positive value.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Amount successfully added and updated wallet details returned",
                            content = @Content(schema = @Schema(implementation = Wallet.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid amount — value must be greater than zero"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Wallet not found for the given walletId"
                    )
            }
    )
    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<Wallet> deposit(
            @PathVariable UUID walletId,
            @Valid @RequestBody AmountRequest request) {
        Wallet updated = walletService.deposit(walletId, request.getAmount());
        return ResponseEntity.ok(updated);
    }


    @Operation(
            summary = "Withdraw money from wallet",
            description = "Debits the specified amount from the wallet and updates the balance within a single transaction. "
                    + "A withdrawal entry is also created to maintain transaction history. "
                    + "The operation ensures that the wallet balance does not drop below zero.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Withdrawal processed successfully with updated wallet details",
                            content = @Content(schema = @Schema(implementation = Wallet.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request — amount must be positive and within the available balance"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Wallet not found for the provided walletId"
                    )
            }
    )
    @PostMapping("/{walletId}/withdraw")
    public ResponseEntity<Wallet> withdraw(
            @PathVariable UUID walletId,
            @Valid @RequestBody AmountRequest request) {
        Wallet updated = walletService.withdraw(walletId, request.getAmount());
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Get wallet balance",
            description = "Returns the current balance of the specified wallet. "
                    + "This is a read-only operation — it does not create any transaction records.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Balance retrieved successfully",
                            content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No wallet exists with the supplied walletId")
            }
    )
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID walletId) {
        BalanceResponse response = walletService.getBalance(walletId);
        return ResponseEntity.ok(response);
    }

}