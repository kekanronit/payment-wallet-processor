package com.example.payment_wallet_processor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest (

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid Email format")
    String email,

    @NotBlank(message = "Password is required")
    String password
){

}

