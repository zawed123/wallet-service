package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.dto.BalanceResponse;
import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.exception.InsufficientFundsException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.exception.WalletAlreadyExistsException;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import com.rs.payments.wallet.service.WalletService;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(UserRepository userRepository,
                             WalletRepository walletRepository,
                             TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    // ============================================================
    // WALLET CREATION
    // ============================================================

    @Override
    public Wallet createWalletForUser(UUID userId) {
        User user = fetchUser(userId);
        ensureWalletDoesNotExist(userId);

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);

        return walletRepository.save(wallet);
    }

    // ============================================================
    // DEPOSIT
    // ============================================================

    @Override
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {
        Wallet wallet = fetchWallet(walletId);

        return applyTransaction(
                wallet,
                amount,
                TransactionType.DEPOSIT,
                "Amount credited",
                LocalDateTime.now()
        );
    }

    // ============================================================
    // WITHDRAW
    // ============================================================

    @Override
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount) {
        Wallet wallet = fetchWallet(walletId);

        validateSufficientBalance(wallet, amount);

        return applyTransaction(
                wallet,
                amount.negate(),
                TransactionType.WITHDRAWAL,
                "Amount debited",
                LocalDateTime.now()
        );
    }

    // ============================================================
    // BALANCE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID walletId) {
        Wallet wallet = fetchWallet(walletId);
        return new BalanceResponse(wallet.getId(), wallet.getBalance());
    }

    // ============================================================
    // TRANSFER
    // ============================================================

    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request) {

        UUID sourceId = request.getFromWalletId();
        UUID targetId = request.getToWalletId();
        BigDecimal amount = request.getAmount();

        validateDifferentWallets(sourceId, targetId);

        Wallet sourceWallet = fetchWallet(sourceId);
        Wallet targetWallet = fetchWallet(targetId);

        validateSufficientBalance(sourceWallet, amount);

        LocalDateTime now = LocalDateTime.now();

        sourceWallet = applyTransaction(
                sourceWallet,
                amount.negate(),
                TransactionType.TRANSFER_OUT,
                "Transfer to wallet",
                now
        );

        targetWallet = applyTransaction(
                targetWallet,
                amount,
                TransactionType.TRANSFER_IN,
                "Transfer from wallet",
                now
        );

        return new TransferResponse(
                sourceWallet.getId(),
                targetWallet.getId(),
                amount,
                sourceWallet.getBalance(),
                targetWallet.getBalance(),
                now
        );
    }

    // ============================================================
    // VALIDATIONS
    // ============================================================

    private void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient balance to complete the transaction"
            );
        }
    }

    private void validateDifferentWallets(UUID sourceId, UUID targetId) {
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException(
                    "Source and destination wallets must be different"
            );
        }
    }

    private void ensureWalletDoesNotExist(UUID userId) {
        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException(
                    "A wallet is already associated with this user"
            );
        }
    }

    // ============================================================
    // FETCH HELPERS
    // ============================================================

    private User fetchUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }

    private Wallet fetchWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found")
                );
    }

    // ============================================================
    // CORE TRANSACTION LOGIC
    // ============================================================

    private Wallet applyTransaction(Wallet wallet,
                                    BigDecimal delta,
                                    TransactionType type,
                                    String description,
                                    LocalDateTime timestamp) {

        wallet.setBalance(wallet.getBalance().add(delta));
        wallet = walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(delta.abs());
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setTimestamp(timestamp);

        transactionRepository.save(transaction);

        return wallet;
    }
}