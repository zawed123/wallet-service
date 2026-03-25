package com.rs.payments.wallet.repository;

import java.util.UUID;
import com.rs.payments.wallet.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for handling Transaction entities.
 * Extends JpaRepository to provide standard database operations
 * such as save, find, update, and delete for transaction records.
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    // Additional query methods can be defined here if needed
}