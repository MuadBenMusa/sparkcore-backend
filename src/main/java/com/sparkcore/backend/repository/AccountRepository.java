package com.sparkcore.backend.repository;

import com.sparkcore.backend.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
// JpaRepository Methoden wie save(), findAll() findById()
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends  JpaRepository<Account, Long> {

    Optional<Account> findByIban(String iban);
}
