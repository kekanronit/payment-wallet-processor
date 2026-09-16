\# Payment Wallet Processor



A Spring Boot backend application for processing wallet debit transactions with

idempotency and concurrency safety.



\## Tech Stack



\- Java 17+

\- Spring Boot

\- Spring Data JPA / Hibernate

\- H2 Database

\- Maven

\- JUnit 5

\- MockMvc



\## Features



\- Process wallet debit transactions

\- Idempotent transaction processing using unique transaction IDs

\- Prevent duplicate deductions during concurrent requests

\- Pessimistic database locking for wallet balance updates

\- Prevent negative wallet balances

\- Global exception handling for insufficient funds

\- H2 in-memory database for integration tests



\## API



\### Process Transaction



```http

POST /api/v1/transactions/process


{

&#x20; "transactionId": "11111111-1111-1111-1111-111111111111",

&#x20; "userId": "22222222-2222-2222-2222-222222222222",

&#x20; "amount": 100.00,

&#x20; "type": "DEBIT"

}

