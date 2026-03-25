package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.ResourceAlreadyExistsException;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(User user) {

        validateUniqueUser(user);

        return userRepository.save(user);
    }

    /**
     * Validates that username and email are unique.
     */
    private void validateUniqueUser(User user) {

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ResourceAlreadyExistsException(
                    "A user with the given username already exists"
            );
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "A user with the given email already exists"
            );
        }
    }
}