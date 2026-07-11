using DailyAyah.Api.Config;
using DailyAyah.Api.Services;

namespace DailyAyah.Api.Jobs;

public sealed class DailyAyahRefreshHostedService(
    DailyAyahService service,
    ILogger<DailyAyahRefreshHostedService> logger
) : BackgroundService
{
    private readonly TimeZoneInfo _turkeyTimeZone = TimeZoneInfo.FindSystemTimeZoneById(AppConstants.TurkeyTimeZone);

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            var delay = GetDelayUntilNextRefresh(DateTimeOffset.UtcNow);
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
                logger.LogInformation("Daily ayah refreshed by scheduler.");
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Scheduled daily ayah refresh failed.");
            }
        }
    }

    private TimeSpan GetDelayUntilNextRefresh(DateTimeOffset nowUtc)
    {
        var nowTr = TimeZoneInfo.ConvertTime(nowUtc, _turkeyTimeZone);
        var scheduledToday = new DateTime(nowTr.Year, nowTr.Month, nowTr.Day, 0, 15, 0, DateTimeKind.Unspecified);

        if (nowTr.TimeOfDay >= new TimeSpan(0, 15, 0))
        {
            var tomorrow = nowTr.Date.AddDays(1);
            scheduledToday = new DateTime(tomorrow.Year, tomorrow.Month, tomorrow.Day, 0, 15, 0, DateTimeKind.Unspecified);
        }

        var nextRunUtc = TimeZoneInfo.ConvertTimeToUtc(scheduledToday, _turkeyTimeZone);
        var delay = nextRunUtc - nowUtc.UtcDateTime;

        return delay <= TimeSpan.Zero ? TimeSpan.FromMinutes(1) : delay;
    }
}
