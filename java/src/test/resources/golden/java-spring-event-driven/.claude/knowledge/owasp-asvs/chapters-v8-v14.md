# ASVS Verification Items: Chapters V8-V14

## V8: Data Protection

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V8.1.1 | Verify the application protects sensitive data from being cached in server components such as load balancers and application caches | L2 | CWE-524 |
| V8.1.2 | Verify that all cached or temporary copies of sensitive data stored on the server are protected from unauthorized access or purged/invalidated after the authorized user accesses the sensitive data | L2 | CWE-524 |
| V8.1.3 | Verify the application minimizes the number of parameters in a request, such as hidden fields, Ajax variables, cookies and header values | L2 | CWE-233 |
| V8.1.4 | Verify the application can detect and alert on abnormal numbers of requests such as by IP, user, total per hour or day, or whatever makes sense for the application | L2 | CWE-770 |
| V8.2.1 | Verify the application sets sufficient anti-caching headers so that sensitive data is not cached in modern browsers | L1 | CWE-525 |
| V8.2.2 | Verify that data stored in browser storage (localStorage, sessionStorage, IndexedDB, or cookies) does not contain sensitive data | L1 | CWE-922 |
| V8.2.3 | Verify that authenticated data is cleared from client storage, such as the browser DOM, after the client or session is terminated | L1 | CWE-922 |
| V8.3.1 | Verify that sensitive data is sent to the server in the HTTP message body or headers and that query string parameters from any HTTP verb do not contain sensitive data | L1 | CWE-319 |
| V8.3.2 | Verify that users have a method to remove or export their data on demand | L1 | CWE-212 |
| V8.3.3 | Verify that users are provided clear language regarding collection and use of supplied personal information and that users have provided opt-in consent for the use of that data before it is used | L1 | CWE-285 |
| V8.3.4 | Verify that all sensitive data created and processed by the application has been identified and ensure that a policy is in place on how to deal with sensitive data | L1 | CWE-200 |
| V8.3.5 | Verify accessing sensitive data is audited (without logging the sensitive data itself), if the data is collected under relevant data protection directives or where logging of access is required | L2 | CWE-532 |
| V8.3.6 | Verify that sensitive information contained in memory is overwritten as soon as it is no longer required to mitigate memory dumping attacks | L2 | CWE-226 |
| V8.3.7 | Verify that sensitive or private information required to be encrypted is encrypted using approved algorithms that provide both confidentiality and integrity | L2 | CWE-327 |
| V8.3.8 | Verify that sensitive personal information is subject to data retention classification, such that old or out of date data is deleted automatically on a schedule or as the situation requires | L2 | CWE-285 |

## V9: Communication

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V9.1.1 | Verify that TLS is used for all client connectivity and does not fall back to insecure or unencrypted communications | L1 | CWE-319 |
| V9.1.2 | Verify using up-to-date TLS testing tools that only strong cipher suites are enabled with the strongest cipher suites set as preferred | L1 | CWE-326 |
| V9.1.3 | Verify that only the latest recommended versions of the TLS protocol are enabled such as TLS 1.2 and TLS 1.3. The latest version of TLS should be the preferred option | L1 | CWE-326 |
| V9.2.1 | Verify that connections to and from the server use trusted TLS certificates. Where internally generated or self-signed certificates are used, the server must be configured to only trust specific internal CAs and specific self-signed certificates | L2 | CWE-295 |
| V9.2.2 | Verify that encrypted communications such as TLS are used for all inbound and outbound connections, including for management ports, monitoring, authentication, API or web service calls, database, cache, storage, and all other components | L2 | CWE-319 |
| V9.2.3 | Verify that all encrypted connections to external systems that involve sensitive information or functions are authenticated | L2 | CWE-287 |
| V9.2.4 | Verify that proper certification revocation such as Online Certificate Status Protocol (OCSP) stapling is enabled and configured | L2 | CWE-299 |
| V9.2.5 | Verify that backend TLS connection failures are logged | L3 | CWE-544 |

## V10: Malicious Code

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V10.1.1 | Verify that a code analysis tool is in use that can detect potentially malicious code such as time functions, unsafe file operations, and suspicious network connections | L3 | CWE-1104 |
| V10.2.1 | Verify that the application source code and third-party libraries do not contain unauthorized phone-home or data collection capabilities | L2 | CWE-829 |
| V10.2.2 | Verify that the application does not ask for unnecessary or excessive permissions to privacy-related features or sensors | L2 | CWE-272 |
| V10.3.1 | Verify that if the application has a client or server auto-update feature, updates should be obtained over secure channels and digitally signed | L1 | CWE-16 |
| V10.3.2 | Verify that the application employs integrity protections such as code signing or subresource integrity. The application must not load or execute code from untrusted sources | L1 | CWE-353 |
| V10.3.3 | Verify that the application has protection from subdomain takeovers if the application relies upon DNS entries or DNS subdomains | L1 | CWE-350 |

