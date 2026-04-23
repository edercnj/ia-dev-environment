# ASVS Verification Items: Chapters V1-V7

## V1: Architecture, Design and Threat Modeling

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V1.1.1 | Verify the use of a secure software development lifecycle that addresses security in all stages of development | L2 | CWE-1053 |
| V1.1.2 | Verify the use of threat modeling for every design change or sprint planning to identify threats and select countermeasures | L2 | CWE-1053 |
| V1.1.3 | Verify that all user stories and features contain functional security constraints | L2 | CWE-1110 |
| V1.1.4 | Verify documentation and justification of all trust boundaries, components, and significant data flows | L2 | CWE-1059 |
| V1.1.5 | Verify definition and security analysis of the application high-level architecture and all connected remote services | L2 | CWE-1059 |
| V1.1.6 | Verify implementation of centralized, simple, vetted, secure, and reusable security controls | L2 | CWE-637 |
| V1.1.7 | Verify availability of a secure coding checklist, security requirements, guideline, or policy to all developers and testers | L2 | CWE-637 |
| V1.2.1 | Verify the use of unique or special low-privilege operating system accounts for all application components and services | L2 | CWE-250 |
| V1.2.2 | Verify that communications between application components are authenticated using the least privilege necessary | L2 | CWE-306 |
| V1.2.3 | Verify that the application uses a single vetted authentication mechanism known to be secure and extensible | L2 | CWE-306 |
| V1.2.4 | Verify that all authentication pathways and identity management APIs implement consistent authentication security controls | L2 | CWE-306 |
| V1.4.1 | Verify that trusted enforcement points such as access control gateways and servers enforce access controls | L2 | CWE-602 |
| V1.4.2 | Verify that the chosen access control solution is flexible enough to meet the application needs | L2 | CWE-284 |
| V1.4.3 | Verify enforcement of the principle of least privilege in functions, data files, URLs, controllers, and services | L2 | CWE-272 |
| V1.5.1 | Verify that input and output requirements clearly define how to handle and process data based on type and content | L2 | CWE-1029 |
| V1.5.2 | Verify that serialization is not used when communicating with untrusted clients | L2 | CWE-502 |
| V1.5.3 | Verify that input validation is enforced on a trusted service layer | L2 | CWE-602 |
| V1.5.4 | Verify that output encoding occurs close to or by the interpreter for which it is intended | L2 | CWE-116 |
| V1.6.1 | Verify that there is an explicit policy for management of cryptographic keys and that a cryptographic key lifecycle follows a key management standard | L2 | CWE-320 |
| V1.6.2 | Verify that consumers of cryptographic services protect key material and other secrets by using key vaults or API-based alternatives | L2 | CWE-320 |
| V1.7.1 | Verify that a common logging format and approach is used across the system | L2 | CWE-1009 |
| V1.7.2 | Verify that logs are securely transmitted to a preferably remote system for analysis, detection, alerting, and escalation | L2 | CWE-778 |
| V1.8.1 | Verify that all sensitive data is identified and classified into protection levels | L2 | CWE-213 |
| V1.8.2 | Verify that all protection levels have an associated set of protection requirements (encryption, integrity, retention, privacy) | L2 | CWE-213 |
| V1.9.1 | Verify that TLS is used for all connections and does not fall back to insecure or unencrypted communications | L2 | CWE-319 |
| V1.10.1 | Verify that a source code control system is in use with procedures to ensure check-ins are accompanied by issues or change tickets | L2 | CWE-284 |
| V1.11.1 | Verify the definition and documentation of all application components in terms of business or security functions they provide | L2 | CWE-1059 |
| V1.11.2 | Verify that all high-value business logic flows have abuse cases and misuse cases including threat actors | L2 | CWE-1053 |
| V1.12.1 | Verify that user-uploaded files are stored outside the web root | L2 | CWE-552 |
| V1.12.2 | Verify that user-uploaded files if displayed or downloaded are served by octet stream downloads or from an unrelated domain | L2 | CWE-646 |
| V1.14.1 | Verify the segregation of components of differing trust levels through well-defined security controls and firewall rules | L2 | CWE-923 |
| V1.14.2 | Verify that binary signatures, trusted connections, and verified endpoints are used to deploy binaries to remote devices | L2 | CWE-494 |

