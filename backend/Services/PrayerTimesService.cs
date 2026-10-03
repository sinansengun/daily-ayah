using System.Globalization;
using System.Text.Json;
using System.Text.RegularExpressions;
using DailyAyah.Api.Config;
using DailyAyah.Api.Models;
using Microsoft.Extensions.Caching.Memory;

namespace DailyAyah.Api.Services;

public sealed class PrayerTimesService(HttpClient client, IMemoryCache cache)
{
    private const string Source = "Diyanet İşleri Başkanlığı";
    private const string LocationsPath = "assets/locations/TURKEY.json";
    private static readonly string[] DefaultCities = ["Istanbul", "Ankara", "Izmir", "Bursa", "Antalya"];

    public IReadOnlyCollection<string> SupportedCities => DefaultCities;

    public async Task<PrayerTimesResponse> GetAsync(string? city, CancellationToken cancellationToken)
    {
        var locations = await GetLocationsAsync(cancellationToken);
        var requestedCity = city?.Trim();
        var resolvedCity = locations.FirstOrDefault(location => NamesMatch(location.City, requestedCity))
            ?? locations.FirstOrDefault(location => NamesMatch(location.State, requestedCity))
            ?? locations.First(location => NamesMatch(location.City, "Istanbul"));
        var turkeyTimeZone = TimeZoneInfo.FindSystemTimeZoneById(AppConstants.TurkeyTimeZone);
        var today = TimeZoneInfo.ConvertTime(DateTimeOffset.UtcNow, turkeyTimeZone).Date;
        var cacheKey = $"prayer-times:{resolvedCity.CityID}:{today:yyyy-MM-dd}";

        return await cache.GetOrCreateAsync(cacheKey, async entry =>
        {
            entry.AbsoluteExpirationRelativeToNow = TimeSpan.FromHours(6);
            var requestUri = $"tr-TR/{resolvedCity.CityID}";
            using var response = await client.GetAsync(requestUri, cancellationToken);
            response.EnsureSuccessStatusCode();

            var page = await response.Content.ReadAsStringAsync(cancellationToken);

            return new PrayerTimesResponse(
                ToDisplayName(resolvedCity.City),
                "Türkiye",
                today.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture),
                AppConstants.TurkeyTimeZone,
                ReadTime(page, "imsak"),
                ReadTime(page, "gunes"),
                ReadTime(page, "ogle"),
                ReadTime(page, "ikindi"),
                ReadTime(page, "aksam"),
                ReadTime(page, "yatsi"),
                Source,
                DateTimeOffset.UtcNow.ToString("O")
            );
        }) ?? throw new InvalidOperationException("Prayer times could not be loaded.");
    }

    private async Task<IReadOnlyList<DiyanetLocation>> GetLocationsAsync(CancellationToken cancellationToken)
    {
        const string cacheKey = "diyanet-turkey-locations";
        return await cache.GetOrCreateAsync(cacheKey, async entry =>
        {
            entry.AbsoluteExpirationRelativeToNow = TimeSpan.FromDays(7);
            await using var stream = await client.GetStreamAsync(LocationsPath, cancellationToken);
            return await JsonSerializer.DeserializeAsync<List<DiyanetLocation>>(stream, cancellationToken: cancellationToken)
                ?? throw new InvalidOperationException("Diyanet district list could not be loaded.");
        }) ?? throw new InvalidOperationException("Diyanet district list could not be loaded.");
    }

    private static string ReadTime(string page, string key)
    {
        var match = Regex.Match(page, $"var _{key}Time = \\\"(?<time>\\d{{2}}:\\d{{2}})\\\";", RegexOptions.CultureInvariant);
        if (!match.Success)
        {
            throw new InvalidOperationException($"Diyanet page did not contain the {key} prayer time.");
        }

        return match.Groups["time"].Value;
    }

    private static bool NamesMatch(string value, string? candidate) =>
        candidate is not null && string.Equals(NormalizeName(value), NormalizeName(candidate), StringComparison.Ordinal);

    private static string NormalizeName(string value) => value
        .Trim()
        .ToUpperInvariant()
        .Replace('İ', 'I')
        .Replace('I', 'I')
        .Replace('Ç', 'C')
        .Replace('Ğ', 'G')
        .Replace('Ö', 'O')
        .Replace('Ş', 'S')
        .Replace('Ü', 'U');

    private static string ToDisplayName(string value) => CultureInfo.GetCultureInfo("tr-TR").TextInfo.ToTitleCase(value.ToLowerInvariant());

    private sealed record DiyanetLocation(string Country, string State, string City, int CityID);
}