## V11: Business Logic

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V11.1.1 | Verify that the application will only process business logic flows for the same user in sequential step order and without skipping steps | L1 | CWE-841 |
| V11.1.2 | Verify that the application will only process business logic flows with all steps being processed in realistic human time | L1 | CWE-799 |
| V11.1.3 | Verify the application has appropriate limits for specific business actions or transactions which are correctly enforced on a per-user basis | L1 | CWE-770 |
| V11.1.4 | Verify that the application has anti-automation controls to protect against excessive calls such as mass data exfiltration, business logic requests, file uploads, or denial of service attacks | L1 | CWE-770 |
| V11.1.5 | Verify the application has business logic limits or validation to protect against likely business risks or threats identified using threat modeling or similar methodologies | L1 | CWE-841 |
| V11.1.6 | Verify that the application does not suffer from time-of-check to time-of-use (TOCTOU) issues or other race conditions for sensitive operations | L2 | CWE-367 |
| V11.1.7 | Verify the application monitors for unusual events or activity from a business logic perspective | L2 | CWE-754 |
| V11.1.8 | Verify that the application has configurable alerting when automated attacks or unusual activity is detected | L2 | CWE-390 |

## V12: Files and Resources

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V12.1.1 | Verify that the application will not accept large files that could fill up storage or cause a denial of service | L1 | CWE-400 |
| V12.1.2 | Verify that the application checks compressed files (zip, gz, docx, odt) against maximum allowed uncompressed size and against maximum number of files before uncompressing the file | L2 | CWE-409 |
| V12.1.3 | Verify that a file size quota and maximum number of files per user is enforced to ensure that a single user cannot fill up the storage with too many files or excessively large files | L2 | CWE-770 |
| V12.2.1 | Verify that files obtained from untrusted sources are validated to be of expected type based on the file content | L2 | CWE-434 |
| V12.3.1 | Verify that user-submitted filename metadata is not used directly by system or framework file systems and that a URL API is used to protect against path traversal | L1 | CWE-22 |
| V12.3.2 | Verify that user-submitted filename metadata is validated or ignored to prevent the disclosure, creation, updating, or removal of local files | L1 | CWE-73 |
| V12.3.3 | Verify that user-submitted filename metadata is validated or ignored to prevent the disclosure or execution of remote files via RFI or SSRF attacks | L1 | CWE-98 |
| V12.3.4 | Verify that the application protects against Reflective File Download (RFD) by validating or ignoring user-submitted filenames in a JSON, JSONP, or URL parameter | L1 | CWE-641 |
| V12.3.5 | Verify that untrusted file metadata is not used directly with system API or libraries to protect against OS command injection | L1 | CWE-78 |
| V12.3.6 | Verify that the application does not include and execute functionality from untrusted sources such as unverified CDNs, JavaScript libraries, npm libraries, or server-side DLLs | L2 | CWE-829 |
| V12.4.1 | Verify that files obtained from untrusted sources are stored outside the web root with limited permissions | L1 | CWE-552 |
| V12.4.2 | Verify that files obtained from untrusted sources are scanned by antivirus scanners to prevent upload and serving of known malicious content | L1 | CWE-509 |
| V12.5.1 | Verify that the web tier is configured to serve only files with specific file extensions to prevent unintentional information and source code leakage | L1 | CWE-552 |
| V12.5.2 | Verify that direct requests to uploaded files will never be executed as HTML/JavaScript content | L1 | CWE-434 |
| V12.6.1 | Verify that the web or application server is configured with an allow list of resources or systems to which the server can send requests or load data from | L1 | CWE-918 |

## V13: API and Web Service

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V13.1.1 | Verify that all application components use the same encodings and parsers to avoid parsing attacks that exploit different URI or file parsing behavior that could be used in SSRF and RFI attacks | L1 | CWE-116 |
| V13.1.2 | Verify that access to administration and management functions is limited to authorized administrators | L1 | CWE-419 |
| V13.1.3 | Verify API URLs do not expose sensitive information such as the API key, session tokens, etc. | L1 | CWE-598 |
| V13.1.4 | Verify that authorization decisions are made at both the URI validated by a programmatic or declarative security at the controller or router and at the resource level validated by model-based permissions | L2 | CWE-285 |
| V13.1.5 | Verify that requests containing unexpected or missing content types are rejected with appropriate headers (HTTP 406 or 415) | L2 | CWE-434 |
| V13.2.1 | Verify that enabled RESTful HTTP methods are a valid choice for the user or action | L1 | CWE-650 |
| V13.2.2 | Verify that JSON schema validation is in place and verified before accepting input | L1 | CWE-20 |
| V13.2.3 | Verify that RESTful web services that utilize cookies are protected from CSRF via the use of at least one or more of the following: double submit cookie pattern, CSRF nonces, or origin request header checks | L1 | CWE-352 |
| V13.2.5 | Verify that REST services explicitly check the incoming Content-Type to be the expected one | L2 | CWE-436 |
| V13.2.6 | Verify that the message headers and payload are trustworthy and not modified in transit. Requiring strong encryption for transport (TLS only) may be sufficient as it provides both confidentiality and integrity protection | L2 | CWE-345 |
| V13.3.1 | Verify that XSD schema validation takes place to ensure a properly formed XML document, followed by validation of each input field before any processing of that data takes place | L1 | CWE-20 |
| V13.3.2 | Verify that the message payload is signed using WS-Security to ensure reliable transport between client and service | L2 | CWE-345 |
| V13.4.1 | Verify that a query allow list or a combination of depth limiting and amount limiting is used to prevent GraphQL or data layer expression DoS due to expensive nested queries | L2 | CWE-770 |
| V13.4.2 | Verify that GraphQL or other data layer authorization logic should be implemented at the business logic layer instead of the GraphQL layer | L2 | CWE-285 |

