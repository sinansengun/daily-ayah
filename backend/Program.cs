using DailyAyah.Api.Abstractions;
using DailyAyah.Api.Data;
using DailyAyah.Api.Jobs;
using DailyAyah.Api.Scraper;
using DailyAyah.Api.Services;

var builder = WebApplication.CreateBuilder(args);

var portFromEnv = Environment.GetEnvironmentVariable("PORT");
if (int.TryParse(portFromEnv, out var port) && port > 0)
{
    builder.WebHost.UseUrls($"http://0.0.0.0:{port}");
}

builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
});

builder.Services.AddHttpClient<IDiyanetScraper, DiyanetScraper>(client =>
{
    client.Timeout = TimeSpan.FromSeconds(15);
});

var databaseOptions = new DailyAyahDatabaseOptions(DailyAyahDatabaseConfig.ResolveConnectionString(builder.Configuration));
builder.Services.AddSingleton(databaseOptions);
builder.Services.AddSingleton<IDailyAyahRecordStore>(provider =>
{
    var options = provider.GetRequiredService<DailyAyahDatabaseOptions>();
    return options.IsConfigured
        ? new PostgresDailyAyahRecordStore(options)
        : new NoOpDailyAyahRecordStore();
});
builder.Services.AddSingleton<DailyAyahDatabaseInitializer>();
builder.Services.AddSingleton<DailyAyahService>();
builder.Services.AddHostedService<DailyAyahRefreshHostedService>();

var app = builder.Build();

app.UseExceptionHandler(errorApp =>
{
    errorApp.Run(async context =>
    {
        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        await context.Response.WriteAsJsonAsync(new
        {
            error = "InternalServerError",
            message = "Request failed while processing daily ayah."
        });
    });
});

app.MapGet("/health", (DailyAyahService service) =>
{
    var snapshot = service.GetSnapshot();

    return Results.Json(new
    {
        status = "ok",
        now = DateTimeOffset.UtcNow.ToString("O"),
        hasData = snapshot.HasData,
        lastFetchedAt = snapshot.LastFetchedAt
    });
});

app.MapGet("/daily-ayah", async (HttpContext context, DailyAyahService service, CancellationToken cancellationToken) =>
{
    var payload = await service.GetDailyAyahAsync(cancellationToken);
    context.Response.Headers.CacheControl = "public, max-age=300, stale-while-revalidate=3600";
    context.Response.Headers.ETag = payload.Hash;

    return Results.Json(payload);
});

app.MapGet("/daily-ayah/history", async (int? days, DailyAyahService service, CancellationToken cancellationToken) =>
{
    if (days is < 1 or > 30)
    {
        return Results.BadRequest(new
        {
            error = "ValidationError",
            message = "Query parameter 'days' must be between 1 and 30."
        });
    }

    var normalizedDays = days ?? 7;
    var items = await service.GetHistoryAsync(normalizedDays, cancellationToken);

    return Results.Json(new
    {
        days = normalizedDays,
        items
    });
});

using (var scope = app.Services.CreateScope())
{
    var initializer = scope.ServiceProvider.GetRequiredService<DailyAyahDatabaseInitializer>();
    var service = scope.ServiceProvider.GetRequiredService<DailyAyahService>();
    var database = scope.ServiceProvider.GetRequiredService<DailyAyahDatabaseOptions>();

    if (!database.IsConfigured)
    {
        app.Logger.LogWarning("Daily ayah database is not configured. Set DATABASE_URL or ConnectionStrings__DailyAyahDb to persist crawled records.");
    }

    try
    {
        await initializer.InitializeAsync(app.Lifetime.ApplicationStopping);
        await service.InitializeFromStoreAsync(app.Lifetime.ApplicationStopping);
        await service.RefreshAsync(force: true, app.Lifetime.ApplicationStopping);
    }
    catch (Exception ex)
    {
        app.Logger.LogWarning(ex, "Initial fetch failed, service will retry on first request.");
    }
}

app.Run();
