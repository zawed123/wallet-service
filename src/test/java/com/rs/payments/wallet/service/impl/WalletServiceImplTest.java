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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private WalletServiceImpl walletService;

    private UUID userId;
    private UUID walletId;
    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId   = UUID.randomUUID();
        walletId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("alice");
        user.setEmail("alice@example.com");

        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("200.00"));
    }

    // =========================================================================
    // createWalletForUser — carried over
    // =========================================================================

    @Nested
    @DisplayName("createWalletForUser")
    class CreateWalletForUser {

        @Test
        @DisplayName("should create wallet with zero balance for existing user")
        void shouldCreateWalletWithZeroBalance() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.existsByUserId(userId)).thenReturn(false);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
                Wallet w = inv.getArgument(0);
                w.setId(UUID.randomUUID());
                return w;
            });

            Wallet result = walletService.createWalletForUser(userId);

            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.createWalletForUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw WalletAlreadyExistsException when user already has wallet")
        void shouldThrowWhenWalletAlreadyExists() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.existsByUserId(userId)).thenReturn(true);

            assertThatThrownBy(() -> walletService.createWalletForUser(userId))
                    .isInstanceOf(WalletAlreadyExistsException.class);
        }
    }

    // =========================================================================
    // deposit — carried over
    // =========================================================================

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("should increase balance by deposit amount")
        void shouldIncreaseBalance() {
            wallet.setBalance(new BigDecimal("100.00"));
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.deposit(walletId, new BigDecimal("50.00"));

            assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should create a DEPOSIT transaction record")
        void shouldCreateDepositTransactionRecord() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            walletService.deposit(walletId, new BigDecimal("75.00"));

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.deposit(walletId, BigDecimal.TEN))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // withdraw — carried over
    // =========================================================================

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("should decrease balance by withdrawal amount")
        void shouldDecreaseBalance() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.withdraw(walletId, new BigDecimal("80.00"));

            assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("120.00"));
        }

        @Test
        @DisplayName("should throw InsufficientFundsException when balance too low")
        void shouldThrowOnInsufficientFunds() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> walletService.withdraw(walletId, new BigDecimal("200.01")))
                    .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("should not persist anything when funds are insufficient")
        void shouldNotPersistOnInsufficientFunds() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> walletService.withdraw(walletId, new BigDecimal("999.00")))
                    .isInstanceOf(InsufficientFundsException.class);

            verify(walletRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }
    }

    // =========================================================================
    // transfer — carried over
    // =========================================================================

    @Nested
    @DisplayName("transfer")
    class Transfer {

        private UUID fromWalletId;
        private UUID toWalletId;
        private Wallet fromWallet;
        private Wallet toWallet;
        private TransferRequest request;

        @BeforeEach
        void setUp() {
            fromWalletId = UUID.randomUUID();
            toWalletId   = UUID.randomUUID();

            User fromUser = new User();
            fromUser.setId(UUID.randomUUID());
            User toUser = new User();
            toUser.setId(UUID.randomUUID());

            fromWallet = new Wallet();
            fromWallet.setId(fromWalletId);
            fromWallet.setUser(fromUser);
            fromWallet.setBalance(new BigDecimal("500.00"));

            toWallet = new Wallet();
            toWallet.setId(toWalletId);
            toWallet.setUser(toUser);
            toWallet.setBalance(new BigDecimal("100.00"));

            request = new TransferRequest();
            request.setFromWalletId(fromWalletId);
            request.setToWalletId(toWalletId);
            request.setAmount(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("should deduct amount from source and credit destination")
        void shouldTransferCorrectly() {
            when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
            when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            TransferResponse response = walletService.transfer(request);

            assertThat(response.getFromWalletBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(response.getToWalletBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("should create TRANSFER_OUT and TRANSFER_IN transaction records")
        void shouldCreateTwoTransactionRecords() {
            when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
            when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            walletService.transfer(request);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, times(2)).save(captor.capture());

            List<Transaction> saved = captor.getAllValues();
            assertThat(saved).extracting(Transaction::getType)
                    .containsExactlyInAnyOrder(TransactionType.TRANSFER_OUT, TransactionType.TRANSFER_IN);
        }

        @Test
        @DisplayName("should throw InsufficientFundsException and not persist anything")
        void shouldThrowAndNotPersistOnInsufficientFunds() {
            request.setAmount(new BigDecimal("999.00"));
            when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
            when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));

            assertThatThrownBy(() -> walletService.transfer(request))
                    .isInstanceOf(InsufficientFundsException.class);

            verify(walletRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when fromWalletId equals toWalletId")
        void shouldThrowWhenSameWallet() {
            request.setToWalletId(fromWalletId);

            assertThatThrownBy(() -> walletService.transfer(request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(walletRepository, never()).findById(any());
        }
    }

    // =========================================================================
    // getBalance — NEW in this cycle
    // =========================================================================

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("should return BalanceResponse with the wallet ID and current balance")
        void shouldReturnBalanceResponseWithCurrentBalance() {
            // Arrange
            wallet.setBalance(new BigDecimal("350.75"));
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

            // Act
            // FAILS: WalletService.getBalance() does not exist yet
            // FAILS: BalanceResponse DTO does not exist yet
            BalanceResponse response = walletService.getBalance(walletId);

            // Assert
            assertThat(response.getWalletId()).isEqualTo(walletId);
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("350.75"));
        }

        @Test
        @DisplayName("should return zero balance for a wallet that has never been funded")
        void shouldReturnZeroBalanceForUnfundedWallet() {
            // Arrange
            wallet.setBalance(BigDecimal.ZERO);
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

            // Act
            BalanceResponse response = walletService.getBalance(walletId);

            // Assert
            assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should use a read-only transaction — must not invoke walletRepository.save()")
        void shouldNotInvokeSaveOnReadOnlyOperation() {
            // Arrange
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

            // Act
            walletService.getBalance(walletId);

            // Assert — balance inquiry is a read-only operation: it must never write to the DB
            verify(walletRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when wallet does not exist")
        void shouldThrowResourceNotFoundExceptionWhenWalletDoesNotExist() {
            // Arrange
            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            // Act & Assert
            // FAILS: getBalance() does not exist
            assertThatThrownBy(() -> walletService.getBalance(walletId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(walletId.toString());
        }

        @Test
        @DisplayName("should reflect the latest balance after a deposit")
        void shouldReflectLatestBalanceAfterDeposit() {
            // Arrange — simulate wallet whose balance was updated by a deposit
            wallet.setBalance(new BigDecimal("175.50"));
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

            // Act
            BalanceResponse response = walletService.getBalance(walletId);

            // Assert — balance must match what is in the repository, not a stale value
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("175.50"));
        }
    }
}