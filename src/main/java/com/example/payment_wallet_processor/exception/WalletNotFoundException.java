package com.example.payment_wallet_processor.exception;

public class WalletNotFoundException extends RuntimeException{

    public WalletNotFoundException(String message){
        super(message);
    }
}
