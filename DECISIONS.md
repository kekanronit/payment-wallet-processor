# Decision Log

## 1. How did you handle the concurrency race condition?

The main concurrency problem occurs when multiple debit requests try to update the same wallet at the same time.

For example, if a wallet has ₹500 and 10 requests simultaneously try to debit ₹100, the application must not allow the balance to become negative.

To solve this, I used **database-level pessimistic locking** on the wallet record.

The wallet row is locked while the transaction is being processed. The database uses a `FOR UPDATE` query to prevent multiple concurrent transactions from modifying the same wallet balance at the same time.

The flow is:

1. A debit request starts a database transaction.
2. The wallet row is locked using pessimistic locking.
3. The current wallet balance is read.
4. The application checks whether sufficient funds are available.
5. If sufficient funds are available, the amount is deducted.
6. The transaction record is saved.
7. The database transaction commits and the lock is released.
8. The next waiting request can then process the latest wallet balance.

For example:

```text
Initial Balance = ₹500

Request 1 → ₹500 → ₹400 → SUCCESS
Request 2 → ₹400 → ₹300 → SUCCESS
Request 3 → ₹300 → ₹200 → SUCCESS
Request 4 → ₹200 → ₹100 → SUCCESS
Request 5 → ₹100 → ₹0   → SUCCESS

Request 6  → ₹0 → INSUFFICIENT FUNDS
Request 7  → ₹0 → INSUFFICIENT FUNDS
Request 8  → ₹0 → INSUFFICIENT FUNDS
Request 9  → ₹0 → INSUFFICIENT FUNDS
Request 10 → ₹0 → INSUFFICIENT FUNDS
```

Therefore, with 10 concurrent requests of ₹100 against a ₹500 wallet:

* 5 requests succeed.
* 5 requests fail because of insufficient funds.
* Final balance is ₹0.
* No negative balance is created.

The concurrency test verifies these conditions automatically.

---

## 2. How did you handle idempotency?

Payment gateways can retry the same webhook request because of network failures or timeouts.

This means the same `transactionId` can arrive multiple times, potentially at the same time.

To prevent the same transaction from being processed multiple times, I use `transactionId` as the idempotency key.

The transaction ID has a **unique database constraint**, which prevents duplicate transaction records from being stored.

The application also checks whether the transaction has already been processed before creating a new transaction.

For example:

```text
Request 1 → transactionId = ABC
Request 2 → transactionId = ABC
Request 3 → transactionId = ABC
```

Only one transaction is processed and the wallet balance is deducted once.

The idempotency integration test sends three identical transaction IDs concurrently and verifies:

```text
Initial Balance       : ₹500
Transaction Amount    : ₹100
Requests Sent         : 3
Transactions Stored   : 1
Final Balance         : ₹400
```

This confirms that duplicate webhook requests do not result in multiple balance deductions.

---

## 3. Why did I use H2 for testing?

The assignment requires zero-config integration testing.

I used an **H2 in-memory database** for the test environment.

This means the reviewer does not need to install or configure MySQL or any other external database to run the tests.

The test database is created when the test starts and exists only for the test execution.

The benefits are:

* No external database setup is required.
* Tests can be executed directly from IntelliJ.
* Tests run quickly.
* Each test starts with a clean database state.
* The test suite can be executed in a zero-config environment.

---

## 4. Why did I use CountDownLatch in the concurrency tests?

The concurrency tests need multiple requests to execute at approximately the same time.

I used `CountDownLatch` to coordinate the worker threads.

The test uses three latches:

### Ready latch

The ready latch waits until all worker threads are ready.

```text
10 threads
   ↓
All become ready
   ↓
Start signal
```

### Start latch

The start latch keeps the worker threads waiting until all threads are ready.

Once the latch is released, all workers can send their requests.

This helps reproduce concurrent request behavior more reliably.

### Finished latch

The finished latch allows the test thread to wait until all concurrent requests have completed before checking the final wallet balance and transaction count.

---

## 5. Where did the AI assistant give me an incorrect or sub-optimal suggestion?

During the development of the concurrency test, an initial test implementation expected `MockMvc.perform()` to always return an HTTP `409 Conflict` response when there were insufficient funds.

However, during the concurrent integration test, the `InsufficientFundsException` was propagated through the Spring MVC/servlet layer instead of being returned directly as a `409` response in the `MockMvc` result.

This caused some worker threads to receive a `ServletException` containing the `InsufficientFundsException`.

The business logic itself was working correctly: the requests were being rejected because the wallet did not have enough funds.

I adjusted the test to inspect the exception cause chain and count `InsufficientFundsException` as an insufficient-funds request.

The final race-condition test produced:

```text
Requests Sent         : 10
Successful Requests   : 5
Insufficient Funds    : 5
Transactions Stored   : 5
Final Balance         : ₹0.00
```

This was a useful lesson that integration tests should account for how exceptions are actually propagated through the testing framework rather than assuming every application exception will always appear as an HTTP response status.

---

## 6. Test Coverage

The required integration tests are:

### Test 1 — Happy Path

**Processes a single valid debit transaction successfully.**

The test verifies that:

* The request succeeds.
* ₹100 is deducted from the wallet.
* The final balance is ₹400.
* One transaction is stored.

### Test 2 — Idempotency

**Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.**

The test verifies that:

* Three identical requests are received.
* Only one transaction is stored.
* The wallet is deducted only once.
* The final balance is ₹400.

### Test 3 — Race Condition

**Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.**

The test verifies that:

* 10 requests are processed concurrently.
* 5 requests succeed.
* 5 requests fail due to insufficient funds.
* 5 transactions are stored.
* The final balance is exactly ₹0.
* The wallet never becomes negative.

---

## 7. Final Decision

The implementation uses:

* **Spring Boot** for the REST API.
* **H2** for zero-config integration testing.
* **JPA/Hibernate** for database operations.
* **Pessimistic database locking** to protect concurrent wallet updates.
* **Unique transaction IDs** to provide idempotency.
* **JUnit 5 and MockMvc** for integration testing.
* **CountDownLatch and ExecutorService** to simulate concurrent requests.

The three required integration tests verify the happy path, idempotency, and concurrent debit/race-condition behavior.
