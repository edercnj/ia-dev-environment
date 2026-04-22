# Example: Web APIs

### Minimal APIs

```csharp
var app = builder.Build();

app.MapGet("/api/v1/merchants", async (
    IMerchantService service,
    [FromQuery] int page = 0,
    [FromQuery] int limit = 20,
    [FromQuery] string? status = null,
    CancellationToken ct = default) =>
{
    var result = await service.FindAllAsync(page, limit, status, ct);
    return Results.Ok(new PaginatedResponse<MerchantResponse>(
        result.Items.Select(m => m.ToResponse()),
        result.Total, page, limit));
});

app.MapGet("/api/v1/merchants/{id:long}", async (
    long id,
    IMerchantService service,
    CancellationToken ct) =>
{
    var merchant = await service.FindByIdAsync(id, ct);
    return merchant is not null
        ? Results.Ok(merchant.ToResponse())
        : Results.Problem(
            title: "Merchant Not Found",
            statusCode: StatusCodes.Status404NotFound,
            type: "https://api.example.com/errors/not-found");
});

app.MapPost("/api/v1/merchants", async (
    CreateMerchantRequest request,
    IMerchantService service,
    IValidator<CreateMerchantRequest> validator,
    CancellationToken ct) =>
{
    var validation = await validator.ValidateAsync(request, ct);
    if (!validation.IsValid)
        return Results.ValidationProblem(validation.ToDictionary());

    var merchant = await service.CreateAsync(request.ToCommand(), ct);
    return Results.Created($"/api/v1/merchants/{merchant.Id}", merchant.ToResponse());
})
.WithName("CreateMerchant")
.Produces<MerchantResponse>(StatusCodes.Status201Created)
.ProducesValidationProblem();

app.MapDelete("/api/v1/merchants/{id:long}", async (
    long id,
    IMerchantService service,
    CancellationToken ct) =>
{
    await service.DeactivateAsync(id, ct);
    return Results.NoContent();
});
```

### Controller-Based (Alternative)

```csharp
[ApiController]
[Route("api/v1/[controller]")]
public class MerchantsController : ControllerBase
{
    private readonly IMerchantService _service;
    private readonly IValidator<CreateMerchantRequest> _validator;

    public MerchantsController(
        IMerchantService service,
        IValidator<CreateMerchantRequest> validator)
    {
        _service = service;
        _validator = validator;
    }

    [HttpGet]
    [ProducesResponseType(typeof(PaginatedResponse<MerchantResponse>), StatusCodes.Status200OK)]
    public async Task<IActionResult> List(
        [FromQuery] int page = 0,
        [FromQuery] int limit = 20,
        [FromQuery] string? status = null,
        CancellationToken ct = default)
    {
        var result = await _service.FindAllAsync(page, limit, status, ct);
        return Ok(new PaginatedResponse<MerchantResponse>(
            result.Items.Select(m => m.ToResponse()),
            result.Total, page, limit));
    }

    [HttpPost]
    [ProducesResponseType(typeof(MerchantResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Create(
        [FromBody] CreateMerchantRequest request,
        CancellationToken ct)
    {
        var validation = await _validator.ValidateAsync(request, ct);
        if (!validation.IsValid)
            return ValidationProblem(new ValidationProblemDetails(validation.ToDictionary()));

        var merchant = await _service.CreateAsync(request.ToCommand(), ct);
        return CreatedAtAction(nameof(GetById), new { id = merchant.Id }, merchant.ToResponse());
    }

    [HttpGet("{id:long}")]
    [ProducesResponseType(typeof(MerchantResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(long id, CancellationToken ct)
    {
        var merchant = await _service.FindByIdAsync(id, ct);
        return merchant is not null
            ? Ok(merchant.ToResponse())
            : NotFound();
    }
}
```

### FluentValidation

```csharp
public class CreateMerchantRequestValidator : AbstractValidator<CreateMerchantRequest>
{
    public CreateMerchantRequestValidator()
    {
        RuleFor(x => x.Mid)
            .NotEmpty()
            .MaximumLength(15)
            .Matches(@"^\d+$").WithMessage("MID must contain only digits");

        RuleFor(x => x.Name)
            .NotEmpty()
            .MaximumLength(100);

        RuleFor(x => x.Type)
            .IsInEnum()
            .WithMessage("Type must be Physical or Online");
    }
}

// Registration
builder.Services.AddValidatorsFromAssemblyContaining<CreateMerchantRequestValidator>();
```

### ProblemDetails for RFC 7807

```csharp
builder.Services.AddProblemDetails(options =>
{
    options.CustomizeProblemDetails = context =>
    {
        context.ProblemDetails.Extensions["traceId"] =
            context.HttpContext.TraceIdentifier;
    };
});

// Global exception handler middleware
app.UseExceptionHandler(errorApp =>
{
    errorApp.Run(async context =>
    {
        var exception = context.Features.Get<IExceptionHandlerFeature>()?.Error;
        var logger = context.RequestServices.GetRequiredService<ILogger<Program>>();
        logger.LogError(exception, "Unhandled exception");

        var problemDetails = exception switch
        {
            MerchantNotFoundException ex => new ProblemDetails
            {
                Type = "https://api.example.com/errors/not-found",
                Title = "Merchant Not Found",
                Status = StatusCodes.Status404NotFound,
                Detail = ex.Message,
                Instance = context.Request.Path
            },
            DuplicateMerchantException ex => new ProblemDetails
            {
                Type = "https://api.example.com/errors/conflict",
                Title = "Duplicate Merchant",
                Status = StatusCodes.Status409Conflict,
                Detail = ex.Message,
                Instance = context.Request.Path
            },
            _ => new ProblemDetails
            {
                Type = "https://api.example.com/errors/internal",
                Title = "Internal Server Error",
                Status = StatusCodes.Status500InternalServerError,
                Detail = "An unexpected error occurred",
                Instance = context.Request.Path
            }
        };

        context.Response.StatusCode = problemDetails.Status ?? 500;
        await context.Response.WriteAsJsonAsync(problemDetails);
    });
});
```
