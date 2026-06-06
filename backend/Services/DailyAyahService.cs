using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using DailyAyah.Api.Abstractions;
using DailyAyah.Api.Config;
using DailyAyah.Api.Models;

namespace DailyAyah.Api.Services;

public sealed class DailyAyahService
{
    private readonly IDiyanetScraper _scraper;
    private readonly TimeZoneInfo _turkeyTimeZone;
    private readonly SemaphoreSlim _refreshLock = new(1, 1);
    private readonly object _stateLock = new();

    private DailyAyahRecord? _current;
    private DailyAyahRecord? _lastSuccess;
    private readonly List<DailyAyahRecord> _history = [];

    public DailyAyahService(IDiyanetScraper scraper)
    {
        _scraper = scraper;
        _turkeyTimeZone = TimeZoneInfo.FindSystemTimeZoneById(AppConstants.TurkeyTimeZone);
    }

    public async Task<DailyAyahApiResponse> RefreshAsync(bool force = false, CancellationToken cancellationToken = default)
    {
        var todayTR = GetTurkeyDateIso(DateTimeOffset.UtcNow);

        lock (_stateLock)
        {
            if (!force && _current is not null && _current.PublishedDateTR == todayTR)
            {
                return ToApiResponse(_current, isStale: false);
            }
        }

        await _refreshLock.WaitAsync(cancellationToken);
        try
        {
            todayTR = GetTurkeyDateIso(DateTimeOffset.UtcNow);

            lock (_stateLock)
            {
                if (!force && _current is not null && _current.PublishedDateTR == todayTR)
                {
                    return ToApiResponse(_current, isStale: false);
                }
            }

            try
            {
                var scraped = await _scraper.FetchDailyAyahAsync(cancellationToken);
                var nowUtc = DateTimeOffset.UtcNow;
                var record = BuildRecord(scraped, nowUtc);

                lock (_stateLock)
                {
                    _current = record;
                    _lastSuccess = record;
                    UpsertHistory(record);
                }

                return ToApiResponse(record, isStale: false);
            }
            catch
            {
                lock (_stateLock)
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
                }

                throw;
            }
        }
        finally
        {
            _refreshLock.Release();
        }
    }

    public Task<DailyAyahApiResponse> GetDailyAyahAsync(CancellationToken cancellationToken = default)
    {
        var todayTR = GetTurkeyDateIso(DateTimeOffset.UtcNow);

        lock (_stateLock)
        {
            if (_current is not null && _current.PublishedDateTR == todayTR)
            {
                return Task.FromResult(ToApiResponse(_current, isStale: false));
            }
        }

        return RefreshAsync(force: false, cancellationToken);
    }

    public IReadOnlyList<DailyAyahRecord> GetHistory(int days = 7)
    {
        var normalized = Math.Clamp(days, 1, 30);

        lock (_stateLock)
        {
            return _history.Take(normalized).ToArray();
        }
    }

    public ServiceSnapshot GetSnapshot()
    {
        lock (_stateLock)
        {
            return new ServiceSnapshot(_current is not null, _current?.FetchedAt);
        }
    }

    private DailyAyahRecord BuildRecord(ScrapedAyah scraped, DateTimeOffset nowUtc)
    {
        var publishedDateTR = GetTurkeyDateIso(nowUtc);
        var fetchedAt = nowUtc.UtcDateTime.ToString("O", CultureInfo.InvariantCulture);
        var fingerprint = $"{scraped.Text}|{scraped.Reference}|{publishedDateTR}";

        return new DailyAyahRecord(
            scraped.Text,
            scraped.Reference,
            AppConstants.DefaultSource,
            publishedDateTR,
            fetchedAt,
            ComputeSha256(fingerprint)
        );
    }

    private void UpsertHistory(DailyAyahRecord record)
    {
        _history.RemoveAll(item => item.PublishedDateTR == record.PublishedDateTR);
        _history.Insert(0, record);

        if (_history.Count > 30)
        {
            _history.RemoveRange(30, _history.Count - 30);
        }
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
