package com.example.payment_wallet_processor.dto;

import com.example.payment_wallet_processor.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest (

    @NotNull
    UUID transactionId,

    @NotNull
    UUID userId,

    @NotNull
    @DecimalMin(value = "0.01")
    BigDecimal amount,

    @NotNull
    TransactionType type

    ){
    
}


