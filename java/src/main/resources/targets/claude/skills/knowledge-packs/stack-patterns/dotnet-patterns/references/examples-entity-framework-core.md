# Example: Entity Framework Core

### DbContext

```csharp
public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<MerchantEntity> Merchants => Set<MerchantEntity>();
    public DbSet<AuditLogEntity> AuditLogs => Set<AuditLogEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(AppDbContext).Assembly);
    }

    public override Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        var entries = ChangeTracker.Entries<BaseEntity>();
        foreach (var entry in entries)
        {
            switch (entry.State)
            {
                case EntityState.Added:
                    entry.Entity.CreatedAt = DateTimeOffset.UtcNow;
                    entry.Entity.UpdatedAt = DateTimeOffset.UtcNow;
                    break;
                case EntityState.Modified:
                    entry.Entity.UpdatedAt = DateTimeOffset.UtcNow;
                    break;
            }
        }
        return base.SaveChangesAsync(cancellationToken);
    }
}
```

### Entity Configuration

```csharp
public class MerchantEntity : BaseEntity
{
    public long Id { get; set; }
    public string Mid { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Status { get; set; } = "active";
    public MerchantType Type { get; set; }

    // Navigation property
    public ICollection<TransactionEntity> Transactions { get; set; } = [];
}

public class MerchantEntityConfiguration : IEntityTypeConfiguration<MerchantEntity>
{
    public void Configure(EntityTypeBuilder<MerchantEntity> builder)
    {
        builder.ToTable("merchants");
        builder.HasKey(m => m.Id);
        builder.Property(m => m.Mid).HasMaxLength(15).IsRequired();
        builder.Property(m => m.Name).HasMaxLength(100).IsRequired();
        builder.Property(m => m.Status).HasMaxLength(20).IsRequired();
        builder.Property(m => m.Type).HasConversion<string>().HasMaxLength(20);

        builder.HasIndex(m => m.Mid).IsUnique();

        builder.HasMany(m => m.Transactions)
            .WithOne(t => t.Merchant)
            .HasForeignKey(t => t.MerchantId);
    }
}
```

### Repository Pattern with EF Core

```csharp
public class MerchantRepository : IMerchantRepository
{
    private readonly AppDbContext _context;

    public MerchantRepository(AppDbContext context)
    {
        _context = context;
    }

    public async Task<Merchant?> FindByIdAsync(long id, CancellationToken ct)
    {
        var entity = await _context.Merchants
            .AsNoTracking()
            .FirstOrDefaultAsync(m => m.Id == id, ct);

        return entity?.ToDomain();
    }

    public async Task<(IReadOnlyList<Merchant> Items, int Total)> FindAllAsync(
        int page, int limit, string? status, CancellationToken ct)
    {
        var query = _context.Merchants.AsNoTracking();

        if (!string.IsNullOrEmpty(status))
            query = query.Where(m => m.Status == status);

        var total = await query.CountAsync(ct);

        var items = await query
            .OrderByDescending(m => m.CreatedAt)
            .Skip(page * limit)
            .Take(limit)
            .Select(m => m.ToDomain())
            .ToListAsync(ct);

        return (items, total);
    }

    public async Task AddAsync(Merchant merchant, CancellationToken ct)
    {
        var entity = MerchantEntity.FromDomain(merchant);
        _context.Merchants.Add(entity);
        await _context.SaveChangesAsync(ct);
    }
}
```

### Migrations

```bash
# Add a migration
dotnet ef migrations add CreateMerchants

# Update database
dotnet ef database update

# Generate SQL script (for production)
dotnet ef migrations script --idempotent -o migrations.sql

# Remove last migration (if not applied)
dotnet ef migrations remove
```

### LINQ Queries

```csharp
// Projection query (returns only needed columns)
var summaries = await _context.Merchants
    .AsNoTracking()
    .Where(m => m.Status == "active")
    .Select(m => new MerchantSummary(m.Id, m.Mid, m.Name))
    .ToListAsync(ct);

// Aggregation
var countByStatus = await _context.Merchants
    .GroupBy(m => m.Status)
    .Select(g => new { Status = g.Key, Count = g.Count() })
    .ToListAsync(ct);

// Exists check (efficient — stops at first match)
var exists = await _context.Merchants
    .AnyAsync(m => m.Mid == mid, ct);
```
