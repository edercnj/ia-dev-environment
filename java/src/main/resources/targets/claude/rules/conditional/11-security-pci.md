# Rule 11 — PCI-DSS Security Prohibitions

> **Compliance:** PCI-DSS v4.0 — These rules are mandatory for all code handling payment card data.

## Scope

Applies to all source files in modules that process, store, or transmit cardholder data (PAN, CVV, expiration date, track data).

## Prohibitions

### PRH-01: No toString() on Card Data Objects

**Rule:** Never implement or invoke `toString()` on domain objects containing cardholder data.

**Reason (PCI-DSS Req 3.4):** `toString()` output frequently appears in logs, debugger output, and error messages, causing unintended PAN disclosure.

**Prohibited:**
```java
public class CardInfo {
    private String pan;
    private String cvv;

    @Override
    public String toString() {
        return "CardInfo{pan=" + pan + ", cvv=" + cvv + "}";
    }
}
```

**Correct Alternative:**
```java
public class CardInfo {
    private String pan;
    private String cvv;

    @Override
    public String toString() {
        return "CardInfo{pan=****" + pan.substring(pan.length() - 4) + "}";
    }
}
```

### PRH-02: No Serialization of Financial Domain Objects to Logs

**Rule:** Never serialize domain objects containing card data directly to log output.

**Reason (PCI-DSS Req 3.1):** Serialized objects in logs expose cardholder data in plaintext, violating storage and logging requirements.

**Prohibited:**
```java
log.info("Processing payment: {}", objectMapper.writeValueAsString(paymentRequest));
log.debug("Card details: {}", cardInfo);
```

**Correct Alternative:**
```java
log.info("Processing payment: txnId={}, amount={}", paymentRequest.transactionId(), paymentRequest.amount());
log.debug("Card operation: tokenRef={}", cardInfo.tokenReference());
```

### PRH-03: No PAN Storage Without Encryption

**Rule:** Never store PAN in database fields, files, or caches without column-level or field-level encryption using AES-256 or stronger.

**Reason (PCI-DSS Req 3.5):** PAN stored in plaintext is a critical compliance violation exposing cardholder data at rest.

**Prohibited:**
```java
@Column(name = "card_number")
private String pan;

cache.put("card:" + customerId, pan);
```

**Correct Alternative:**
```java
@Column(name = "card_number_encrypted")
@Convert(converter = AesEncryptedConverter.class)
private String pan;

cache.put("card:" + customerId, encryptionService.encrypt(pan));
```

### PRH-04: No Math.random() for Security Tokens

**Rule:** Never use `Math.random()`, `java.util.Random`, or any non-cryptographic RNG for generating tokens, session identifiers, nonces, or encryption keys.

**Reason (PCI-DSS Req 6.2):** Non-cryptographic RNGs produce predictable values that can be exploited to forge tokens or session identifiers.

**Prohibited:**
```java
String token = String.valueOf(Math.random());
String sessionId = new Random().nextLong() + "";
byte[] key = new byte[32];
new Random().nextBytes(key);
```

**Correct Alternative:**
```java
String token = UUID.randomUUID().toString();
byte[] sessionBytes = new byte[32];
SecureRandom.getInstanceStrong().nextBytes(sessionBytes);
String sessionId = Base64.getUrlEncoder().encodeToString(sessionBytes);
```

### PRH-05: No CVV, Expiry, or Full Card Number in Logs

**Rule:** Never log CVV/CVC, card expiration date, or complete card number in any log level (trace, debug, info, warn, error).

**Reason (PCI-DSS Req 3.2):** Sensitive authentication data and full PAN in logs creates a persistent compliance violation in log storage systems.

**Prohibited:**
```java
log.debug("Validating card: number={}, cvv={}, exp={}", cardNumber, cvv, expiryDate);
log.error("Payment failed for card " + fullCardNumber);
logger.trace("CVV check: {}", cvvValue);
```

**Correct Alternative:**
```java
log.debug("Validating card: last4={}, hasValidCvv={}", maskPan(cardNumber), cvv != null);
log.error("Payment failed for card ending {}", last4Digits(fullCardNumber));
logger.trace("CVV check completed: valid={}", cvvValid);
```

### PRH-06: No Card Data in Query Strings or URLs

**Rule:** Never accept or transmit PAN, CVV, or expiration date as URL query parameters, path variables, or fragment identifiers.

**Reason (PCI-DSS Req 4.2):** URLs are logged by web servers, proxies, and browser history, causing unintended data leakage.

**Prohibited:**
```java
@GetMapping("/cards")
public CardResponse getCard(@RequestParam String pan) { ... }

String url = "/api/payment?cardNumber=" + pan + "&cvv=" + cvv;
```

**Correct Alternative:**
```java
@PostMapping("/cards/lookup")
public CardResponse getCard(@RequestBody CardLookupRequest request) { ... }

// Use POST with encrypted request body for card operations
```

### PRH-07: No Hardcoded Encryption Keys or Secrets

**Rule:** Never embed encryption keys, API keys, database passwords, or any secret value directly in source code.

**Reason (PCI-DSS Req 6.5):** Hardcoded secrets in source code are exposed through version control history and decompiled artifacts.

**Prohibited:**
```java
private static final String AES_KEY = "MySecretKey12345";
private static final String DB_PASSWORD = "prod_password_123";
```

**Correct Alternative:**
```java
@Value("${encryption.key}")
private String aesKey;

// Or via KMS/HSM:
private final KeyManagementService kms;
byte[] key = kms.getKey("payment-encryption-key");
```

## Enforcement

- These prohibitions are checked during code review via the `x-review-compliance` skill
- Violations are classified as **CRITICAL** severity and block merge
- Automated static analysis should flag patterns matching prohibited examples
