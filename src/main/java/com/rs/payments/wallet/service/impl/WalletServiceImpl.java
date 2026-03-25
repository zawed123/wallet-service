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

    public WalletServiceImpl(UserRepository userRepository, WalletRepository walletRepository,TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository=transactionRepository;
    }

    @Override
    public Wallet createWalletForUser(UUID userId) {

        User user = getUserOrThrow(userId);
        validateUserHasNoWallet(userId);

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        return walletRepository.save(wallet); // Cascade persists wallet
    }

    @Override
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {
        Wallet wallet = findWalletOrThrow(walletId);
        return updateBalance(wallet, amount, TransactionType.DEPOSIT, "Deposit",
                LocalDateTime.now());
    }

    @Override
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount) {
        Wallet wallet = findWalletOrThrow(walletId);
        assertSufficientFunds(wallet, amount);
        return updateBalance(wallet, amount.negate(), TransactionType.WITHDRAWAL, "Withdrawal",
                LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID walletId) {
        // Read-only transaction — no save() calls must happen here.
        // Returns only the wallet ID and balance, not the full entity,
        // so callers do not receive more data than they need.
        Wallet wallet = findWalletOrThrow(walletId);
        return new BalanceResponse(wallet.getId(), wallet.getBalance());
    }

    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        UUID fromId       = request.getFromWalletId();
        UUID toId         = request.getToWalletId();
        BigDecimal amount = request.getAmount();

        assertNotSameWallet(fromId, toId);

        Wallet fromWallet = findWalletOrThrow(fromId);
        Wallet toWallet   = findWalletOrThrow(toId);

        assertSufficientFunds(fromWallet, amount);

        LocalDateTime now = LocalDateTime.now();

        fromWallet = updateBalance(fromWallet, amount.negate(),
                TransactionType.TRANSFER_OUT, "Transfer to wallet " + toId, now);

        toWallet = updateBalance(toWallet, amount,
                TransactionType.TRANSFER_IN, "Transfer from wallet " + fromId, now);

        return new TransferResponse(
                fromWallet.getId(), toWallet.getId(),
                amount,
                fromWallet.getBalance(), toWallet.getBalance(),
                now);
    }

    private void assertSufficientFunds(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: balance is " + wallet.getBalance()
                            + ", requested " + amount);
        }
    }

    private void assertNotSameWallet(UUID fromId, UUID toId) {
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException(
                    "Cannot transfer to the same wallet: " + fromId);
        }
    }


    private Wallet updateBalance(Wallet wallet, BigDecimal delta,
                                 TransactionType type, String description,
                                 LocalDateTime timestamp) {
        wallet.setBalance(wallet.getBalance().add(delta));
        wallet = walletRepository.save(wallet);
        transactionRepository.save(
                buildTransaction(wallet, delta.abs(), type, description, timestamp));
        return wallet;
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private void validateUserHasNoWallet(UUID userId) {
        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException("Wallet already exists for user: " + userId);
        }
    }

    private Wallet findWalletOrThrow(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found with id: " + walletId));
    }

    private Transaction buildTransaction(Wallet wallet, BigDecimal amount,
                                         TransactionType type, String description,
                                         LocalDateTime timestamp) {
        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setDescription(description);
        tx.setTimestamp(timestamp);
        return tx;
    }
}