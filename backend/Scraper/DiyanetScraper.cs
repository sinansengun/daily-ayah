using System.Net;
using System.Text.Json;
using System.Text.RegularExpressions;
using DailyAyah.Api.Abstractions;
using DailyAyah.Api.Config;
using DailyAyah.Api.Models;

namespace DailyAyah.Api.Scraper;

public sealed class SourceNotUpdatedException(string message) : InvalidOperationException(message);

public sealed class DiyanetScraper(HttpClient httpClient) : IDiyanetScraper
{
    private const int MaxNetworkAttempts = 4;
    private const int MaxSourceNotUpdatedAttempts = 10;
    private static readonly Regex ReferenceRegex = new(@"\(([^()]{2,140})\)", RegexOptions.Compiled);
    private static readonly TimeZoneInfo TurkeyTimeZone = TimeZoneInfo.FindSystemTimeZoneById(AppConstants.TurkeyTimeZone);

    public async Task<ScrapedAyah> FetchDailyAyahAsync(CancellationToken cancellationToken = default)
    {
        for (var attempt = 1; ; attempt++)
        {
            try
            {
                return await FetchDailyContentOnceAsync(cancellationToken);
            }
            catch (HttpRequestException) when (attempt < MaxNetworkAttempts)
            {
                // Diyanet intermittently resets connections; retry with backoff.
                await Task.Delay(TimeSpan.FromSeconds(attempt * 2), cancellationToken);
            }
            catch (SourceNotUpdatedException) when (attempt < MaxSourceNotUpdatedAttempts)
            {
                // Right after midnight Diyanet may still serve yesterday's content.
                await Task.Delay(TimeSpan.FromSeconds(30), cancellationToken);
            }
        }
    }

    private async Task<ScrapedAyah> FetchDailyContentOnceAsync(CancellationToken cancellationToken)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, AppConstants.DiyanetDailyContentUrl);
        request.Headers.TryAddWithoutValidation("Accept", "application/json");
        request.Headers.TryAddWithoutValidation("User-Agent", "DiyanetDailyAyah/1.0 (+contact: admin@example.com)");

        using var response = await httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
        response.EnsureSuccessStatusCode();

        var json = await response.Content.ReadAsStringAsync(cancellationToken);
        var todayDayOfYear = TimeZoneInfo.ConvertTime(DateTimeOffset.UtcNow, TurkeyTimeZone).DayOfYear;
        return ParseDailyContentJson(json, todayDayOfYear);
    }

    public static ScrapedAyah ParseDailyContentJson(string json, int? expectedDayOfYear = null)
    {
        using var document = JsonDocument.Parse(json);
        if (expectedDayOfYear is not null &&
            document.RootElement.TryGetProperty("dayOfYear", out var dayOfYear) &&
            dayOfYear.ValueKind == JsonValueKind.Number &&
            dayOfYear.GetInt32() != expectedDayOfYear)
        {
            throw new SourceNotUpdatedException(
                $"Daily content is for day {dayOfYear.GetInt32()}, expected {expectedDayOfYear}.");
        }

        if (!document.RootElement.TryGetProperty("items", out var items) || items.ValueKind != JsonValueKind.Array)
        {
            throw new InvalidOperationException("Daily content JSON has no 'items' array.");
        }

        (string Text, string? Reference)? ayah = null, hadis = null, dua = null;
        foreach (var item in items.EnumerateArray())
        {
            var code = GetJsonString(item, "contentTypeCode")?.ToLowerInvariant();
            var text = NormalizeText(GetJsonString(item, "title") ?? string.Empty);
            if (string.IsNullOrWhiteSpace(text))
            {
                continue;
            }

            var referenceRaw = NormalizeText(GetJsonString(item, "source") ?? string.Empty);
            var reference = ExtractReference(referenceRaw);
            if (string.IsNullOrWhiteSpace(reference))
            {
                reference = NormalizeText(referenceRaw.Trim('(', ')'));
            }

            var entry = (text, string.IsNullOrWhiteSpace(reference) ? null : reference);
            switch (code)
            {
                case "ayet": ayah ??= entry; break;
                case "hadis": hadis ??= entry; break;
                case "dua": dua ??= entry; break;
            }
        }

        if (ayah is null || string.IsNullOrWhiteSpace(ayah.Value.Reference))
        {
            throw new InvalidOperationException("Daily content JSON has no ayah with a reference.");
        }

        return new ScrapedAyah(
            ayah.Value.Text,
            ayah.Value.Reference!,
            hadis?.Text,
            hadis?.Reference,
            dua?.Text,
            dua?.Reference
        );
    }

    private static string? GetJsonString(JsonElement element, string name)
    {
        return element.TryGetProperty(name, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()
            : null;
    }

    private static string NormalizeText(string value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return string.Empty;
        }

        var decoded = WebUtility.HtmlDecode(value);
        return Regex.Replace(decoded.Replace('\u00A0', ' '), @"\s+", " ").Trim();
    }

    private static string ExtractReference(string text)
    {
        var match = ReferenceRegex.Match(text);
        return match.Success ? NormalizeText(match.Groups[1].Value) : string.Empty;
    }
}
