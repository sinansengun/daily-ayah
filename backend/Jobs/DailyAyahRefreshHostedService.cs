using DailyAyah.Api.Config;
using DailyAyah.Api.Services;

namespace DailyAyah.Api.Jobs;

public sealed class DailyAyahRefreshHostedService(
    DailyAyahService service,
    ILogger<DailyAyahRefreshHostedService> logger
) : BackgroundService
{
    private static readonly TimeSpan RetryInterval = TimeSpan.FromHours(1);
    private readonly TimeZoneInfo _turkeyTimeZone = TimeZoneInfo.FindSystemTimeZoneById(AppConstants.TurkeyTimeZone);

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var retrying = !service.HasScrapedRecordForToday();

        while (!stoppingToken.IsCancellationRequested)
        {
            var delay = GetDelayUntilNextRefresh(DateTimeOffset.UtcNow);
            if (retrying && RetryInterval < delay)
            {
                delay = RetryInterval;
            }
            logger.LogInformation("Next daily ayah refresh in {Delay}.", delay);

            try
            {
                await Task.Delay(delay, stoppingToken);
            }
            catch (TaskCanceledException)
            {
                break;
            }

            try
            {
                await service.RefreshAsync(force: true, stoppingToken);
            }
            catch (Exception ex) when (!stoppingToken.IsCancellationRequested)
            {
                logger.LogError(ex, "Scheduled daily ayah refresh failed.");
            }

            retrying = !service.HasScrapedRecordForToday();
            if (retrying)
            {
                logger.LogWarning("Daily ayah not scraped for today yet; retrying in {RetryInterval}.", RetryInterval);
            }
            else
            {
                logger.LogInformation("Daily ayah refreshed by scheduler.");
            }
        }
    }

    private TimeSpan GetDelayUntilNextRefresh(DateTimeOffset nowUtc)
    {
        var nowTr = TimeZoneInfo.ConvertTime(nowUtc, _turkeyTimeZone);
        var runTime = new TimeSpan(0, 1, 0);
        var runDate = nowTr.TimeOfDay >= runTime ? nowTr.Date.AddDays(1) : nowTr.Date;
        var scheduled = DateTime.SpecifyKind(runDate + runTime, DateTimeKind.Unspecified);

        var nextRunUtc = TimeZoneInfo.ConvertTimeToUtc(scheduled, _turkeyTimeZone);
        var delay = nextRunUtc - nowUtc.UtcDateTime;

        return delay <= TimeSpan.Zero ? TimeSpan.FromMinutes(1) : delay;
    }
}
