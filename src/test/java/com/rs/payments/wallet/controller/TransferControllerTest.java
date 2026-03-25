package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.exception.InsufficientFundsException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private WalletService walletService;

    // FAILS: TransferController class does not exist yet
    @InjectMocks
    private TransferController transferController;

    private UUID fromWalletId;
    private UUID toWalletId;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        fromWalletId = UUID.randomUUID();
        toWalletId   = UUID.randomUUID();

        // FAILS: TransferRequest class does not exist yet
        request = new TransferRequest();
        request.setFromWalletId(fromWalletId);
        request.setToWalletId(toWalletId);
        request.setAmount(new BigDecimal("100.00"));
    }

    // =========================================================================
    // POST /transfers — happy path
    // =========================================================================

    @Test
    @DisplayName("POST /transfers - should return 200 OK with TransferResponse on success")
    void shouldReturn200WithTransferResponseOnSuccess() {
        // Arrange
        // FAILS: TransferResponse class does not exist yet
        TransferResponse transferResponse = new TransferResponse(
                fromWalletId,
                toWalletId,
                new BigDecimal("100.00"),
                new BigDecimal("400.00"),   // fromWallet balance after transfer
                new BigDecimal("600.00"),   // toWallet balance after transfer
                LocalDateTime.now()
        );

        // FAILS: WalletService.transfer() method does not exist yet
        when(walletService.transfer(request)).thenReturn(transferResponse);

        // Act
        ResponseEntity<TransferResponse> response = transferController.transfer(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getFromWalletId()).isEqualTo(fromWalletId);
        assertThat(response.getBody().getToWalletId()).isEqualTo(toWalletId);
        assertThat(response.getBody().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getBody().getFromWalletBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(response.getBody().getToWalletBalance()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(response.getBody().getTimestamp()).isNotNull();
        verify(walletService).transfer(request);
    }

    // =========================================================================
    // POST /transfers — error paths
    // =========================================================================

    @Test
    @DisplayName("POST /transfers - should propagate InsufficientFundsException → 400")
    void shouldPropagateInsufficientFundsExceptionWhenSourceBalanceTooLow() {
        // Arrange
        when(walletService.transfer(request))
                .thenThrow(new InsufficientFundsException(
                        "Insufficient funds: balance is 50.00, requested 100.00"));

        // Act & Assert
        assertThatThrownBy(() -> transferController.transfer(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("POST /transfers - should propagate ResourceNotFoundException → 404")
    void shouldPropagateResourceNotFoundExceptionWhenWalletDoesNotExist() {
        // Arrange
        when(walletService.transfer(request))
                .thenThrow(new ResourceNotFoundException(
                        "Wallet not found with id: " + fromWalletId));

        // Act & Assert
        assertThatThrownBy(() -> transferController.transfer(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    @Test
    @DisplayName("POST /transfers - should propagate IllegalArgumentException → 400 for same-wallet transfer")
    void shouldPropagateIllegalArgumentExceptionWhenTransferringToSameWallet() {
        // Arrange — same wallet on both sides
        request.setToWalletId(fromWalletId);
        when(walletService.transfer(request))
                .thenThrow(new IllegalArgumentException(
                        "Cannot transfer to the same wallet"));

        // Act & Assert
        assertThatThrownBy(() -> transferController.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same wallet");
    }
}
