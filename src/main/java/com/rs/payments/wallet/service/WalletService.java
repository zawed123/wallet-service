package com.rs.payments.wallet.service;

import java.math.BigDecimal;
import java.util.UUID;

import com.rs.payments.wallet.dto.BalanceResponse;
import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.model.Wallet;

/**
 * Service interface defining wallet-related operations.
 * Provides methods for wallet creation, balance management,
 * and fund transfers between wallets.
 */
public interface WalletService {

    /**
     * Creates a new wallet for the specified user.
     *
     * @param userId unique identifier of the user
     * @return newly created wallet
     */
    Wallet createWalletForUser(UUID userId);

    /**
     * Adds the given amount to the wallet balance.
     *
     * @param walletId wallet identifier
     * @param amount amount to be credited
     * @return updated wallet details
     */
    Wallet deposit(UUID walletId, BigDecimal amount);

    /**
     * Deducts the given amount from the wallet balance.
     *
     * @param walletId wallet identifier
     * @param amount amount to be debited
     * @return updated wallet details
     */
    Wallet withdraw(UUID walletId, BigDecimal amount);

    /**
     * Transfers funds from one wallet to another.
     *
     * @param request transfer request containing source, destination, and amount
     * @return transfer result with updated balances
     */
    TransferResponse transfer(TransferRequest request);

    /**
     * Retrieves the current balance of the wallet.
     *
     * @param walletId wallet identifier
     * @return balance response
     */
    BalanceResponse getBalance(UUID walletId);
}