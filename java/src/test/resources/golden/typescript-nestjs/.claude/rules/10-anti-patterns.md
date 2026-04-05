# Rule 10 — Anti-Patterns (typescript + nestjs)

> Language-specific anti-patterns with incorrect and correct code examples.
> Each entry references the rule or knowledge pack it violates.

## Anti-Patterns

### ANTI-001: God Service (CRITICAL)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (SRP)

**Incorrect code:**
```typescript
// Service with multiple responsibilities — violates SRP
@Injectable()
export class OrderService {
  async createOrder(req: OrderRequest): Promise<Order> { /* ... */ }
  async sendEmail(to: string, body: string): Promise<void> { /* ... */ }
  async generateInvoice(order: Order): Promise<Invoice> { /* ... */ }
  async updateInventory(order: Order): Promise<void> { /* ... */ }
}
```

**Correct code:**
```typescript
// Each service has a single responsibility
@Injectable()
export class OrderService {
  constructor(
    private readonly inventoryPort: InventoryPort,
    private readonly notificationPort: NotificationPort,
  ) {}

  async createOrder(req: OrderRequest): Promise<Order> {
    const order = Order.create(req);
    await this.inventoryPort.reserve(order.items);
    await this.notificationPort.orderCreated(order);
    return order;
  }
}
```

### ANTI-002: Controller with Business Logic (CRITICAL)
**Category:** SERVICE_LAYER
**Rule violated:** `04-architecture-summary.md` (layer rules)

**Incorrect code:**
```typescript
// Controller contains business logic
@Controller('users')
export class UserController {
  constructor(private readonly userRepo: UserRepository) {}

  @Get(':id')
  async getUser(@Param('id') id: string) {
    const user = await this.userRepo.findOne({ where: { id } });
    if (!user) throw new NotFoundException();
    return user; // exposes entity
  }
}
```

**Correct code:**
```typescript
// Controller delegates to use case
@Controller('users')
export class UserController {
  constructor(
    private readonly findUserUseCase: FindUserUseCase,
  ) {}

  @Get(':id')
  async getUser(@Param('id') id: string): Promise<UserResponse> {
    return this.findUserUseCase.execute(id);
  }
}
```

### ANTI-003: any Type Usage (HIGH)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md` (type safety)

**Incorrect code:**
```typescript
// any defeats TypeScript type checking
async function processData(data: any): any {
  const result = data.items.map((item: any) => item.value);
  return { total: result.reduce((a: any, b: any) => a + b) };
}
```

**Correct code:**
```typescript
// Explicit types enforce compile-time safety
interface DataPayload {
  items: ReadonlyArray<{ value: number }>;
}

interface ProcessResult {
  total: number;
}

function processData(data: DataPayload): ProcessResult {
  const total = data.items.reduce(
    (sum, item) => sum + item.value, 0,
  );
  return { total };
}
```

### ANTI-004: Exception Swallowing (CRITICAL)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md#error-handling`

**Incorrect code:**
```typescript
// Exception swallowed — no logging, no re-throw
async function importData(path: string): Promise<void> {
  try {
    const data = await fs.readFile(path, 'utf-8');
    await process(data);
  } catch (e) {
    // silently ignored
  }
}
```

**Correct code:**
```typescript
// Exception logged with context and re-thrown
async function importData(path: string): Promise<void> {
  try {
    const data = await fs.readFile(path, 'utf-8');
    await process(data);
  } catch (error) {
    throw new DataImportException(
      `Failed to import file: ${path}`,
      { cause: error },
    );
  }
}
```

### ANTI-005: Circular Module Dependency (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `04-architecture-summary.md` (dependency direction)

**Incorrect code:**
```typescript
// Circular dependency between modules
@Module({
  imports: [PaymentModule], // OrderModule -> PaymentModule
})
export class OrderModule {}

@Module({
  imports: [OrderModule], // PaymentModule -> OrderModule (circular)
})
export class PaymentModule {}
```

**Correct code:**
```typescript
// Break cycle with port interface (DIP)
// domain/port/payment.port.ts
export interface PaymentPort {
  process(amount: number): Promise<PaymentResult>;
}

@Module({
  providers: [
    { provide: 'PaymentPort', useClass: PaymentAdapter },
  ],
})
export class OrderModule {}
```
