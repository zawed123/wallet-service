package com.rs.payments.wallet.repository;

import java.util.UUID;
import com.rs.payments.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for performing database operations on Wallet entities.
 * Extends JpaRepository to provide standard CRUD functionality,
 * along with a custom check to ensure a user has only one wallet.
 */
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Verifies whether a wallet is already associated with the given user.
     *
     * @param userId the unique identifier of the user
     * @return true if a wallet exists for the user, otherwise false
     */
    boolean existsByUserId(UUID userId);
}