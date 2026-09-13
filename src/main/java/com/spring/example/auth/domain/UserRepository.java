package com.spring.example.auth.domain;

import java.util.Optional;

/** User persistence port. The implementation lives in the infrastructure layer. */
public interface UserRepository {

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	User save(User user);
}