## V14: Configuration

| ID | Description | Level | CWE |
|----|-------------|-------|-----|
| V14.1.1 | Verify that the application build and deployment processes are performed in a secure and repeatable way, such as CI/CD automation, automated configuration management, and automated deployment scripts | L2 | CWE-1104 |
| V14.1.2 | Verify that compiler flags are configured to enable all available buffer overflow protections and warnings, including stack randomization, data execution prevention, and to break the build if an unsafe pointer, memory, format string, integer, or string operation is found | L3 | CWE-120 |
| V14.1.3 | Verify that server configuration is hardened as per the recommendations of the application server and frameworks in use | L2 | CWE-16 |
| V14.1.4 | Verify that the application, configuration, and all dependencies can be re-deployed using automated deployment scripts, built from a documented and tested runbook in a reasonable time, or restored from backups in a timely fashion | L2 | CWE-1104 |
| V14.1.5 | Verify that authorized administrators can verify the integrity of all security-relevant configurations to detect tampering | L3 | CWE-353 |
| V14.2.1 | Verify that all components are up to date, preferably using a dependency checker during build or compile time | L1 | CWE-1104 |
| V14.2.2 | Verify that all unneeded features, documentation, sample applications, and configurations are removed | L1 | CWE-1002 |
| V14.2.3 | Verify that if application assets such as JavaScript libraries, CSS or web fonts are hosted externally on a CDN or external provider, Subresource Integrity (SRI) is used to validate the integrity of the asset | L1 | CWE-829 |
| V14.2.4 | Verify that third-party components come from pre-defined trusted and continually maintained repositories | L2 | CWE-829 |
| V14.2.5 | Verify that a Software Bill of Materials (SBOM) is maintained of all third-party libraries in use | L2 | CWE-1104 |
| V14.2.6 | Verify that the attack surface is reduced by sandboxing or encapsulating third-party libraries to expose only the required behavior into the application | L2 | CWE-265 |
| V14.3.1 | Verify that web or application server and application framework debug modes are disabled in production to eliminate debug features, developer consoles, and unintended security disclosures | L1 | CWE-497 |
| V14.3.2 | Verify that the HTTP headers or any part of the HTTP response do not expose detailed version information of system components | L1 | CWE-200 |
| V14.3.3 | Verify that every HTTP response contains a Content-Type header specifying a safe character set | L1 | CWE-173 |
| V14.4.1 | Verify that every HTTP response contains a Content-Disposition header that sets a safe filename | L1 | CWE-116 |
| V14.4.2 | Verify that the content of the Content Security Policy (CSP) response header is defined and applied | L1 | CWE-1021 |
| V14.4.3 | Verify that the X-Content-Type-Options header is set to nosniff | L1 | CWE-116 |
| V14.4.4 | Verify that a Strict-Transport-Security header is included on all responses and for all subdomains | L1 | CWE-523 |
| V14.4.5 | Verify that a suitable Referrer-Policy header is included to avoid exposing sensitive information in the URL through the Referer header to untrusted parties | L1 | CWE-116 |
| V14.4.6 | Verify that a suitable X-Frame-Options or Content-Security-Policy frame-ancestors header is in use for sites where content should not be embedded in a third-party site | L1 | CWE-1021 |
| V14.4.7 | Verify that the Content Security Policy header specifies a default-src that will fall back if a more specific directive is not defined | L1 | CWE-1021 |
| V14.5.1 | Verify that the application server only accepts the HTTP methods in use by the application/API, including pre-flight OPTIONS and that any requests to methods invalid for the application context are logged | L1 | CWE-749 |
| V14.5.2 | Verify that the supplied Origin header is not used for authentication or access control decisions, as the Origin header can easily be changed by an attacker | L1 | CWE-346 |
| V14.5.3 | Verify that the cross-domain resource sharing (CORS) Access-Control-Allow-Origin header uses a strict allow list of trusted domains and subdomains to match against and does not support the null origin | L1 | CWE-346 |
| V14.5.4 | Verify that HTTP headers added by a trusted proxy or SSO devices, such as a bearer token, are authenticated by the application | L2 | CWE-306 |
