# Rule 12 — Security Anti-Patterns (TypeScript)

> Language-specific security anti-patterns with vulnerable and fixed code examples.
> Each entry references its CWE identifier and severity level.

## Security Anti-Patterns

### T1: eval(req.body.expression)
**CWE:** CWE-95 — Improper Neutralization of Directives in Dynamically Evaluated Code
**Severity:** CRITICAL

#### Vulnerable Code
```typescript
// User input executed as JavaScript code
@Post('calculate')
async calculate(
  @Body() body: { expression: string },
): Promise<number> {
  return eval(body.expression);
}
```

#### Fixed Code
```typescript
// Safe math expression parser without code execution
import { evaluate } from 'mathjs';

@Post('calculate')
async calculate(
  @Body() body: CalculateRequest,
): Promise<number> {
  return evaluate(body.expression);
}
```

#### Why it is dangerous
`eval()` executes arbitrary JavaScript code in the Node.js process context. An attacker can pass `require('child_process').execSync('rm -rf /')` as the expression, gaining full control of the server. There is no safe way to sandbox `eval()` in Node.js.

### T2: Object.assign Without Prototype Check
**CWE:** CWE-1321 — Improperly Controlled Modification of Object Prototype Attributes
**Severity:** HIGH

#### Vulnerable Code
```typescript
// User input merged without prototype check
function updateConfig(
  config: Record<string, unknown>,
  userInput: Record<string, unknown>,
): Record<string, unknown> {
  return Object.assign(config, userInput);
}
```

#### Fixed Code
```typescript
// Validated DTO with class-validator prevents pollution
import { IsString, IsOptional } from 'class-validator';

class UpdateConfigDto {
  @IsString()
  @IsOptional()
  theme?: string;

  @IsString()
  @IsOptional()
  locale?: string;
}

function updateConfig(
  config: Record<string, unknown>,
  dto: UpdateConfigDto,
): Record<string, unknown> {
  return { ...config, ...dto };
}
```

#### Why it is dangerous
An attacker can send `{"__proto__": {"isAdmin": true}}` in the request body. `Object.assign` copies this to the object prototype, making `isAdmin` true for every object in the application. This can bypass authorization checks, enable debug modes, or trigger denial of service.

### T3: Regex Without ReDoS Protection
**CWE:** CWE-1333 — Inefficient Regular Expression Complexity
**Severity:** MEDIUM

#### Vulnerable Code
```typescript
// Regex with catastrophic backtracking potential
function validateEmail(email: string): boolean {
  const emailRegex =
    /^([a-zA-Z0-9]+\.)*[a-zA-Z0-9]+@([a-zA-Z0-9]+\.)+[a-zA-Z]{2,}$/;
  return emailRegex.test(email);
}
```

#### Fixed Code
```typescript
// Use class-validator or a linear-time regex
import { isEmail } from 'class-validator';

function validateEmail(email: string): boolean {
  return isEmail(email);
}
```

#### Why it is dangerous
Nested quantifiers (`(a+)+`, `(a*)*`, `(a|a)+`) cause the regex engine to explore exponentially many paths on malicious input. An attacker can send a specially crafted string that causes the regex to run for minutes or hours, blocking the Node.js event loop and making the service unresponsive.

### T4: innerHTML with User Input
**CWE:** CWE-79 — Improper Neutralization of Input During Web Page Generation
**Severity:** HIGH

#### Vulnerable Code
```typescript
// User input injected as raw HTML
function renderComment(comment: string): string {
  return `<div>${comment}</div>`;
}

// Or in DOM manipulation
element.innerHTML = userInput;
```

#### Fixed Code
```typescript
// Sanitize HTML before rendering
import DOMPurify from 'dompurify';

function renderComment(comment: string): string {
  const sanitized = DOMPurify.sanitize(comment);
  return `<div>${sanitized}</div>`;
}

// Or use textContent for plain text
element.textContent = userInput;
```

#### Why it is dangerous
Setting `innerHTML` with unsanitized user input allows an attacker to inject `<script>` tags, `<img onerror>` handlers, or other HTML that executes JavaScript in the victim's browser. This enables session hijacking, credential theft, and actions performed as the victim.

### T5: jwt.verify Without Algorithms Restriction
**CWE:** CWE-327 — Use of a Broken or Risky Cryptographic Algorithm
**Severity:** CRITICAL

#### Vulnerable Code
```typescript
// JWT verified without restricting algorithms
import * as jwt from 'jsonwebtoken';

function verifyToken(token: string, secret: string) {
  return jwt.verify(token, secret);
}
```

#### Fixed Code
```typescript
// JWT verified with explicit algorithm restriction
import * as jwt from 'jsonwebtoken';

function verifyToken(
  token: string,
  secret: string,
): jwt.JwtPayload {
  return jwt.verify(token, secret, {
    algorithms: ['HS256'],
    complete: false,
  }) as jwt.JwtPayload;
}
```

#### Why it is dangerous
Without an `algorithms` restriction, an attacker can change the JWT header to `"alg": "none"` (bypassing verification entirely) or switch from RS256 to HS256 (using the public key as the HMAC secret). Explicitly specifying the allowed algorithm prevents algorithm confusion attacks.
