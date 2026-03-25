package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.AmountRequest;
import com.rs.payments.wallet.dto.BalanceResponse;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.exception.WalletAlreadyExistsException;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private UUID userId;
    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId   = UUID.randomUUID();
        walletId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("250.00"));
    }


    @Nested
    @DisplayName("POST /wallets")
    class CreateWallet {

        @Test
        @DisplayName("should return 201 Created with the new wallet")
        void shouldReturn201WhenWalletCreatedSuccessfully() {
            CreateWalletRequest request = new CreateWalletRequest();
            request.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            when(walletService.createWalletForUser(userId)).thenReturn(wallet);

            ResponseEntity<Wallet> response = walletController.createWallet(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(walletService).createWalletForUser(userId);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException → 404")
        void shouldPropagateResourceNotFoundException() {
            CreateWalletRequest request = new CreateWalletRequest();
            request.setUserId(userId);
            when(walletService.createWalletForUser(userId))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            assertThatThrownBy(() -> walletController.createWallet(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should propagate WalletAlreadyExistsException → 400")
        void shouldPropagateWalletAlreadyExistsException() {
            CreateWalletRequest request = new CreateWalletRequest();
            request.setUserId(userId);
            when(walletService.createWalletForUser(userId))
                    .thenThrow(new WalletAlreadyExistsException("Already has a wallet"));

            assertThatThrownBy(() -> walletController.createWallet(request))
                    .isInstanceOf(WalletAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("POST /wallets/{id}/deposit")
    class Deposit {

        @Test
        @DisplayName("should return 200 OK with updated wallet after deposit")
        void shouldReturn200WithUpdatedWalletOnDeposit() {
            AmountRequest request = new AmountRequest();
            request.setAmount(new BigDecimal("100.00"));
            when(walletService.deposit(walletId, new BigDecimal("100.00"))).thenReturn(wallet);

            ResponseEntity<Wallet> response = walletController.deposit(walletId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(walletService).deposit(walletId, new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException → 404")
        void shouldPropagateResourceNotFoundException() {
            AmountRequest request = new AmountRequest();
            request.setAmount(new BigDecimal("50.00"));
            when(walletService.deposit(walletId, new BigDecimal("50.00")))
                    .thenThrow(new ResourceNotFoundException("Wallet not found"));

            assertThatThrownBy(() -> walletController.deposit(walletId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


    @Nested
    @DisplayName("POST /wallets/{id}/withdraw")
    class Withdraw {

        @Test
        @DisplayName("should return 200 OK with updated wallet after withdrawal")
        void shouldReturn200WithUpdatedWalletOnWithdraw() {
            AmountRequest request = new AmountRequest();
            request.setAmount(new BigDecimal("50.00"));
            when(walletService.withdraw(walletId, new BigDecimal("50.00"))).thenReturn(wallet);

            ResponseEntity<Wallet> response = walletController.withdraw(walletId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(walletService).withdraw(walletId, new BigDecimal("50.00"));
        }
    }


    @Nested
    @DisplayName("GET /wallets/{id}/balance")
    class GetBalance {

        @Test
        @DisplayName("should return 200 OK with BalanceResponse containing the current balance")
        void shouldReturn200WithBalanceResponse() {
            BalanceResponse expectedResponse = new BalanceResponse(walletId, new BigDecimal("250.00"));
            when(walletService.getBalance(walletId)).thenReturn(expectedResponse);
            ResponseEntity<BalanceResponse> response = walletController.getBalance(walletId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getWalletId()).isEqualTo(walletId);
            assertThat(response.getBody().getBalance())
                    .isEqualByComparingTo(new BigDecimal("250.00"));
            verify(walletService).getBalance(walletId);
        }

        @Test
        @DisplayName("should return 200 OK with zero balance for a newly created wallet")
        void shouldReturn200WithZeroBalanceForNewWallet() {
            // Arrange
            BalanceResponse zeroBalance = new BalanceResponse(walletId, BigDecimal.ZERO);
            when(walletService.getBalance(walletId)).thenReturn(zeroBalance);

            // Act
            ResponseEntity<BalanceResponse> response = walletController.getBalance(walletId);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException so handler maps it to 404")
        void shouldPropagateResourceNotFoundExceptionWhenWalletDoesNotExist() {
            // Arrange
            when(walletService.getBalance(walletId))
                    .thenThrow(new ResourceNotFoundException(
                            "Wallet not found with id: " + walletId));

            assertThatThrownBy(() -> walletController.getBalance(walletId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wallet not found");
        }
    }
}
