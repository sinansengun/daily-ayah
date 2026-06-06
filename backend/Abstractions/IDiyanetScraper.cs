using DailyAyah.Api.Models;

namespace DailyAyah.Api.Abstractions;

public interface IDiyanetScraper
{
    Task<ScrapedAyah> FetchDailyAyahAsync(CancellationToken cancellationToken = default);
}
