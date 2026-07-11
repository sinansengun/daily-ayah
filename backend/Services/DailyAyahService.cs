using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using DailyAyah.Api.Abstractions;
using DailyAyah.Api.Config;
using DailyAyah.Api.Data;
using DailyAyah.Api.Models;

namespace DailyAyah.Api.Services;

public sealed class DailyAyahService
{
    private readonly IDiyanetScraper _scraper;
    private readonly IDailyAyahRecordStore _store;
    private readonly TimeZoneInfo _turkeyTimeZone;

    private DailyAyahRecord? _current;
    private DailyAyahRecord? _lastSuccess;
    private IReadOnlyList<DailyAyahRecord> _history = [];

    public DailyAyahService(IDiyanetScraper scraper, IDailyAyahRecordStore store)
    {
        _scraper = scraper;
        _store = store;
        _turkeyTimeZone = TimeZoneInfo.FindSystemTimeZoneById(AppConstants.TurkeyTimeZone);
    }

    public async Task InitializeFromStoreAsync(CancellationToken cancellationToken = default)
    {
        if (!_store.IsConfigured)
        {
            return;
        }

        var latest = await _store.GetLatestAsync(cancellationToken);
        var history = await _store.GetHistoryAsync(30, cancellationToken);
        var todayTR = GetTurkeyDateIso(DateTimeOffset.UtcNow);

        _history = history;

        if (latest is null)
        {
            return;
        }

        _lastSuccess = latest;
        if (latest.PublishedDateTR == todayTR)
        {
            _current = latest;
        }
    }

    public async Task<DailyAyahApiResponse> RefreshAsync(bool force = false, CancellationToken cancellationToken = default)
    {
        var todayTR = GetTurkeyDateIso(DateTimeOffset.UtcNow);

        if (!force && _current is not null && _current.PublishedDateTR == todayTR)
        {
            return ToApiResponse(_current, isStale: false);
        }

        ScrapedAyah scraped;
        try
        {
            scraped = await _scraper.FetchDailyAyahAsync(cancellationToken);
        }
        catch
        {
            return GetStaleFallbackOrThrow();
        }

        var nowUtc = DateTimeOffset.UtcNow;
        var record = BuildRecord(scraped, nowUtc);
        await _store.UpsertAsync(record, cancellationToken);

        _current = record;
        _lastSuccess = record;
        UpsertHistory(record);

        return ToApiResponse(record, isStale: false);
    }

    public Task<DailyAyahApiResponse> GetDailyAyahAsync(CancellationToken cancellationToken = default)
    {
        var todayTR = GetTurkeyDateIso(DateTimeOffset.UtcNow);

        if (_current is not null && _current.PublishedDateTR == todayTR)
        {
            return Task.FromResult(ToApiResponse(_current, isStale: false));
        }

        return Task.FromResult(GetStaleFallbackOrThrow());
    }

    public async Task<IReadOnlyList<DailyAyahRecord>> GetHistoryAsync(int days = 7, CancellationToken cancellationToken = default)
    {
        var normalized = Math.Clamp(days, 1, 30);

        var stored = await _store.GetHistoryAsync(normalized, cancellationToken);
        if (stored.Count > 0)
        {
            return stored;
        }

        return _history.Take(normalized).ToArray();
    }

    public ServiceSnapshot GetSnapshot()
    {
        var latest = _current ?? _lastSuccess;
        return new ServiceSnapshot(latest is not null, latest?.FetchedAt);
    }

    private DailyAyahApiResponse GetStaleFallbackOrThrow()
    {
        if (_current is not null)
        {
            return ToApiResponse(_current, isStale: true);
        }

        if (_lastSuccess is not null)
        {
            _current = _lastSuccess;
            return ToApiResponse(_lastSuccess, isStale: true);
        }

        throw new InvalidOperationException("Unable to fetch daily ayah and no fallback record is available.");
    }

    private DailyAyahRecord BuildRecord(ScrapedAyah scraped, DateTimeOffset nowUtc)
    {
        var publishedDateTR = GetTurkeyDateIso(nowUtc);
        var fetchedAt = nowUtc.UtcDateTime.ToString("O", CultureInfo.InvariantCulture);
        var fingerprint = $"{scraped.Text}|{scraped.Reference}|{scraped.HadithText ?? string.Empty}|{scraped.HadithReference ?? string.Empty}|{scraped.DuaText ?? string.Empty}|{scraped.DuaReference ?? string.Empty}|{publishedDateTR}";

        return new DailyAyahRecord(
            scraped.Text,
            scraped.Reference,
            scraped.HadithText,
            scraped.HadithReference,
            scraped.DuaText,
            scraped.DuaReference,
            AppConstants.DefaultSource,
            publishedDateTR,
            fetchedAt,
            ComputeSha256(fingerprint)
        );
    }

    private void UpsertHistory(DailyAyahRecord record)
    {
        _history = _history
            .Where(item => item.PublishedDateTR != record.PublishedDateTR)
            .Prepend(record)
            .Take(30)
            .ToArray();
    }

    private string GetTurkeyDateIso(DateTimeOffset utcNow)
    {
        var turkeyNow = TimeZoneInfo.ConvertTime(utcNow, _turkeyTimeZone);
        return turkeyNow.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
    }

    private static DailyAyahApiResponse ToApiResponse(DailyAyahRecord record, bool isStale)
    {
        return new DailyAyahApiResponse(
            record.Text,
            record.Reference,
            record.HadithText,
            record.HadithReference,
            record.DuaText,
            record.DuaReference,
            record.Source,
            record.PublishedDateTR,
            record.FetchedAt,
            record.Hash,
            isStale
        );
    }

    private static string ComputeSha256(string input)
    {
        var hash = SHA256.HashData(Encoding.UTF8.GetBytes(input));
        return Convert.ToHexString(hash).ToLowerInvariant();
    }
}
