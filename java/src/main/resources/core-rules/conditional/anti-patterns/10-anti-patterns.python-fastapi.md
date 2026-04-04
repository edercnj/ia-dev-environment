# Rule 10 — Anti-Patterns ({LANGUAGE_NAME} + {FRAMEWORK_NAME})

> Language-specific anti-patterns with incorrect and correct code examples.
> Each entry references the rule or knowledge pack it violates.

## Anti-Patterns

### ANTI-001: Synchronous Endpoint Blocking Event Loop (CRITICAL)
**Category:** CONCURRENCY
**Rule violated:** `03-coding-standards.md` (async patterns)

**Incorrect code:**
```python
# Synchronous def blocks the asyncio event loop
@app.get("/users/{user_id}")
def get_user(user_id: int):
    # This blocks the event loop for ALL requests
    user = db.query(User).filter(User.id == user_id).first()
    return user
```

**Correct code:**
```python
# async def allows event loop to handle concurrent requests
@app.get("/users/{user_id}")
async def get_user(
    user_id: int,
    db: AsyncSession = Depends(get_db),
) -> UserResponse:
    user = await db.get(User, user_id)
    if user is None:
        raise HTTPException(
            status_code=404,
            detail=f"User not found: {user_id}",
        )
    return UserResponse.model_validate(user)
```

### ANTI-002: Pydantic Model Without Validation (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `06-security-baseline.md` (input validation)

**Incorrect code:**
```python
# No validation — accepts any value, any type
from typing import Any

class CreateUserRequest(BaseModel):
    name: Any  # no type safety
    email: Any  # no format validation
    age: Any  # no range check
```

**Correct code:**
```python
# Explicit validation with field constraints
from pydantic import BaseModel, EmailStr, Field

class CreateUserRequest(BaseModel):
    name: str = Field(
        ..., min_length=1, max_length=100,
    )
    email: EmailStr
    age: int = Field(..., ge=0, le=150)
```

### ANTI-003: Circular Dependency Injection (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `04-architecture-summary.md` (dependency direction)

**Incorrect code:**
```python
# Circular dependency — A depends on B, B depends on A
class OrderService:
    def __init__(self, payment_service: "PaymentService"):
        self.payment_service = payment_service

class PaymentService:
    def __init__(self, order_service: OrderService):
        self.order_service = order_service  # circular
```

**Correct code:**
```python
# Break cycle with port interface (DIP)
from abc import ABC, abstractmethod

class PaymentPort(ABC):
    @abstractmethod
    async def process(self, amount: Decimal) -> PaymentResult:
        ...

class OrderService:
    def __init__(self, payment_port: PaymentPort):
        self.payment_port = payment_port

class PaymentService(PaymentPort):
    async def process(self, amount: Decimal) -> PaymentResult:
        return PaymentResult(success=True)
```

### ANTI-004: Bare Exception Handling (HIGH)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md#error-handling`

**Incorrect code:**
```python
# Catches everything, swallows all errors
@app.get("/data")
async def get_data():
    try:
        return await fetch_data()
    except:  # bare except — catches SystemExit, KeyboardInterrupt
        pass  # silently swallowed
```

**Correct code:**
```python
# Specific exception with context and proper HTTP response
@app.get("/data")
async def get_data():
    try:
        return await fetch_data()
    except DataFetchError as e:
        raise HTTPException(
            status_code=502,
            detail=f"Failed to fetch data: {e}",
        ) from e
```

### ANTI-005: Global Mutable State (CRITICAL)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#forbidden`

**Incorrect code:**
```python
# Global mutable dict — race condition in async context
_cache: dict[str, str] = {}

@app.get("/cached/{key}")
async def get_cached(key: str):
    if key not in _cache:
        _cache[key] = await expensive_lookup(key)
    return {"value": _cache[key]}
```

**Correct code:**
```python
# Injected cache service with proper concurrency control
class CacheService:
    def __init__(self):
        self._lock = asyncio.Lock()
        self._cache: dict[str, str] = {}

    async def get_or_fetch(self, key: str) -> str:
        async with self._lock:
            if key not in self._cache:
                self._cache[key] = await expensive_lookup(key)
            return self._cache[key]

@app.get("/cached/{key}")
async def get_cached(
    key: str,
    cache: CacheService = Depends(get_cache_service),
):
    value = await cache.get_or_fetch(key)
    return {"value": value}
```
