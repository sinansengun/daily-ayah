using DailyAyah.Api.Models;

namespace DailyAyah.Api.Data;

public interface ITafsirAyahStore
{
    Task<TafsirAyahResponse?> GetAsync(int surahNumber, int ayahNumber, CancellationToken cancellationToken = default);
}