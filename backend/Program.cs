using DailyAyah.Api.Abstractions;
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

app.MapGet("/daily-ayah/history", (int? days, DailyAyahService service) =>
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

    return Results.Json(new
    {
        days = normalizedDays,
        items = service.GetHistory(normalizedDays)
    });
});

using (var scope = app.Services.CreateScope())
{
    var service = scope.ServiceProvider.GetRequiredService<DailyAyahService>();

    try
    {
        await service.RefreshAsync(force: true, app.Lifetime.ApplicationStopping);
    }
    catch (Exception ex)
    {
        app.Logger.LogWarning(ex, "Initial fetch failed, service will retry on first request.");
    }
}

app.Run();
