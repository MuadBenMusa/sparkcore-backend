package com.sparkcore.backend.repository;

import com.sparkcore.backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Spring Boot Magie: Sucht alle Transaktionen, bei denen die IBAN entweder Sender oder Empfänger ist,
    // und sortiert sie nach Datum (neueste zuerst)!
    List<Transaction> findBySenderIbanOrReceiverIbanOrderByTimestampDesc(String senderIban, String receiverIban);
}