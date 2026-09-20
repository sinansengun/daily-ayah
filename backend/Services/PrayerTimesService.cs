using System.Globalization;
using System.Text.Json;
using DailyAyah.Api.Config;
using DailyAyah.Api.Models;
using Microsoft.Extensions.Caching.Memory;

namespace DailyAyah.Api.Services;

public sealed class PrayerTimesService(HttpClient client, IMemoryCache cache)
{
    private const string Source = "AlAdhan - Diyanet Isleri Baskanligi method";
    private static readonly IReadOnlyDictionary<string, PrayerCity> Cities = new Dictionary<string, PrayerCity>(StringComparer.OrdinalIgnoreCase)
    {
        ["Istanbul"] = new("Istanbul", 41.0082, 28.9784),
        ["Ankara"] = new("Ankara", 39.9334, 32.8597),
        ["Izmir"] = new("Izmir", 38.4237, 27.1428),
        ["Bursa"] = new("Bursa", 40.1950, 29.0600),
        ["Antalya"] = new("Antalya", 36.8969, 30.7133)
    };

    public IReadOnlyCollection<string> SupportedCities => Cities.Keys.ToArray();

    public async Task<PrayerTimesResponse> GetAsync(string? city, CancellationToken cancellationToken)
    {
        var resolvedCity = Cities.TryGetValue(city?.Trim() ?? string.Empty, out var value)
            ? value
            : Cities["Istanbul"];
        var turkeyTimeZone = TimeZoneInfo.FindSystemTimeZoneById(AppConstants.TurkeyTimeZone);
        var today = TimeZoneInfo.ConvertTime(DateTimeOffset.UtcNow, turkeyTimeZone).Date;
        var cacheKey = $"prayer-times:{resolvedCity.Name}:{today:yyyy-MM-dd}";

        return await cache.GetOrCreateAsync(cacheKey, async entry =>
        {
            entry.AbsoluteExpirationRelativeToNow = TimeSpan.FromHours(6);
            var date = today.ToString("dd-MM-yyyy", CultureInfo.InvariantCulture);
            var requestUri = $"v1/timings/{date}?latitude={resolvedCity.Latitude.ToString(CultureInfo.InvariantCulture)}&longitude={resolvedCity.Longitude.ToString(CultureInfo.InvariantCulture)}&method=13&timezonestring={AppConstants.TurkeyTimeZone}";
            using var response = await client.GetAsync(requestUri, cancellationToken);
            response.EnsureSuccessStatusCode();

            await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
            using var document = await JsonDocument.ParseAsync(stream, cancellationToken: cancellationToken);
            var timings = document.RootElement.GetProperty("data").GetProperty("timings");

            return new PrayerTimesResponse(
                resolvedCity.Name,
                "Türkiye",
                today.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture),
                AppConstants.TurkeyTimeZone,
                ReadTime(timings, "Fajr"),
                ReadTime(timings, "Sunrise"),
                ReadTime(timings, "Dhuhr"),
                ReadTime(timings, "Asr"),
                ReadTime(timings, "Maghrib"),
                ReadTime(timings, "Isha"),
                Source,
                DateTimeOffset.UtcNow.ToString("O")
            );
        }) ?? throw new InvalidOperationException("Prayer times could not be loaded.");
    }

    private static string ReadTime(JsonElement timings, string name) => timings
        .GetProperty(name)
        .GetString()?
        .Split(' ', StringSplitOptions.RemoveEmptyEntries)[0] ?? string.Empty;

    private sealed record PrayerCity(string Name, double Latitude, double Longitude);
}