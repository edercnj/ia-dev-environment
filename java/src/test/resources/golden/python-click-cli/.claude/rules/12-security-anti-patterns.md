# Rule 12 — Security Anti-Patterns (Python)

> Language-specific security anti-patterns with vulnerable and fixed code examples.
> Each entry references its CWE identifier and severity level.

## Security Anti-Patterns

### P1: pickle.loads(user_input)
**CWE:** CWE-502 — Deserialization of Untrusted Data
**Severity:** CRITICAL

#### Vulnerable Code
```python
# Deserializes arbitrary Python objects from untrusted input
import pickle

def load_session(data: bytes) -> dict:
    return pickle.loads(data)
```

#### Fixed Code
```python
# Use JSON for untrusted data — no arbitrary code execution
import json

def load_session(data: bytes) -> dict:
    return json.loads(data.decode("utf-8"))
```

#### Why it is dangerous
`pickle.loads()` can execute arbitrary Python code embedded in the serialized stream. An attacker can craft a pickle payload that runs system commands (`os.system`, `subprocess.call`) when deserialized, achieving remote code execution.

### P2: eval()/exec() with User Input
**CWE:** CWE-95 — Improper Neutralization of Directives in Dynamically Evaluated Code
**Severity:** CRITICAL

#### Vulnerable Code
```python
# User input executed as Python code
def calculate(expression: str) -> float:
    return eval(expression)
```

#### Fixed Code
```python
# Safe expression parsing without code execution
import ast

def calculate(expression: str) -> float:
    tree = ast.parse(expression, mode="eval")
    for node in ast.walk(tree):
        if not isinstance(
            node,
            (ast.Expression, ast.BinOp, ast.Constant,
             ast.Add, ast.Sub, ast.Mult, ast.Div),
        ):
            raise ValueError(
                f"Unsupported expression: {expression}"
            )
    return float(eval(compile(tree, "<calc>", "eval")))
```

#### Why it is dangerous
`eval()` and `exec()` execute arbitrary Python code. An attacker can pass `__import__('os').system('rm -rf /')` as input, gaining full control of the server. Even sandboxed `eval()` can be escaped through Python's introspection capabilities.

### P3: f-string SQL Query
**CWE:** CWE-89 — SQL Injection
**Severity:** CRITICAL

#### Vulnerable Code
```python
# User input interpolated directly into SQL query
async def find_user(db: AsyncSession, name: str):
    query = f"SELECT * FROM users WHERE name = '{name}'"
    result = await db.execute(text(query))
    return result.fetchall()
```

#### Fixed Code
```python
# Parameterized query prevents SQL injection
async def find_user(db: AsyncSession, name: str):
    query = text(
        "SELECT * FROM users WHERE name = :name"
    )
    result = await db.execute(
        query, {"name": name}
    )
    return result.fetchall()
```

#### Why it is dangerous
String interpolation in SQL queries allows an attacker to inject arbitrary SQL (e.g., `' OR 1=1 --`). This can bypass authentication, exfiltrate entire databases, or execute administrative operations. Parameterized queries treat user input as data, never as SQL.

### P4: jwt.decode() Without Verification
**CWE:** CWE-347 — Improper Verification of Cryptographic Signature
**Severity:** CRITICAL

#### Vulnerable Code
```python
# JWT decoded without signature verification
import jwt

def get_user_id(token: str) -> str:
    payload = jwt.decode(
        token,
        options={"verify_signature": False},
    )
    return payload["sub"]
```

#### Fixed Code
```python
# JWT decoded with signature verification and audience
import jwt

def get_user_id(
    token: str,
    secret: str,
    audience: str,
) -> str:
    payload = jwt.decode(
        token,
        secret,
        algorithms=["HS256"],
        audience=audience,
    )
    return payload["sub"]
```

#### Why it is dangerous
Without signature verification, an attacker can forge JWT tokens with arbitrary claims (e.g., `admin: true`, any `sub` value). The `options={"verify_signature": False}` flag explicitly disables the only security mechanism that proves the token was issued by a trusted party.

### P5: subprocess(shell=True) with User Input
**CWE:** CWE-78 — Improper Neutralization of Special Elements used in an OS Command
**Severity:** CRITICAL

#### Vulnerable Code
```python
# User input passed to shell command
import subprocess

def convert_file(filename: str) -> bytes:
    result = subprocess.run(
        f"convert {filename} output.pdf",
        shell=True,
        capture_output=True,
    )
    return result.stdout
```

#### Fixed Code
```python
# Argument list prevents shell injection
import subprocess
from pathlib import Path

def convert_file(filename: str) -> bytes:
    safe_path = Path(filename).resolve()
    if not safe_path.is_relative_to(Path("/uploads")):
        raise ValueError(
            f"Invalid file path: {filename}"
        )
    result = subprocess.run(
        ["convert", str(safe_path), "output.pdf"],
        capture_output=True,
        check=True,
    )
    return result.stdout
```

#### Why it is dangerous
With `shell=True`, the command string is passed to `/bin/sh`, allowing shell metacharacters. An attacker can inject `; rm -rf /` or `$(cat /etc/passwd)` into the filename. Using an argument list (`shell=False`) prevents the shell from interpreting metacharacters.
