package com.tushar.hackathon.repository.customer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerRef(String customerRef);

    boolean existsByCustomerRef(String customerRef);
}
