package com.spring.example.auth.infrastructure.persistence;

import com.spring.example.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);
}
