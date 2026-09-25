package com.example.payment_wallet_processor.dto;



public record LoginResponse (
    String token,
    String tokenType,
    String email,
    String role


    ){

}