## V2: Authentication

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V2.1.1 | Verify that user set passwords are at least 12 characters in length after combining spaces | L1 | CWE-521 |
| V2.1.2 | Verify that passwords of at least 64 characters are permitted and that passwords of more than 128 characters are denied | L1 | CWE-521 |
| V2.1.3 | Verify that password truncation is not performed and that successive spaces are not coalesced | L1 | CWE-521 |
| V2.1.4 | Verify that any printable Unicode character including language-neutral characters is permitted in passwords | L1 | CWE-521 |
| V2.1.5 | Verify users can change their password | L1 | CWE-620 |
| V2.1.6 | Verify that password change functionality requires the user current and new password | L1 | CWE-620 |
| V2.1.7 | Verify that passwords submitted during account registration, login, and password change are checked against a set of breached passwords | L1 | CWE-521 |
| V2.1.8 | Verify that a password strength meter is provided to help users set a stronger password | L1 | CWE-521 |
| V2.1.9 | Verify that there are no password composition rules limiting the type of characters permitted | L1 | CWE-521 |
| V2.1.10 | Verify that there are no periodic credential rotation or password history requirements | L1 | CWE-263 |
| V2.1.11 | Verify that paste functionality, browser password helpers, and external password managers are permitted | L1 | CWE-521 |
| V2.1.12 | Verify that the user can choose to either temporarily view the entire masked password or temporarily view the last typed character of the password | L1 | CWE-521 |
| V2.2.1 | Verify that anti-automation controls are effective at mitigating breached credential testing, brute force, and account lockout attacks | L1 | CWE-307 |
| V2.2.2 | Verify that the use of weak authenticators such as SMS and email is limited to secondary verification and not as a replacement for more secure authentication methods | L2 | CWE-304 |
| V2.2.3 | Verify that secure notifications are sent to users after updates to authentication details such as credential resets or email/address changes | L1 | CWE-620 |
| V2.3.1 | Verify that system-generated initial passwords or activation codes are securely randomly generated and at least 6 characters long and may contain letters and numbers and expire after a short period of time | L1 | CWE-330 |
| V2.4.1 | Verify that passwords are stored in a form that is resistant to offline attacks using approved one-way key derivation or password hashing function | L2 | CWE-916 |
| V2.5.1 | Verify that a system-generated initial activation or recovery secret is not sent in clear text to the user | L1 | CWE-640 |
| V2.5.2 | Verify password hints or knowledge-based authentication are not present | L1 | CWE-640 |
| V2.5.3 | Verify password credential recovery does not reveal the current password in any way | L1 | CWE-640 |
| V2.5.4 | Verify shared or default accounts are not present | L2 | CWE-16 |
| V2.7.1 | Verify that clear text out-of-band (OOTB) authenticators such as SMS or PSTN are not offered by default and that stronger alternatives are offered first | L1 | CWE-287 |
| V2.7.2 | Verify that the out-of-band verifier expires requests, codes, or tokens after 10 minutes | L1 | CWE-287 |
| V2.7.3 | Verify that the out-of-band verifier authentication requests, codes, or tokens are only usable once and only for the original authentication request | L1 | CWE-287 |
| V2.8.1 | Verify that time-based OTPs have a defined lifetime before expiring | L1 | CWE-613 |
| V2.9.1 | Verify that cryptographic keys used in verification are stored securely and protected against disclosure | L2 | CWE-320 |
| V2.10.1 | Verify that intra-service secrets do not rely on unchanging credentials such as passwords, API keys, or shared accounts with privileged access | L2 | CWE-287 |
| V2.10.2 | Verify that if passwords are required for service authentication, the service account used is not a default credential | L2 | CWE-255 |

