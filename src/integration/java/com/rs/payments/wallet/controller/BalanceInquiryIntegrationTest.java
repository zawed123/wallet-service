package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.BaseIntegrationTest;
import com.rs.payments.wallet.dto.AmountRequest;
import com.rs.payments.wallet.dto.BalanceResponse;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.dto.TransferRequest;
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

class BalanceInquiryIntegrationTest extends BaseIntegrationTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user = userRepository.save(user);

        wallet = createWalletViaApi(user.getId());
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    @DisplayName("GET /wallets/{id}/balance - 200 OK with zero balance for a new wallet")
    void shouldReturnZeroBalanceForNewWallet() {
        // Act
        // FAILS: GET /wallets/{id}/balance endpoint does not exist yet → 404
        // FAILS: BalanceResponse DTO does not exist yet
        ResponseEntity<BalanceResponse> response =
                restTemplate.getForEntity(balanceUrl(wallet.getId()), BalanceResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getWalletId()).isEqualTo(wallet.getId());
        assertThat(response.getBody().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance - 200 OK with correct balance after a deposit")
    void shouldReturnCorrectBalanceAfterDeposit() {
        // Arrange
        depositViaApi(wallet.getId(), new BigDecimal("300.00"));

        // Act
        ResponseEntity<BalanceResponse> response =
                restTemplate.getForEntity(balanceUrl(wallet.getId()), BalanceResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBalance())
                .isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance - 200 OK with correct balance after deposit then withdrawal")
    void shouldReturnCorrectBalanceAfterDepositAndWithdrawal() {
        // Arrange
        depositViaApi(wallet.getId(), new BigDecimal("500.00"));
        withdrawViaApi(wallet.getId(), new BigDecimal("150.00"));

        // Act
        ResponseEntity<BalanceResponse> response =
                restTemplate.getForEntity(balanceUrl(wallet.getId()), BalanceResponse.class);

        // Assert — 500 - 150 = 350
        assertThat(response.getBody().getBalance())
                .isEqualByComparingTo(new BigDecimal("350.00"));
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance - reflects balance after receiving a transfer")
    void shouldReflectBalanceAfterReceivingTransfer() {
        // Arrange — create a second user and wallet (the sender)
        User sender = new User();
        sender.setUsername("bob");
        sender.setEmail("bob@example.com");
        sender = userRepository.save(sender);
        Wallet senderWallet = createWalletViaApi(sender.getId());
        depositViaApi(senderWallet.getId(), new BigDecimal("400.00"));

        // Execute transfer to alice's wallet
        transferViaApi(senderWallet.getId(), wallet.getId(), new BigDecimal("200.00"));

        // Act — check alice's balance
        ResponseEntity<BalanceResponse> response =
                restTemplate.getForEntity(balanceUrl(wallet.getId()), BalanceResponse.class);

        // Assert — alice started at 0 and received 200
        assertThat(response.getBody().getBalance())
                .isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance - reflects balance after sending a transfer")
    void shouldReflectBalanceAfterSendingTransfer() {
        // Arrange
        depositViaApi(wallet.getId(), new BigDecimal("500.00"));

        User receiver = new User();
        receiver.setUsername("charlie");
        receiver.setEmail("charlie@example.com");
        receiver = userRepository.save(receiver);
        Wallet receiverWallet = createWalletViaApi(receiver.getId());

        // Execute transfer from alice to charlie
        transferViaApi(wallet.getId(), receiverWallet.getId(), new BigDecimal("100.00"));

        // Act
        ResponseEntity<BalanceResponse> response =
                restTemplate.getForEntity(balanceUrl(wallet.getId()), BalanceResponse.class);

        // Assert — 500 - 100 = 400
        assertThat(response.getBody().getBalance())
                .isEqualByComparingTo(new BigDecimal("400.00"));
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance - balance is exactly zero after full withdrawal")
    void shouldReturnExactZeroAfterFullWithdrawal() {
        // Arrange
        depositViaApi(wallet.getId(), new BigDecimal("100.00"));
        withdrawViaApi(wallet.getId(), new BigDecimal("100.00"));

        // Act
        ResponseEntity<BalanceResponse> response =
                restTemplate.getForEntity(balanceUrl(wallet.getId()), BalanceResponse.class);

        // Assert — must be exactly 0, never negative
        assertThat(response.getBody().getBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance - response body contains the wallet ID")
    void shouldIncludeWalletIdInResponse() {
        // Act
        ResponseEntity<BalanceResponse> response =
                restTemplate.getForEntity(balanceUrl(wallet.getId()), BalanceResponse.class);

        // Assert — callers must be able to correlate the response to the wallet
        assertThat(response.getBody().getWalletId()).isEqualTo(wallet.getId());
    }

    // =========================================================================
    // Not found — 404
    // =========================================================================

    @Test
    @DisplayName("GET /wallets/{id}/balance - 404 when wallet does not exist")
    void shouldReturn404WhenWalletDoesNotExist() {
        // Act & Assert
        assertThatThrownBy(() -> restTemplate.getForEntity(
                balanceUrl(UUID.randomUUID()), BalanceResponse.class))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
    // =========================================================================
    // Helpers
    // =========================================================================

    private String walletsUrl() {
        return "http://localhost:" + port + "/wallets";
    }

    private String balanceUrl(UUID walletId) {
        return "http://localhost:" + port + "/wallets/" + walletId + "/balance";
    }

    private String depositUrl(UUID walletId) {
        return "http://localhost:" + port + "/wallets/" + walletId + "/deposit";
    }

    private String withdrawUrl(UUID walletId) {
        return "http://localhost:" + port + "/wallets/" + walletId + "/withdraw";
    }

    private String transfersUrl() {
        return "http://localhost:" + port + "/transfers";
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

    private void withdrawViaApi(UUID walletId, BigDecimal amount) {
        AmountRequest req = new AmountRequest();
        req.setAmount(amount);
        restTemplate.postForEntity(withdrawUrl(walletId), req, Wallet.class);
    }

    private void transferViaApi(UUID from, UUID to, BigDecimal amount) {
        TransferRequest req = new TransferRequest();
        req.setFromWalletId(from);
        req.setToWalletId(to);
        req.setAmount(amount);
        restTemplate.postForEntity(transfersUrl(), req, Object.class);
    }
}
