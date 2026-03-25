package com.rs.payments.wallet.service;

import com.rs.payments.wallet.model.User;

/**
 * Service interface for managing user-related operations.
 * Defines core business functionality for user creation and validation.
 */
public interface UserService {

    /**
     * Creates a new user in the system.
     *
     * @param user user details to be persisted
     * @return created user entity
     */
    User createUser(User user);
}