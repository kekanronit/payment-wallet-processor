package com.example.payment_wallet_processor;


import com.example.payment_wallet_processor.entity.Wallet;
import com.example.payment_wallet_processor.exception.InsufficientFundsException;
import com.example.payment_wallet_processor.repostiory.TransactionRepository;
import com.example.payment_wallet_processor.repostiory.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc


public class TransactionConcurrencyTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final UUID userId =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final UUID transactionId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setup(){
        transactionRepository.deleteAll();
        walletRepository.deleteAll();

        Wallet wallet = new Wallet(
                userId,
                new BigDecimal("500.00")
        );

        walletRepository.save(wallet);

    }

    @Test
    @DisplayName("Sends 3 identical transactionID simultaneously. Ensures the balance is only deducted once")
    void shouldprocessIdenticalTransactionOnlyOnce()
            throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        CountDownLatch ready = new  CountDownLatch(3);

        CountDownLatch start = new CountDownLatch(1);

        CountDownLatch finished = new CountDownLatch(3);

        String requestBody = """
        {
            "transactionId": "11111111-1111-1111-1111-111111111111",
            "userId": "22222222-2222-2222-2222-222222222222",
            "amount": 100.00,
            "type": "DEBIT"
        }
        """;

        for(int i = 0; i < 3; i++){

            executorService.execute(() -> {

                try {
                    ready.countDown();

                    start.await();

                    mockMvc.perform(
                            MockMvcRequestBuilders.post("/api/v1/transactions/process")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    ).andReturn();
                } catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    finished.countDown();
                }

            });
        }

        ready.await();

        start.countDown();

        finished.await();

        executorService.shutdown();

        Wallet wallet = walletRepository
                .findByUserId(userId)
                .orElseThrow();

        BigDecimal finalBalance = wallet.getBalance();

        long transactionCount = transactionRepository.count();

        System.out.println();
        System.out.println("======================================");
        System.out.println("3 IDENTICAL CONCURRENT REQUEST TEST");
        System.out.println("Initial Balance : ₹500.00");
        System.out.println("Transaction Amount : ₹100.00");
        System.out.println("Requests Sent : 3");
        System.out.println("Transactions Stored : " + transactionCount);
        System.out.println("Final Balance : ₹" + finalBalance);
        System.out.println("======================================");

        Assertions.assertEquals(
                new BigDecimal("400.00"),
                finalBalance
        );

        Assertions.assertEquals(
                1,
                transactionCount
        );
    }

    @Test
    @DisplayName("Sends 10 concurrent debit requests. Ensures only 5 succeed due to insufficient funds")
    void shouldAllowOnlyFiveConcurrentDebits()
              throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        CountDownLatch ready = new CountDownLatch(10);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(10);

        String userIdString = userId.toString();

        int[] successCount = {0};
        int[] insufficientFundsCount = {0};

        for (int i = 0; i < 10; i++) {

            final int requestNumber = i;

            executorService.execute(() -> {

                try {
                    ready.countDown();

                    start.await();

                    String transactionIdString =
                            UUID.randomUUID().toString();

                    String requestBody = """
                {
                    "transactionId": "%s",
                    "userId": "%s",
                    "amount": 100.00,
                    "type": "DEBIT"
                }
                """.formatted(
                            transactionIdString,
                            userIdString
                    );

                    int status = mockMvc.perform(
                            MockMvcRequestBuilders.post(
                                            "/api/v1/transactions/process"
                                    )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    ).andReturn().getResponse().getStatus();

                    if (status >= 200 && status < 300) {
                        synchronized (successCount) {
                            successCount[0]++;
                        }
                    }

                    if (status == 409) {
                        synchronized (insufficientFundsCount) {
                            insufficientFundsCount[0]++;
                        }
                    }

                } catch (Exception e) {
                    Throwable cause = e;

                    while (cause != null) {

                        if (cause instanceof com.example.payment_wallet_processor.exception.InsufficientFundsException) {

                            synchronized (insufficientFundsCount) {
                                insufficientFundsCount[0]++;
                            }

                            return;
                        }

                        cause = cause.getCause();
                    }
                    throw new RuntimeException(e);
                } finally {
                    finished.countDown();
                }

            });
        }

        // Make sure all 10 threads are ready
        ready.await();

        // Start all requests together
        start.countDown();

        // Wait for all requests to finish
        finished.await();

        executorService.shutdown();

        Wallet wallet = walletRepository
                .findByUserId(userId)
                .orElseThrow();

        BigDecimal finalBalance = wallet.getBalance();

        long transactionCount = transactionRepository.count();

        System.out.println();
        System.out.println("======================================");
        System.out.println("10 CONCURRENT DEBIT REQUEST TEST");
        System.out.println("Initial Balance       : ₹500.00");
        System.out.println("Transaction Amount    : ₹100.00");
        System.out.println("Requests Sent         : 10");
        System.out.println("Successful Requests   : " + successCount[0]);
        System.out.println("Insufficient Funds    : " + insufficientFundsCount[0]);
        System.out.println("Transactions Stored   : " + transactionCount);
        System.out.println("Final Balance         : ₹" + finalBalance);
        System.out.println("======================================");

        assertEquals(
                5,
                successCount[0]
        );

        assertEquals(
                5,
                insufficientFundsCount[0]
        );

        assertEquals(
                new BigDecimal("0.00"),
                finalBalance
        );

        assertEquals(
                5,
                transactionCount
        );
    }

  


   
}
