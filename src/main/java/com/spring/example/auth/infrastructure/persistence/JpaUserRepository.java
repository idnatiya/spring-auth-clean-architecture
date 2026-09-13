package com.spring.example.auth.infrastructure.persistence;

import com.spring.example.auth.domain.User;
import com.spring.example.auth.domain.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserRepository implements UserRepository {

	private final SpringDataUserRepository delegate;

	JpaUserRepository(SpringDataUserRepository delegate) {
		this.delegate = delegate;
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return delegate.findByUsername(username);
	}

	@Override
	public boolean existsByUsername(String username) {
		return delegate.existsByUsername(username);
	}

	@Override
	public User save(User user) {
		return delegate.save(user);
	}
}