## V3: Session Management

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V3.1.1 | Verify the application never reveals session tokens in URL parameters or error messages | L1 | CWE-598 |
| V3.2.1 | Verify the application generates a new session token on user authentication | L1 | CWE-384 |
| V3.2.2 | Verify that session tokens possess at least 64 bits of entropy | L1 | CWE-331 |
| V3.2.3 | Verify the application only stores session tokens in the browser using secure methods such as secured cookies | L1 | CWE-539 |
| V3.2.4 | Verify that session tokens are generated using approved cryptographic algorithms | L1 | CWE-331 |
| V3.3.1 | Verify that logout and expiration invalidate the session token | L1 | CWE-613 |
| V3.3.2 | Verify that if authenticators permit users to remain logged in, both re-authentication occurs periodically and active sessions are terminated after inactivity period | L1 | CWE-613 |
| V3.3.3 | Verify that the application gives the option to terminate all other active sessions after a successful password change | L2 | CWE-613 |
| V3.4.1 | Verify that cookie-based session tokens have the Secure attribute set | L1 | CWE-614 |
| V3.4.2 | Verify that cookie-based session tokens have the HttpOnly attribute set | L1 | CWE-1004 |
| V3.4.3 | Verify that cookie-based session tokens utilize the SameSite attribute to limit exposure to cross-site request forgery attacks | L1 | CWE-1275 |
| V3.4.4 | Verify that cookie-based session tokens use the __Host- prefix | L1 | CWE-16 |
| V3.4.5 | Verify that if published under a domain name with other applications that set or use cookies, the application sets the path attribute using the most precise path possible | L1 | CWE-16 |
| V3.5.1 | Verify the application allows users to revoke OAuth tokens that form trust relationships with linked applications | L2 | CWE-290 |
| V3.7.1 | Verify the application ensures a valid login session or requires re-authentication or secondary verification before allowing any sensitive transactions or account modifications | L1 | CWE-778 |

## V4: Access Control

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V4.1.1 | Verify that the application enforces access control rules on a trusted service layer, especially if client-side access control is present and could be bypassed | L1 | CWE-285 |
| V4.1.2 | Verify that all user and data attributes and policy information used by access controls cannot be manipulated by end users unless specifically authorized | L1 | CWE-639 |
| V4.1.3 | Verify that the principle of least privilege exists: users should only be able to access functions, data files, URLs, controllers, services, and other resources for which they possess specific authorization | L1 | CWE-285 |
| V4.1.4 | Verify that the principle of deny by default exists whereby new users/roles start with minimal or no permissions and users/roles do not receive access to new features until access is explicitly assigned | L1 | CWE-276 |
| V4.1.5 | Verify that access controls fail securely including when an exception occurs | L1 | CWE-285 |
| V4.2.1 | Verify that sensitive data and APIs are protected against Insecure Direct Object Reference (IDOR) attacks targeting creation, reading, updating, and deletion of records | L1 | CWE-639 |
| V4.2.2 | Verify that the application or framework enforces a strong anti-CSRF mechanism to protect authenticated functionality | L1 | CWE-352 |
| V4.3.1 | Verify administrative interfaces use appropriate multi-factor authentication to prevent unauthorized use | L1 | CWE-419 |
| V4.3.2 | Verify that directory browsing is disabled unless deliberately desired | L1 | CWE-548 |
| V4.3.3 | Verify the application has additional authorization (step up or adaptive authentication) for lower-value systems and segregation of duties for high-value applications | L2 | CWE-732 |

