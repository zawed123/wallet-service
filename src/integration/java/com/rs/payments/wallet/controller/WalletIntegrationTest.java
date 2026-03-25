package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.BaseIntegrationTest;
import com.rs.payments.wallet.dto.AmountRequest;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

class WalletIntegrationTest extends BaseIntegrationTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    private User existingUser;
    private Wallet existingWallet;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        existingUser = new User();
        existingUser.setUsername("walletowner");
        existingUser.setEmail("walletowner@example.com");
        existingUser = userRepository.save(existingUser);

        existingWallet = createWalletViaApi(existingUser.getId());
    }

    // =========================================================================
    // POST /wallets — carried over
    // =========================================================================

    @Nested
    @DisplayName("POST /wallets")
    class CreateWallet {

        @Test
        @DisplayName("201 Created with zero balance for a valid user")
        void shouldCreateWalletAndReturn201() {
            User freshUser = new User();
            freshUser.setUsername("freshuser");
            freshUser.setEmail("fresh@example.com");
            freshUser = userRepository.save(freshUser);

            ResponseEntity<Wallet> response =
                    restTemplate.postForEntity(walletsUrl(), walletRequest(freshUser.getId()), Wallet.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("400 when user already has a wallet")
        void shouldReturn400WhenUserAlreadyHasWallet() {
            assertThatThrownBy(() ->
                    restTemplate.postForEntity(walletsUrl(), walletRequest(existingUser.getId()), String.class))
                    .isInstanceOf(HttpClientErrorException.class)
                    .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("404 when the referenced user does not exist")
        void shouldReturn404WhenUserDoesNotExist() {
            assertThatThrownBy(() ->
                    restTemplate.postForEntity(walletsUrl(), walletRequest(UUID.randomUUID()), String.class))
                    .isInstanceOf(HttpClientErrorException.class)
                    .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // =========================================================================
    // POST /wallets/{id}/deposit — carried over
    // =========================================================================

    @Nested
    @DisplayName("POST /wallets/{id}/deposit")
    class Deposit {

        @Test
        @DisplayName("200 OK — balance increases by the deposited amount")
        void shouldIncreaseBalanceByDepositAmount() {
            ResponseEntity<Wallet> response =
                    deposit(existingWallet.getId(), new BigDecimal("150.00"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getBalance())
                    .isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("400 Bad Request — amount of zero is rejected")
        void shouldReturn400WhenAmountIsZero() {
            assertThatThrownBy(() -> deposit(existingWallet.getId(), BigDecimal.ZERO))
                    .isInstanceOf(HttpClientErrorException.class)
                    .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("404 Not Found — depositing into a non-existent wallet")
        void shouldReturn404WhenWalletDoesNotExist() {
            assertThatThrownBy(() -> deposit(UUID.randomUUID(), new BigDecimal("50.00")))
                    .isInstanceOf(HttpClientErrorException.class)
                    .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // =========================================================================
    // POST /wallets/{id}/withdraw — NEW in this cycle
    // =========================================================================

    @Nested
    @DisplayName("POST /wallets/{id}/withdraw")
    class Withdraw {

        @Test
        @DisplayName("200 OK — balance decreases by the withdrawn amount")
        void shouldDecreaseBalanceByWithdrawalAmount() {
            // Arrange — fund the wallet first
            deposit(existingWallet.getId(), new BigDecimal("200.00"));

            // Act
            // FAILS: endpoint POST /wallets/{id}/withdraw does not exist yet → 404
            ResponseEntity<Wallet> response =
                    withdraw(existingWallet.getId(), new BigDecimal("80.00"));

            // Assert — 200.00 - 80.00 = 120.00
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getBalance())
                    .isEqualByComparingTo(new BigDecimal("120.00"));
        }

        @Test
        @DisplayName("200 OK — withdrawing the exact balance leaves zero")
        void shouldAllowWithdrawingEntireBalance() {
            // Arrange
            deposit(existingWallet.getId(), new BigDecimal("100.00"));

            // Act — withdraw everything
            ResponseEntity<Wallet> response =
                    withdraw(existingWallet.getId(), new BigDecimal("100.00"));

            // Assert — balance is exactly zero, never negative
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getBalance())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("200 OK — a WITHDRAWAL transaction record is created in the database")
        void shouldCreateWithdrawalTransactionRecord() {
            // Arrange
            deposit(existingWallet.getId(), new BigDecimal("100.00"));
            long countBefore = transactionRepository.count();

            // Act
            withdraw(existingWallet.getId(), new BigDecimal("40.00"));

            // Assert — one new transaction added
            assertThat(transactionRepository.count()).isEqualTo(countBefore + 1);
        }

        @Test
        @DisplayName("400 Bad Request — amount exceeds balance, balance stays unchanged")
        void shouldReturn400AndLeaveBalanceUnchangedOnInsufficientFunds() {
            // Arrange — fund with 50.00
            deposit(existingWallet.getId(), new BigDecimal("50.00"));

            // Act — try to withdraw 50.01
            // FAILS: endpoint missing, and InsufficientFundsException does not exist yet
            assertThatThrownBy(() -> withdraw(existingWallet.getId(), new BigDecimal("50.01")))
                    .isInstanceOf(HttpClientErrorException.class)
                    .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));

            // Assert — balance must be completely unchanged after the failed attempt
            ResponseEntity<Wallet> check =
                    restTemplate.getForEntity(balanceUrl(existingWallet.getId()), Wallet.class);
            assertThat(check.getBody().getBalance())
                    .isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("400 Bad Request — no transaction is recorded when funds are insufficient")
        void shouldNotCreateTransactionOnInsufficientFunds() {
            // Arrange
            deposit(existingWallet.getId(), new BigDecimal("50.00"));
            long countAfterDeposit = transactionRepository.count();

            // Act — try to overdraw
            assertThatThrownBy(() -> withdraw(existingWallet.getId(), new BigDecimal("999.00")))
                    .isInstanceOf(HttpClientErrorException.class);

            // Assert — transaction count must not change
            assertThat(transactionRepository.count()).isEqualTo(countAfterDeposit);
        }

        @Test
        @DisplayName("400 Bad Request — zero withdrawal amount is rejected by validation")
        void shouldReturn400WhenAmountIsZero() {
            assertThatThrownBy(() -> withdraw(existingWallet.getId(), BigDecimal.ZERO))
                    .isInstanceOf(HttpClientErrorException.class)
                    .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("400 Bad Request — negative withdrawal amount is rejected by validation")
        void shouldReturn400WhenAmountIsNegative() {
            assertThatThrownBy(() -> withdraw(existingWallet.getId(), new BigDecimal("-10.00")))
                    .isInstanceOf(HttpClientErrorException.class)
                    .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("404 Not Found — withdrawing from a non-existent wallet")
        void shouldReturn404WhenWalletDoesNotExist() {
            assertThatThrownBy(() -> withdraw(UUID.randomUUID(), new BigDecimal("10.00")))
                    .isInstanceOf(HttpClientErrorException.class)
                    .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String walletsUrl() {
        return "http://localhost:" + port + "/wallets";
    }

    private String depositUrl(UUID walletId) {
        return "http://localhost:" + port + "/wallets/" + walletId + "/deposit";
    }

    private String withdrawUrl(UUID walletId) {
        return "http://localhost:" + port + "/wallets/" + walletId + "/withdraw";
    }

    private String balanceUrl(UUID walletId) {
        return "http://localhost:" + port + "/wallets/" + walletId;
    }

    private CreateWalletRequest walletRequest(UUID userId) {
        CreateWalletRequest req = new CreateWalletRequest();
        req.setUserId(userId);
        return req;
    }

    private Wallet createWalletViaApi(UUID userId) {
        return restTemplate
                .postForEntity(walletsUrl(), walletRequest(userId), Wallet.class)
                .getBody();
    }

    private ResponseEntity<Wallet> deposit(UUID walletId, BigDecimal amount) {
        AmountRequest req = new AmountRequest();
        req.setAmount(amount);
        return restTemplate.postForEntity(depositUrl(walletId), req, Wallet.class);
    }

    private ResponseEntity<Wallet> withdraw(UUID walletId, BigDecimal amount) {
        AmountRequest req = new AmountRequest();
        req.setAmount(amount);
        return restTemplate.postForEntity(withdrawUrl(walletId), req, Wallet.class);
    }
}