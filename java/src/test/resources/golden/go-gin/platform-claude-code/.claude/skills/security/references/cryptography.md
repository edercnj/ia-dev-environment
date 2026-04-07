# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Cryptography

## Encryption in Transit

### TLS Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| Protocol version | TLS 1.2 | **TLS 1.3** |
| Certificate type | RSA 2048-bit | ECDSA P-256 or RSA 4096-bit |
| Certificate validity | 1 year max | 90 days (auto-renewed via ACME) |
| OCSP stapling | Recommended | **Required** |

### Disabled Protocols and Features

- **Disabled protocols:** SSL 2.0, SSL 3.0, TLS 1.0, TLS 1.1
- **Disabled features:** Compression (CRIME attack), renegotiation (unless secure), 0-RTT replay-vulnerable data
- **Disabled cipher suites:** NULL, EXPORT, DES, RC4, 3DES, MD5-based MACs

### TLS 1.3 Configuration per {{FRAMEWORK}}

Configure TLS 1.3 as the minimum protocol version in your {{FRAMEWORK}} application.
All {{LANGUAGE}} services MUST enforce TLS 1.3 for external traffic and TLS 1.2+ for internal service-to-service communication.

```
// {{FRAMEWORK}} TLS configuration pattern ({{LANGUAGE}})
// 1. Set minimum TLS version to 1.3
// 2. Configure approved cipher suites only
// 3. Disable client-initiated renegotiation
// 4. Enable OCSP stapling for certificate validation

server:
    ssl:
        enabled: true
        protocol: TLS
        enabled-protocols: TLSv1.3
        ciphers:
            - TLS_AES_256_GCM_SHA384
            - TLS_AES_128_GCM_SHA256
            - TLS_CHACHA20_POLY1305_SHA256
        key-store: classpath:keystore.p12
        key-store-type: PKCS12
```

