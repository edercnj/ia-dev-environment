# Example: Testing

### WebApplicationFactory Integration Tests

```csharp
public class MerchantApiTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;
    private readonly WebApplicationFactory<Program> _factory;

    public MerchantApiTests(WebApplicationFactory<Program> factory)
    {
        _factory = factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                // Replace real DB with in-memory
                services.RemoveAll<DbContextOptions<AppDbContext>>();
                services.AddDbContext<AppDbContext>(options =>
                    options.UseInMemoryDatabase("TestDb"));

                // Replace external services with mocks
                services.RemoveAll<ITokenProvider>();
                services.AddSingleton<ITokenProvider>(new MockTokenProvider());
            });
        });
        _client = _factory.CreateClient();
    }

    [Fact]
    public async Task CreateMerchant_ReturnsCreated()
    {
        var request = new CreateMerchantRequest("12345", "Test Shop", MerchantType.Physical);

        var response = await _client.PostAsJsonAsync("/api/v1/merchants", request);

        response.StatusCode.Should().Be(HttpStatusCode.Created);

        var merchant = await response.Content.ReadFromJsonAsync<MerchantResponse>();
        merchant.Should().NotBeNull();
        merchant!.Mid.Should().Be("12345");
        merchant.Name.Should().Be("Test Shop");
    }

    [Fact]
    public async Task GetMerchant_WhenNotFound_Returns404ProblemDetails()
    {
        var response = await _client.GetAsync("/api/v1/merchants/999");

        response.StatusCode.Should().Be(HttpStatusCode.NotFound);

        var problem = await response.Content.ReadFromJsonAsync<ProblemDetails>();
        problem.Should().NotBeNull();
        problem!.Title.Should().Be("Merchant Not Found");
        problem.Type.Should().Be("https://api.example.com/errors/not-found");
    }

    [Fact]
    public async Task CreateMerchant_InvalidRequest_Returns400()
    {
        var request = new CreateMerchantRequest("", "", MerchantType.Physical);

        var response = await _client.PostAsJsonAsync("/api/v1/merchants", request);

        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task ListMerchants_ReturnsPaginatedResponse()
    {
        // Seed data
        using var scope = _factory.Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        context.Merchants.AddRange(
            new MerchantEntity { Mid = "001", Name = "Shop A", Status = "active", Type = MerchantType.Physical },
            new MerchantEntity { Mid = "002", Name = "Shop B", Status = "active", Type = MerchantType.Online }
        );
        await context.SaveChangesAsync();

        var response = await _client.GetAsync("/api/v1/merchants?page=0&limit=10");

        response.StatusCode.Should().Be(HttpStatusCode.OK);
    }
}
```

### Unit Tests with Moq

```csharp
public class MerchantServiceTests
{
    private readonly Mock<IMerchantRepository> _repositoryMock;
    private readonly Mock<ILogger<MerchantService>> _loggerMock;
    private readonly IOptions<FeatureOptions> _options;
    private readonly MerchantService _sut;

    public MerchantServiceTests()
    {
        _repositoryMock = new Mock<IMerchantRepository>();
        _loggerMock = new Mock<ILogger<MerchantService>>();
        _options = Options.Create(new FeatureOptions { RequireApproval = false });
        _sut = new MerchantService(
            _repositoryMock.Object,
            _loggerMock.Object,
            new OptionsWrapper<FeatureOptions>(_options.Value));
    }

    [Fact]
    public async Task CreateAsync_ValidCommand_ReturnsMerchant()
    {
        var command = new CreateMerchantCommand("12345", "Test Shop", MerchantType.Physical);
        _repositoryMock
            .Setup(r => r.AddAsync(It.IsAny<Merchant>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var result = await _sut.CreateAsync(command, CancellationToken.None);

        result.Should().NotBeNull();
        result.Mid.Should().Be("12345");
        _repositoryMock.Verify(r => r.AddAsync(It.IsAny<Merchant>(), It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task FindByIdAsync_WhenNotFound_ReturnsNull()
    {
        _repositoryMock
            .Setup(r => r.FindByIdAsync(999, It.IsAny<CancellationToken>()))
            .ReturnsAsync((Merchant?)null);

        var result = await _sut.FindByIdAsync(999, CancellationToken.None);

        result.Should().BeNull();
    }
}
```

### FluentAssertions Examples

```csharp
// Object assertions
merchant.Should().NotBeNull();
merchant.Should().BeEquivalentTo(expected, options =>
    options.Excluding(m => m.CreatedAt));

// Collection assertions
merchants.Should().HaveCount(5);
merchants.Should().ContainSingle(m => m.Mid == "12345");
merchants.Should().BeInDescendingOrder(m => m.CreatedAt);

// Exception assertions
var act = () => service.CreateAsync(invalidCommand, CancellationToken.None);
await act.Should().ThrowAsync<ValidationException>()
    .WithMessage("*MID*required*");
```
