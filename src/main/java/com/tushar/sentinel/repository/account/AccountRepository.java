package com.tushar.sentinel.repository.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountRef(String accountRef);

    boolean existsByAccountRef(String accountRef);
}
