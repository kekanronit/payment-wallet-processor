package com.example.payment_wallet_processor.dto;

import com.example.payment_wallet_processor.entity.TransactionStatus;
import com.example.payment_wallet_processor.entity.TransactionType;


import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse (

    UUID transactionId,
    UUID userId,
    BigDecimal amount,
    TransactionType type,
    TransactionStatus status,
    BigDecimal balance,
    String message

    ){


}

