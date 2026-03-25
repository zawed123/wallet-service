package com.rs.payments.wallet.repository;

import java.util.UUID;
import com.rs.payments.wallet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing User entities.
 * Provides basic CRUD operations via JpaRepository along with
 * custom existence checks for username and email to enforce uniqueness.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Checks if a user already exists with the given username.
     *
     * @param username the username to verify
     * @return true if a user with the username exists, otherwise false
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user already exists with the given email.
     *
     * @param email the email to verify
     * @return true if a user with the email exists, otherwise false
     */
    boolean existsByEmail(String email);
}