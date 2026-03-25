package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.BaseIntegrationTest;
import com.rs.payments.wallet.dto.AmountRequest;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferIntegrationTest extends BaseIntegrationTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;

    private Wallet aliceWallet;
    private Wallet bobWallet;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        // Alice starts with 500.00
        User alice = new User();
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice = userRepository.save(alice);
        aliceWallet = createWalletViaApi(alice.getId());
        depositViaApi(aliceWallet.getId(), new BigDecimal("500.00"));

        // Bob starts with 100.00
        User bob = new User();
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        bob = userRepository.save(bob);
        bobWallet = createWalletViaApi(bob.getId());
        depositViaApi(bobWallet.getId(), new BigDecimal("100.00"));
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    @DisplayName("POST /transfers - 200 OK with correct balances in TransferResponse")
    void shouldTransferFundsAndReturnCorrectBalances() {
        // Arrange
        TransferRequest request = transferRequest(
                aliceWallet.getId(), bobWallet.getId(), new BigDecimal("200.00"));

        // Act
        // FAILS: POST /transfers endpoint does not exist yet → 404
        // FAILS: TransferRequest DTO does not exist yet
        // FAILS: TransferResponse DTO does not exist yet
        // FAILS: WalletService.transfer() does not exist yet
        ResponseEntity<TransferResponse> response =
                restTemplate.postForEntity(transfersUrl(), request, TransferResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getFromWalletId()).isEqualTo(aliceWallet.getId());
        assertThat(response.getBody().getToWalletId()).isEqualTo(bobWallet.getId());
        assertThat(response.getBody().getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        // Alice: 500 - 200 = 300
        assertThat(response.getBody().getFromWalletBalance())
                .isEqualByComparingTo(new BigDecimal("300.00"));
        // Bob: 100 + 200 = 300
        assertThat(response.getBody().getToWalletBalance())
                .isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("POST /transfers - source and destination balances are actually persisted in the DB")
    void shouldPersistBothBalancesInDatabase() {
        // Act
        restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("150.00")),
                TransferResponse.class);

        // Assert — reload from DB directly, not from response body
        Wallet alice = walletRepository.findById(aliceWallet.getId()).orElseThrow();
        Wallet bob   = walletRepository.findById(bobWallet.getId()).orElseThrow();

        assertThat(alice.getBalance()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(bob.getBalance()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("POST /transfers - exactly two transaction records created: TRANSFER_OUT and TRANSFER_IN")
    void shouldCreateTwoTransactionRecords() {
        // Arrange
        long countBefore = transactionRepository.count();

        // Act
        restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("50.00")),
                TransferResponse.class);

        // Assert — exactly 2 new transaction rows
        assertThat(transactionRepository.count()).isEqualTo(countBefore + 2);
    }

    @Test
    @DisplayName("POST /transfers - transferring the entire source balance leaves exactly zero")
    void shouldAllowTransferOfEntireBalance() {
        // Act
        ResponseEntity<TransferResponse> response =
                restTemplate.postForEntity(transfersUrl(),
                        transferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("500.00")),
                        TransferResponse.class);

        // Assert
        assertThat(response.getBody().getFromWalletBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // Insufficient funds — 400 with full rollback
    // =========================================================================

    @Test
    @DisplayName("POST /transfers - 400 when source balance is insufficient")
    void shouldReturn400WhenSourceBalanceIsInsufficient() {
        // Alice has 500.00 — try to send 500.01
        assertThatThrownBy(() -> restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("500.01")),
                String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("POST /transfers - source balance is completely unchanged after failed transfer")
    void shouldLeaveSourceBalanceUnchangedAfterFailedTransfer() {
        // Act — attempt overdraft
        assertThatThrownBy(() -> restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("999.00")),
                String.class))
                .isInstanceOf(HttpClientErrorException.class);

        // Assert — Alice's balance must still be 500.00
        Wallet alice = walletRepository.findById(aliceWallet.getId()).orElseThrow();
        assertThat(alice.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("POST /transfers - destination balance is completely unchanged after failed transfer")
    void shouldLeaveDestinationBalanceUnchangedAfterFailedTransfer() {
        // Act — attempt overdraft
        assertThatThrownBy(() -> restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("999.00")),
                String.class))
                .isInstanceOf(HttpClientErrorException.class);

        // Assert — Bob's balance must still be 100.00
        Wallet bob = walletRepository.findById(bobWallet.getId()).orElseThrow();
        assertThat(bob.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("POST /transfers - no transaction records created after failed transfer")
    void shouldNotCreateTransactionRecordsOnFailedTransfer() {
        long countBefore = transactionRepository.count();

        assertThatThrownBy(() -> restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("999.00")),
                String.class))
                .isInstanceOf(HttpClientErrorException.class);

        assertThat(transactionRepository.count()).isEqualTo(countBefore);
    }

    // =========================================================================
    // Validation — 400
    // =========================================================================

    @Test
    @DisplayName("POST /transfers - 400 when amount is zero")
    void shouldReturn400WhenAmountIsZero() {
        assertThatThrownBy(() -> restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), bobWallet.getId(), BigDecimal.ZERO),
                String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("POST /transfers - 400 when amount is negative")
    void shouldReturn400WhenAmountIsNegative() {
        assertThatThrownBy(() -> restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("-10.00")),
                String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("POST /transfers - 400 when fromWalletId equals toWalletId")
    void shouldReturn400WhenTransferringToSameWallet() {
        assertThatThrownBy(() -> restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), aliceWallet.getId(), new BigDecimal("10.00")),
                String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // =========================================================================
    // Not found — 404
    // =========================================================================

    @Test
    @DisplayName("POST /transfers - 404 when source wallet does not exist")
    void shouldReturn404WhenSourceWalletDoesNotExist() {
        assertThatThrownBy(() -> restTemplate.postForEntity(transfersUrl(),
                transferRequest(UUID.randomUUID(), bobWallet.getId(), new BigDecimal("50.00")),
                String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("POST /transfers - 404 when destination wallet does not exist")
    void shouldReturn404WhenDestinationWalletDoesNotExist() {
        assertThatThrownBy(() -> restTemplate.postForEntity(transfersUrl(),
                transferRequest(aliceWallet.getId(), UUID.randomUUID(), new BigDecimal("50.00")),
                String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String transfersUrl() {
        return "http://localhost:" + port + "/transfers";
    }

    private String walletsUrl() {
        return "http://localhost:" + port + "/wallets";
    }

    private String depositUrl(UUID walletId) {
        return "http://localhost:" + port + "/wallets/" + walletId + "/deposit";
    }

    private Wallet createWalletViaApi(UUID userId) {
        CreateWalletRequest req = new CreateWalletRequest();
        req.setUserId(userId);
        return restTemplate.postForEntity(walletsUrl(), req, Wallet.class).getBody();
    }

    private void depositViaApi(UUID walletId, BigDecimal amount) {
        AmountRequest req = new AmountRequest();
        req.setAmount(amount);
        restTemplate.postForEntity(depositUrl(walletId), req, Wallet.class);
    }

    private TransferRequest transferRequest(UUID from, UUID to, BigDecimal amount) {
        TransferRequest req = new TransferRequest();
        req.setFromWalletId(from);
        req.setToWalletId(to);
        req.setAmount(amount);
        return req;
    }
}