**Environment-specific configuration:**
- **Development:** Self-signed certificates are acceptable; generate via `keytool` or `openssl`
- **Staging:** Use internal CA-signed certificates; validate certificate chain
- **Production:** Use publicly trusted CA-signed certificates (ACME/Let's Encrypt preferred); enforce OCSP stapling; monitor expiration alerts at 30, 14, and 7 days

### Approved TLS 1.3 Cipher Suites

```
TLS_AES_256_GCM_SHA384
TLS_AES_128_GCM_SHA256
TLS_CHACHA20_POLY1305_SHA256
```

### Approved TLS 1.2 Cipher Suites (when 1.3 not available)

```
TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256
TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256
```

### Certificate Management

- Automate certificate issuance and renewal (cert-manager, ACME, Let's Encrypt)
- Store private keys in secrets manager or HSM; never in source code
- Monitor certificate expiration; alert at 30, 14, and 7 days before expiry
- Implement certificate pinning only for mobile apps (with backup pins and rotation plan)
- Use separate certificates per environment (dev, staging, production)

### Mutual TLS (mTLS)

- Use mTLS for service-to-service communication in zero-trust networks
- Issue short-lived client certificates (24h-7d) via service mesh or internal CA
- Validate both client and server certificate chains
- Implement certificate rotation without downtime (dual certificate support)

```
// mTLS configuration pattern for {{FRAMEWORK}} ({{LANGUAGE}})
tlsConfig:
    minVersion: TLS_1_3
    clientAuth: RequireAndVerifyClientCert
    clientCAs: /etc/ssl/internal-ca.pem
    certificates:
        certFile: /etc/ssl/service.crt
        keyFile: /etc/ssl/service.key
```

## Cipher Suite Selection

### Selection Table

| Category | Recommended | Acceptable | Deprecated |
|----------|-------------|------------|------------|
| Key Exchange | ECDHE, X25519 | DHE (2048+) | RSA key exchange, DH < 2048 |
| Symmetric Encryption | AES-256-GCM, ChaCha20-Poly1305 | AES-128-GCM | 3DES, RC4, DES |
| Hash / MAC | SHA-256, SHA-384, SHA-512 | SHA-3 | MD5, SHA-1 |
| Signature | ECDSA P-256+, Ed25519, RSA-PSS 2048+ | RSA PKCS#1 v1.5 2048+ | RSA < 2048, DSA |

**Rules:**
- **Recommended:** Use for all new {{LANGUAGE}} implementations
- **Acceptable:** Permitted for backward compatibility; plan migration to Recommended
- **Deprecated:** FORBIDDEN in any new or existing {{FRAMEWORK}} configuration; remove immediately

### {{FRAMEWORK}} Cipher Suite Configuration

```
// Configure cipher suites in {{FRAMEWORK}} ({{LANGUAGE}})
// Only include Recommended and Acceptable suites
// NEVER include Deprecated algorithms

allowedCipherSuites:
    # Recommended (TLS 1.3)
    - TLS_AES_256_GCM_SHA384
    - TLS_CHACHA20_POLY1305_SHA256
    - TLS_AES_128_GCM_SHA256
    # Acceptable (TLS 1.2 fallback)
    - TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
    - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
```

## Encryption at Rest

### Approaches

| Approach | Scope | Use When |
|----------|-------|----------|
| Transparent Data Encryption (TDE) | Full database/disk | Default for all persistent storage |
| Application-level encryption | Specific fields/columns | Sensitive fields (PII, financial data) |
| File-level encryption | Individual files | Backups, exports, file storage |

### Requirements

- **Algorithm:** AES-256-GCM (authenticated encryption) for all new implementations
- **Key management:** Use Customer-Managed Keys (CMK) via KMS; never embed keys in application code
- **Backup encryption:** ALL backups must be encrypted with separate keys from primary data
- **Key hierarchy:** Use envelope encryption (data key encrypted by master key)

### Envelope Encryption Pattern

```
// Envelope encryption in {{LANGUAGE}} ({{FRAMEWORK}})
function encryptData(plaintext):
    // 1. Generate a unique Data Encryption Key (DEK)
    dek = kms.generateDataKey(masterKeyId)

    // 2. Encrypt the plaintext with the DEK
    ciphertext = aes256gcm.encrypt(plaintext, dek.plaintext)

    // 3. Store encrypted DEK alongside ciphertext
    return {
        encryptedData: ciphertext,
        encryptedDek: dek.ciphertext,  // DEK encrypted by master key
        masterKeyId: masterKeyId,
        algorithm: "AES-256-GCM",
        iv: ciphertext.iv
    }

// Decrypt data
function decryptData(encryptedPayload):
    // 1. Decrypt the DEK using the master key
    dek = kms.decrypt(encryptedPayload.encryptedDek, encryptedPayload.masterKeyId)

    // 2. Decrypt the data using the plaintext DEK
    plaintext = aes256gcm.decrypt(encryptedPayload.encryptedData, dek)
    return plaintext
```

### Application-Level Field Encryption

```
// Field-level encryption in {{LANGUAGE}} ({{FRAMEWORK}})
@Entity
class Customer:
    id: UUID
    name: String                           // Not encrypted (INTERNAL classification)

    @Encrypted(algorithm="AES-256-GCM")
    email: String                          // Encrypted at rest (RESTRICTED)

    @Encrypted(algorithm="AES-256-GCM")
    taxId: String                          // Encrypted at rest (RESTRICTED)

    @Encrypted(algorithm="AES-256-GCM")
    phoneNumber: String                    // Encrypted at rest (RESTRICTED)
```

## Hashing

### Hashing Algorithm Selection

| Use Case | Algorithm | Parameters | NEVER Use |
|----------|-----------|-----------|-----------|
| Password hashing | **Argon2id** (preferred), bcrypt (fallback) | Argon2id: Memory 64MB, Iterations 3, Parallelism 4; bcrypt: cost >= 12 | MD5, SHA-1, SHA-256 without salt, plain text |
| Data integrity | **SHA-256**, SHA-3 (SHA3-256) | Default parameters | MD5, CRC32 |
| HMAC | **HMAC-SHA-256**, HMAC-SHA-512 | Key length >= 256 bits | HMAC-MD5 |
| Token generation | **CSPRNG** + Base64 | Minimum 128-bit entropy (32 hex chars) | Math.random(), UUID v4, predictable seeds |
| High-performance integrity | BLAKE3 | Default parameters | MD5, Adler32 |

### Password Hashing

| Algorithm | Parameters | Use Case |
|-----------|-----------|----------|
| **argon2id** (preferred) | Memory: 64MB, Iterations: 3, Parallelism: 4 | Password storage |
| **bcrypt** (acceptable) | Cost factor: >= 12 | Password storage (legacy systems) |
| **scrypt** (acceptable) | N: 2^17, r: 8, p: 1 | Password storage (alternative) |

**FORBIDDEN for passwords:** MD5, SHA-1, SHA-256 (unsalted), plain text

```
// Password hashing in {{LANGUAGE}} ({{FRAMEWORK}})
// GOOD -- Use Argon2id with secure parameters
hashedPassword = argon2id.hash(password, {
    memory: 65536,    // 64 MB
    iterations: 3,
    parallelism: 4,
    hashLength: 32
})

// GOOD -- Verify password
isValid = argon2id.verify(hashedPassword, candidatePassword)

// BAD -- NEVER use fast hashing for passwords
hashedPassword = sha256(password)           // FORBIDDEN
hashedPassword = md5(password + salt)       // FORBIDDEN
```

### Integrity Hashing

| Algorithm | Use Case |
|-----------|----------|
| SHA-256 | File integrity, checksums, content addressing |
| SHA-3 (SHA3-256) | New implementations requiring NIST standard |
| BLAKE3 | High-performance integrity verification |

### HMAC (Message Authentication)

- Use **HMAC-SHA256** for message authentication and API request signing
- Key length must be at least 256 bits
- Use constant-time comparison to prevent timing attacks

```
// HMAC verification in {{LANGUAGE}} ({{FRAMEWORK}})
// GOOD -- Constant-time HMAC verification
function verifyWebhook(payload, signature, secret):
    expected = hmacSha256(payload, secret)
    return constantTimeEquals(expected, signature)  // Timing-safe comparison

// BAD -- Timing-vulnerable comparison
function verifyWebhook(payload, signature, secret):
    expected = hmacSha256(payload, secret)
    return expected == signature  // Vulnerable to timing attack
```

### Token Generation

- Use **CSPRNG** (Cryptographically Secure Pseudo-Random Number Generator) for all token generation
- Minimum token entropy: 128 bits (32 hex chars or 22 base64 chars)
- Never use `Math.random()`, `rand()`, or similar non-cryptographic PRNGs for security tokens

```
// Token generation in {{LANGUAGE}} ({{FRAMEWORK}})
// GOOD -- CSPRNG token generation
token = cryptoSecureRandom(32)  // 256-bit token
sessionId = base64UrlEncode(cryptoSecureRandom(32))

// BAD -- Predictable token
token = md5(timestamp + username)
sessionId = base64(Math.random().toString())
```

## Key Management

### Key Management Patterns

| Pattern | Description | Use When |
|---------|-------------|----------|
| KMS / Envelope Encryption | Master key in KMS encrypts data keys; data keys encrypt data | Default for all encryption at rest |
| Key Rotation | Automated periodic key replacement with grace period | All key types (see rotation table below) |
| Key Derivation (HKDF) | Derive multiple keys from a single master secret | Session keys, per-record keys |
| Key Derivation (PBKDF2) | Derive encryption key from password | Encrypting user-owned data with user password |
| Secrets Manager | Centralized storage for credentials and keys | All secrets (DB passwords, API keys, tokens) |

### Centralized Key Management

- Use a centralized key management system: **HashiCorp Vault**, **AWS KMS**, **Azure Key Vault**, **GCP Cloud KMS**
- Never store encryption keys alongside encrypted data
- Never embed keys in application source code or configuration files
- All key operations must be audited

### Key Rotation Policy

| Key Type | Rotation Frequency | Versioning |
|----------|-------------------|------------|
| Data encryption keys | 90 days | Mandatory -- keep old versions to decrypt existing data |
| Signing keys | 365 days | Mandatory -- old key verifies existing signatures |
| TLS certificates | 90 days | Auto-renewed via ACME |
| API signing keys | 180 days | Versioned (key ID in header) |
| Master keys (KMS) | Annual review | Managed by KMS provider |

### Key Derivation

```
// Key derivation in {{LANGUAGE}} ({{FRAMEWORK}})
// HKDF -- Derive per-record encryption key from master secret
function deriveKey(masterSecret, context, keyLength):
    salt = cryptoSecureRandom(32)
    prk = hkdfExtract(salt, masterSecret)
    derivedKey = hkdfExpand(prk, context, keyLength)
    return { derivedKey, salt }

// PBKDF2 -- Derive encryption key from user password
function deriveKeyFromPassword(password, salt):
    return pbkdf2(password, salt, {
        iterations: 600000,     // NIST SP 800-132 minimum
        hashFunction: "SHA-256",
        keyLength: 256
    })
```

### Key Versioning

```
// Key versioning in {{LANGUAGE}} ({{FRAMEWORK}})
function decrypt(ciphertext, metadata):
    keyVersion = metadata.keyVersion
    key = keyStore.getKey(metadata.keyId, keyVersion)
    return aes256gcm.decrypt(ciphertext, key)

// Always encrypt with latest version
function encrypt(plaintext, keyId):
    latestKey = keyStore.getLatestKey(keyId)
    ciphertext = aes256gcm.encrypt(plaintext, latestKey.material)
    return {
        ciphertext: ciphertext,
        keyId: keyId,
        keyVersion: latestKey.version
    }
```

### Secrets Manager Integration

```
// Secrets manager integration in {{LANGUAGE}} ({{FRAMEWORK}})
// Fetch secrets at startup, cache with TTL, auto-refresh
function getSecret(secretName):
    cached = secretCache.get(secretName)
    if cached != null AND NOT cached.isExpired():
        return cached.value

    secret = secretsManager.getSecretValue(secretName)
    secretCache.put(secretName, secret, ttl=300)  // 5-min TTL
    return secret.value
```

### Separation of Duties

- Key administrators cannot use keys for encryption/decryption
- Application operators cannot create or manage keys
- Audit team has read-only access to key usage logs
- Emergency revocation requires multi-party approval (M-of-N)

### HSM (Hardware Security Module)

- Use HSM for master key storage in high-security environments (PCI-DSS, financial)
- HSM-backed keys never leave the hardware boundary
- FIPS 140-2 Level 3 minimum for production master keys

### Emergency Revocation

```
// Emergency key revocation in {{LANGUAGE}} ({{FRAMEWORK}})
function emergencyRevoke(keyId, reason):
    // 1. Disable key immediately
    keyStore.disableKey(keyId)

    // 2. Log revocation event
    auditLog.critical("KEY_REVOKED", {keyId, reason, revokedBy, timestamp})

    // 3. Alert security team
    alerting.sendCritical("Encryption key revoked: " + keyId)

    // 4. Trigger re-encryption with new key (async)
    reEncryptionService.scheduleReEncryption(keyId)
```

## Digital Signatures

### API Request Signing

- Sign API requests using HMAC-SHA256 or RSA/ECDSA for non-repudiation
- Include timestamp in signed payload to prevent replay attacks
- Reject requests with timestamps older than 5 minutes

```
// API request signing in {{LANGUAGE}} ({{FRAMEWORK}})
function signRequest(method, path, body, timestamp, secretKey):
    payload = method + "\n" + path + "\n" + timestamp + "\n" + sha256(body)
    signature = hmacSha256(payload, secretKey)
    return base64Encode(signature)

// Headers
X-Timestamp: 1700000000
X-Signature: base64(hmac-sha256(payload, key))
X-Key-Id: key-version-identifier
```

### JWT Signing

| Algorithm | Key Type | Use When |
|-----------|----------|----------|
| **RS256** | RSA 2048+ | Widely supported, asymmetric verification needed |
| **ES256** | ECDSA P-256 | Smaller tokens, modern systems |
| **EdDSA** | Ed25519 | Highest performance, modern systems |

**FORBIDDEN JWT algorithms:** `none`, HS256 with public key confusion, RSA with key < 2048 bits

```
// JWT signing in {{LANGUAGE}} ({{FRAMEWORK}})
jwt.create({
    issuer: "auth-service",
    audience: "api-service",
    subject: userId,
    expiration: now() + 15.minutes,       // Short-lived access tokens
    notBefore: now(),
    jwtId: generateUUID(),                 // Unique token ID for revocation
    algorithm: "ES256",
    key: signingPrivateKey
})
```

### Code Signing

- Sign all release artifacts (binaries, container images, packages)
- Verify signatures in CI/CD pipelines before deployment
- Use cosign/sigstore for container image signing
- Store signing keys in HSM or vault

### Webhook Signatures

- Sign all outgoing webhooks with HMAC-SHA256
- Include signature in header (`X-Signature-256`)
- Document signature verification for consumers
- Rotate webhook secrets on a regular schedule

## Field-Level Encryption and Tokenization

### Field-Level Encryption

Use field-level encryption for sensitive data columns that require application-layer protection beyond TDE.

**Deterministic vs Randomized Encryption:**

| Mode | Properties | Use When |
|------|-----------|----------|
| Deterministic | Same plaintext produces same ciphertext; allows equality queries | Indexed lookup fields (email, tax ID) |
| Randomized | Same plaintext produces different ciphertext each time; stronger security | Non-searchable fields (notes, addresses) |

**Trade-offs:**
- Deterministic encryption enables `WHERE email = ?` queries but leaks frequency distribution
- Randomized encryption provides semantic security but prevents server-side search
- Choose based on query requirements and threat model for your {{FRAMEWORK}} application

### Format-Preserving Encryption (FPE)

FPE encrypts data while preserving its original format (length, character set, check digits).

| Data Type | Format Preserved | Algorithm |
|-----------|-----------------|-----------|
| Credit card PAN | 16 digits, Luhn-valid | FF1 (NIST SP 800-38G) |
| Phone number | Country-specific format | FF3-1 |
| SSN / Tax ID | NNN-NN-NNNN format | FF1 |

```
// Format-preserving encryption in {{LANGUAGE}} ({{FRAMEWORK}})
function encryptFpe(plaintext, format, tweak):
    key = keyStore.getLatestKey("fpe-key")
    return ff1.encrypt(key, tweak, plaintext, format.radix)

// Example: PAN encryption preserving format
pan = "4111111111111111"
encryptedPan = encryptFpe(pan, NUMERIC_16, merchantId)
// Result: "9283746501928374" (still 16 digits)
```

### Vault-Based Tokenization

Tokenization replaces sensitive data with non-reversible tokens stored in an isolated vault.
Use for PCI-DSS compliance and data minimization.

**Principles:**
- **Non-reversible tokens:** Tokenization must not be reversible without access to the token vault
- **Isolated vault:** Token-to-value mapping stored in a separate, hardened database
- **Detokenization audit:** Every detokenization operation must be logged with requester identity and purpose
- **Format-preserving:** Tokens maintain the format of the original data (e.g., 16-digit token for a 16-digit PAN)

```
// Vault-based tokenization in {{LANGUAGE}} ({{FRAMEWORK}})
function tokenize(sensitiveValue, format):
    // 1. Generate format-preserving token
    token = tokenVault.generateToken(format)

    // 2. Store mapping in isolated vault
    tokenVault.store(token, encrypt(sensitiveValue))

    // 3. Audit the tokenization event
    auditLog.log("TOKENIZED", {format, requester, timestamp})

    return token

// Detokenize (restricted access)
function detokenize(token, purpose, requester):
    // 1. Verify requester authorization
    if NOT authorized(requester, "DETOKENIZE"):
        throw ForbiddenException("Not authorized to detokenize")

    // 2. Retrieve and decrypt original value
    encryptedValue = tokenVault.retrieve(token)
    originalValue = decrypt(encryptedValue)

    // 3. Audit the detokenization event
    auditLog.log("DETOKENIZED", {token, purpose, requester, timestamp})

    return originalValue
```

### Tokenization Use Cases

| Data Type | Tokenization Format | Example |
|-----------|-------------------|---------|
| PAN (Credit Card) | Format-preserving (16 digits) | `4111111111111111` -> `9832748291038472` |
| SSN | Format-preserving (NNN-NN-NNNN) | `123-45-6789` -> `987-65-4321` |
| Bank Account | Random token with prefix | `ACCT-a8f3b9c2e1d4` |
| Email | Random token | `TOK-7f8a9b3c2d1e` |

## Deprecated/Forbidden Algorithms

| Algorithm | Status | Replacement |
|-----------|--------|-------------|
| MD5 | **FORBIDDEN** | SHA-256 or SHA-3 |
| SHA-1 | **FORBIDDEN** | SHA-256 or SHA-3 |
| DES / 3DES | **FORBIDDEN** | AES-256-GCM |
| RC4 | **FORBIDDEN** | AES-256-GCM or ChaCha20 |
| RSA < 2048 bits | **FORBIDDEN** | RSA 4096 or ECDSA P-256 |
| ECB mode | **FORBIDDEN** | GCM or CBC with HMAC |
| PKCS#1 v1.5 padding | **DEPRECATED** | OAEP (RSA) |
| bcrypt cost < 12 | **FORBIDDEN** | bcrypt cost >= 12 or argon2id |

## Anti-Patterns (FORBIDDEN)

- Use deprecated or broken cryptographic algorithms
- Store encryption keys in source code, config files, or alongside encrypted data
- Use ECB mode for any encryption operation
- Generate security tokens with non-cryptographic PRNGs
- Compare HMAC values using non-constant-time functions
- Implement custom cryptographic algorithms or protocols
- Skip certificate validation in production
- Use the same encryption key across environments
- Store password hashes using fast hash algorithms (SHA-256, MD5)
- Hardcode initialization vectors (IVs) or reuse IVs with the same key
