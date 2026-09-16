package com.example.payment_wallet_processor.repostiory;

import com.example.payment_wallet_processor.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

 public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(UUID transactionId);
    boolean existsByTransactionId(UUID transactionId);
}