## V5: Validation, Sanitization and Encoding

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V5.1.1 | Verify that the application has defenses against HTTP parameter pollution attacks, particularly if the application framework makes no distinction about the source of request parameters | L1 | CWE-235 |
| V5.1.2 | Verify that frameworks protect against mass parameter assignment attacks, or the application has countermeasures to protect against unsafe parameter assignment | L1 | CWE-915 |
| V5.1.3 | Verify that all input is validated using positive validation (allow lists) | L1 | CWE-20 |
| V5.1.4 | Verify that structured data is strongly typed and validated against a defined schema including allowed characters, length and pattern | L1 | CWE-20 |
| V5.1.5 | Verify that URL redirects and forwards only allow destinations which appear on an allow list or show a warning when redirecting to potentially untrusted content | L1 | CWE-601 |
| V5.2.1 | Verify that all untrusted HTML input from WYSIWYG editors or similar is properly sanitized with an HTML sanitizer library or framework feature | L1 | CWE-116 |
| V5.2.2 | Verify that unstructured data is sanitized to enforce safety measures such as allowed characters and length | L1 | CWE-138 |
| V5.2.3 | Verify that the application sanitizes user input before passing to mail systems to protect against SMTP or IMAP injection | L1 | CWE-147 |
| V5.2.4 | Verify that the application avoids the use of eval() or other dynamic code execution features | L1 | CWE-95 |
| V5.2.5 | Verify that the application protects against template injection attacks by ensuring that any user input being included is sanitized or sandboxed | L1 | CWE-94 |
| V5.2.6 | Verify that the application protects against SSRF attacks by validating or sanitizing untrusted data or HTTP file metadata | L1 | CWE-918 |
| V5.2.7 | Verify that the application sanitizes, disables, or sandboxes user-supplied SVG scriptable content, especially as they relate to XSS resulting from inline scripts and foreignObject | L1 | CWE-159 |
| V5.2.8 | Verify that the application sanitizes, disables, or sandboxes user-supplied scriptable or expression template language content such as Markdown, CSS or XSL stylesheets, BBCode, or similar | L1 | CWE-94 |
| V5.3.1 | Verify that output encoding is relevant for the interpreter and context required | L1 | CWE-116 |
| V5.3.2 | Verify that output encoding preserves the user chosen character set and locale | L1 | CWE-176 |
| V5.3.3 | Verify that context-aware, preferably automated output escaping protects against reflected, stored, and DOM-based XSS | L1 | CWE-79 |
| V5.3.4 | Verify that data selection or database queries (SQL, HQL, ORM, NoSQL) use parameterized queries, ORMs, entity frameworks, or are otherwise protected against database injection attacks | L1 | CWE-89 |
| V5.3.5 | Verify that where parameterized or safer mechanisms are not present, context-specific output encoding is used to protect against injection attacks | L1 | CWE-89 |
| V5.3.6 | Verify that the application protects against JSON injection attacks, JSON eval attacks, and JavaScript expression evaluation | L1 | CWE-830 |
| V5.3.7 | Verify that the application protects against LDAP injection vulnerabilities | L1 | CWE-90 |
| V5.3.8 | Verify that the application protects against OS command injection and that operating system calls use parameterized OS queries or use contextual command-line output encoding | L1 | CWE-78 |
| V5.3.9 | Verify that the application protects against Local File Inclusion (LFI) or Remote File Inclusion (RFI) attacks | L1 | CWE-829 |
| V5.3.10 | Verify that the application protects against XPath injection or XML injection attacks | L1 | CWE-643 |
| V5.5.1 | Verify that serialized objects use integrity checks or are encrypted to prevent hostile object creation or data tampering | L1 | CWE-502 |
| V5.5.2 | Verify that the application correctly restricts XML parsers to only use the most restrictive configuration possible and to ensure that unsafe features such as resolving external entities are disabled | L1 | CWE-611 |
| V5.5.3 | Verify that deserialization of untrusted data is avoided or is protected in both custom code and third-party libraries | L1 | CWE-502 |
| V5.5.4 | Verify that when parsing JSON in browsers or JavaScript-based backends, JSON.parse is used to parse the JSON document | L1 | CWE-95 |

