package com.example.payment_wallet_processor.controller;

import com.example.payment_wallet_processor.dto.TransactionRequest;
import com.example.payment_wallet_processor.dto.TransactionResponse;
import com.example.payment_wallet_processor.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService  transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processTransaction(@Valid @RequestBody TransactionRequest transactionRequest) {

        TransactionResponse response = transactionService.processTransaction(transactionRequest);

        return  ResponseEntity.ok(response);

    }


}
