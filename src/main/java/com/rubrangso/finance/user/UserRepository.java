package com.rubrangso.finance.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link User} records. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
