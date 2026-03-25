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
            summary = "Deposit funds into a wallet",
            description = "Adds the specified amount to the wallet balance atomically "
                    + "and records a DEPOSIT transaction. Amount must be greater than zero.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Deposit successful — returns the updated wallet",
                            content = @Content(schema = @Schema(implementation = Wallet.class))),
                    @ApiResponse(responseCode = "400", description = "Amount is null, zero, or negative"),
                    @ApiResponse(responseCode = "404", description = "No wallet exists with the supplied walletId")
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
            summary = "Withdraw funds from a wallet",
            description = "Subtracts the specified amount from the wallet balance atomically "
                    + "and records a WITHDRAWAL transaction. "
                    + "The balance will never go negative — insufficient funds returns 400.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Withdrawal successful — returns the updated wallet",
                            content = @Content(schema = @Schema(implementation = Wallet.class))),
                    @ApiResponse(responseCode = "400",
                            description = "Amount is null, zero, negative, or exceeds the available balance"),
                    @ApiResponse(responseCode = "404", description = "No wallet exists with the supplied walletId")
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