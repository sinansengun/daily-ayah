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
    private readonly ILogger<DailyAyahService> _logger;
    private readonly TimeZoneInfo _turkeyTimeZone;

    private DailyAyahRecord? _current;
    private DailyAyahRecord? _lastSuccess;
    private string? _scrapedDateTR;
    private IReadOnlyList<DailyAyahRecord> _history = [];

    public DailyAyahService(IDiyanetScraper scraper, IDailyAyahRecordStore store, ILogger<DailyAyahService> logger)
    {
        _scraper = scraper;
        _store = store;
        _logger = logger;
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
        catch (Exception ex) when (!cancellationToken.IsCancellationRequested)
        {
            _logger.LogError(ex, "Daily ayah scrape failed.");
            return await ApplyFallbackAsync(todayTR, cancellationToken);
        }

        var nowUtc = DateTimeOffset.UtcNow;
        var record = BuildRecord(scraped, nowUtc);
        if (_store.IsConfigured)
        {
            var recentRecords = await _store.GetHistoryAsync(30, cancellationToken);
            record = SelectFallbackForDuplicate(record, recentRecords, nowUtc);
        }
        await _store.UpsertAsync(record, cancellationToken);

        _current = record;
        _lastSuccess = record;
        _scrapedDateTR = record.PublishedDateTR;
        UpsertHistory(record);

        return ToApiResponse(record, isStale: false);
    }

    public bool HasScrapedRecordForToday()
    {
        return _scrapedDateTR == GetTurkeyDateIso(DateTimeOffset.UtcNow);
    }

    private async Task<DailyAyahApiResponse> ApplyFallbackAsync(string todayTR, CancellationToken cancellationToken)
    {
        if (_current is not null && _current.PublishedDateTR == todayTR)
        {
            _logger.LogWarning("Scrape failed; keeping existing record for {PublishedDateTR} ({Reference}).", todayTR, _current.Reference);
            return ToApiResponse(_current, isStale: false);
        }

        var recent = _store.IsConfigured
            ? await _store.GetHistoryAsync(30, cancellationToken)
            : _history;
        var latest = recent.FirstOrDefault();
        var candidates = recent
            .Where(record => latest is null || !string.Equals(record.Text, latest.Text, StringComparison.Ordinal))
            .ToArray();
        if (candidates.Length == 0)
        {
            candidates = recent.ToArray();
        }

        if (candidates.Length == 0)
        {
            _logger.LogError("Daily ayah fallback unavailable: no history records; serving stale data if any.");
            return GetStaleFallbackOrThrow();
        }

        var nowUtc = DateTimeOffset.UtcNow;
        var record = RestampForToday(candidates[Random.Shared.Next(candidates.Length)], nowUtc);
        await _store.UpsertAsync(record, cancellationToken);

        _current = record;
        UpsertHistory(record);

        _logger.LogError(
            "Daily ayah fallback applied for {PublishedDateTR} using {Reference}.",
            record.PublishedDateTR,
            record.Reference);

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
        var tafsirReference = AyahReferenceParser.Parse(scraped.Reference);

        return new DailyAyahRecord(
            scraped.Text,
            scraped.Reference,
            tafsirReference.SurahNumber,
            tafsirReference.AyahNumber,
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

    private DailyAyahRecord SelectFallbackForDuplicate(
        DailyAyahRecord scrapedRecord,
        IReadOnlyList<DailyAyahRecord> recentRecords,
        DateTimeOffset nowUtc)
    {
        var isDuplicate = recentRecords.Any(record =>
            string.Equals(record.Text, scrapedRecord.Text, StringComparison.Ordinal) &&
            string.Equals(record.Reference, scrapedRecord.Reference, StringComparison.Ordinal));

        if (!isDuplicate)
        {
            return scrapedRecord;
        }

        var candidates = recentRecords
            .Where(record =>
                !string.Equals(record.Text, scrapedRecord.Text, StringComparison.Ordinal) ||
                !string.Equals(record.Reference, scrapedRecord.Reference, StringComparison.Ordinal))
            .ToArray();

        if (candidates.Length == 0)
        {
            return scrapedRecord;
        }

        return RestampForToday(candidates[Random.Shared.Next(candidates.Length)], nowUtc);
    }

    private DailyAyahRecord RestampForToday(DailyAyahRecord selected, DateTimeOffset nowUtc)
    {
        var publishedDateTR = GetTurkeyDateIso(nowUtc);
        var fetchedAt = nowUtc.UtcDateTime.ToString("O", CultureInfo.InvariantCulture);
        var fingerprint = $"{selected.Text}|{selected.Reference}|{selected.HadithText ?? string.Empty}|{selected.HadithReference ?? string.Empty}|{selected.DuaText ?? string.Empty}|{selected.DuaReference ?? string.Empty}|{publishedDateTR}";

        return selected with
        {
            PublishedDateTR = publishedDateTR,
            FetchedAt = fetchedAt,
            Hash = ComputeSha256(fingerprint)
        };
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
            record.SurahNumber,
            record.AyahNumber,
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
