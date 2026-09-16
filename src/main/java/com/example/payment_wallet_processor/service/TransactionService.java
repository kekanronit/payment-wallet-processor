package com.example.payment_wallet_processor.service;

import com.example.payment_wallet_processor.dto.TransactionRequest;
import com.example.payment_wallet_processor.dto.TransactionResponse;
import com.example.payment_wallet_processor.entity.Transaction;
import com.example.payment_wallet_processor.entity.TransactionStatus;
import com.example.payment_wallet_processor.entity.TransactionType;
import com.example.payment_wallet_processor.entity.Wallet;
import com.example.payment_wallet_processor.exception.InsufficientFundsException;
import com.example.payment_wallet_processor.exception.WalletNotFoundException;
import com.example.payment_wallet_processor.repostiory.TransactionRepository;
import com.example.payment_wallet_processor.repostiory.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse processTransaction(
            TransactionRequest transactionRequest
    ) {

        // 1. Lock the wallet
        Wallet wallet = walletRepository
                .findByUserIdForUpdate(transactionRequest.userId())
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user "
                                        + transactionRequest.userId()
                        )
                );

        // 2. Check if transaction was already processed
        var existingTransaction =
                transactionRepository.findByTransactionId(
                        transactionRequest.transactionId()
                );

        if (existingTransaction.isPresent()) {

            Transaction existing = existingTransaction.get();

            return new TransactionResponse(
                    existing.getTransactionId(),
                    existing.getUserId(),
                    existing.getAmount(),
                    existing.getType(),
                    existing.getStatus(),
                    wallet.getBalance(),
                    "Transaction already processed"
            );
        }

        // 3. Process transaction
        processWalletBalance(wallet, transactionRequest);

        // 4. Save updated wallet
        walletRepository.save(wallet);

        // 5. Save transaction record
        Transaction transaction = new Transaction(
                transactionRequest.transactionId(),
                transactionRequest.userId(),
                transactionRequest.amount(),
                transactionRequest.type(),
                TransactionStatus.SUCCESS,
                LocalDateTime.now()
        );

        transactionRepository.save(transaction);

        // 6. Return successful response
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                wallet.getBalance(),
                "Transaction processed successfully"
        );
    }

    private void processWalletBalance(
            Wallet wallet,
            TransactionRequest transactionRequest
    ) {

        if (transactionRequest.type() == TransactionType.DEBIT) {

            if (wallet.getBalance()
                    .compareTo(transactionRequest.amount()) < 0) {

                throw new InsufficientFundsException(
                        "Insufficient Wallet balance"
                );
            }

            wallet.setBalance(
                    wallet.getBalance()
                            .subtract(transactionRequest.amount())
            );

        } else if (transactionRequest.type() == TransactionType.CREDIT) {

            wallet.setBalance(
                    wallet.getBalance()
                            .add(transactionRequest.amount())
            );
        }
    }
}