## V6: Stored Cryptography

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V6.1.1 | Verify that regulated private data is stored encrypted while at rest, such as Personally Identifiable Information (PII), sensitive personal information, or data assessed likely to be subject to EU GDPR | L2 | CWE-311 |
| V6.1.2 | Verify that regulated health data is stored encrypted while at rest | L2 | CWE-311 |
| V6.1.3 | Verify that regulated financial data is stored encrypted while at rest | L2 | CWE-311 |
| V6.2.1 | Verify that all cryptographic modules fail securely, and errors are handled in a way that does not enable Padding Oracle attacks | L1 | CWE-310 |
| V6.2.2 | Verify that industry-proven or government-approved cryptographic algorithms, modes, and libraries are used, instead of custom coded cryptography | L1 | CWE-327 |
| V6.2.3 | Verify that encryption initialization vector, cipher configuration, and block modes are configured securely using the latest advice | L1 | CWE-326 |
| V6.2.4 | Verify that random number, encryption or hashing algorithms, key lengths, rounds, ciphers or modes, can be reconfigured, upgraded, or swapped at any time to protect against cryptographic breaks | L1 | CWE-326 |
| V6.2.5 | Verify that known insecure block modes (ECB), padding modes (PKCS#1 v1.5), ciphers with small block sizes (Triple-DES, Blowfish), and weak hashing algorithms (MD5, SHA1) are not used | L1 | CWE-326 |
| V6.2.6 | Verify that nonces, initialization vectors, and other single-use numbers are not used for more than one encryption key/data combination | L2 | CWE-326 |
| V6.2.7 | Verify that encrypted data is authenticated via signatures, authenticated cipher modes, or HMAC to ensure that ciphertext is not altered by an unauthorized party | L2 | CWE-326 |
| V6.2.8 | Verify that all cryptographic operations are constant-time, with no short-circuit operations in comparisons, calculations, or returns, to avoid leaking information | L3 | CWE-385 |
| V6.3.1 | Verify that all random numbers, random file names, random GUIDs, and random strings are generated using the cryptographic module approved cryptographically secure pseudo-random number generator | L2 | CWE-338 |
| V6.3.2 | Verify that random GUIDs are created using the GUID v4 algorithm and a CSPRNG | L2 | CWE-338 |
| V6.4.1 | Verify that a secrets management solution such as a key vault is used to securely create, store, control access to, and destroy secrets | L2 | CWE-798 |
| V6.4.2 | Verify that key material is not exposed to the application but instead uses an isolated security module for cryptographic operations | L2 | CWE-320 |

## V7: Error Handling and Logging

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V7.1.1 | Verify that the application does not log credentials or payment details. Session tokens should only be stored in logs in an irreversible, hashed form | L1 | CWE-532 |
| V7.1.2 | Verify that the application does not log other sensitive data as defined under local privacy laws or relevant security policy | L1 | CWE-532 |
| V7.1.3 | Verify that the application logs security-relevant events including successful and failed authentication events, access control failures, deserialization failures, and input validation failures | L2 | CWE-778 |
| V7.1.4 | Verify that each log event includes necessary information that would allow for a detailed investigation of the timeline when an event happens | L2 | CWE-778 |
| V7.2.1 | Verify that all authentication decisions are logged, without storing sensitive session tokens or passwords. This should include requests with relevant metadata needed for security investigations | L2 | CWE-778 |
| V7.2.2 | Verify that all access control decisions can be logged and all failed decisions are logged. This should include requests with relevant metadata | L2 | CWE-285 |
| V7.3.1 | Verify that the application appropriately encodes user-supplied data to prevent log injection | L2 | CWE-117 |
| V7.3.2 | Verify that all events are protected from injection when viewed in log-viewing software | L2 | CWE-117 |
| V7.3.3 | Verify that security logs are protected from unauthorized access and modification | L2 | CWE-200 |
| V7.3.4 | Verify that time sources are synchronized to the correct time and time zone, strongly considering logging only in UTC | L2 | CWE-210 |
| V7.4.1 | Verify that a generic message is shown when an unexpected or security-sensitive error occurs, potentially with a unique ID which support personnel can use to investigate | L1 | CWE-210 |
| V7.4.2 | Verify that exception handling is used across the codebase to account for expected and unexpected error conditions | L2 | CWE-544 |
| V7.4.3 | Verify that a last resort error handler is defined which will catch all unhandled exceptions | L2 | CWE-431 |
