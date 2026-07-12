using DailyAyah.Api.Models;

namespace DailyAyah.Api.Data;

public sealed class NoOpTafsirAyahStore : ITafsirAyahStore
{
    public Task<TafsirAyahResponse?> GetAsync(int surahNumber, int ayahNumber, CancellationToken cancellationToken = default)
    {
        return Task.FromResult<TafsirAyahResponse?>(null);
    }
}