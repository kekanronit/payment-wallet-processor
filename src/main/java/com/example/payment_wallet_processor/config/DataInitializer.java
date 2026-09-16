package com.example.payment_wallet_processor.config;

import com.example.payment_wallet_processor.entity.Wallet;
import com.example.payment_wallet_processor.repostiory.WalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeWallet(WalletRepository walletRepository) {
        return args -> {

            UUID userId = UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );
if (walletRepository.findByUserId(userId).isEmpty()){

    Wallet  wallet = new Wallet(
            userId,
            new BigDecimal("500.00")
    );

    walletRepository.save(wallet);

    System.out.println("Test wallet has been created" + userId + " | Balance : 500.00"

    );
          }

        };
    }
}
