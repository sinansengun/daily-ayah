using DailyAyah.Api.Models;
using Microsoft.Extensions.Caching.Memory;

namespace DailyAyah.Api.Data;

public sealed class CachedTafsirAyahStore(ITafsirAyahStore inner, IMemoryCache cache) : ITafsirAyahStore
{
    private static readonly MemoryCacheEntryOptions CacheOptions = new()
    {
        AbsoluteExpirationRelativeToNow = TimeSpan.FromHours(12),
        SlidingExpiration = TimeSpan.FromHours(1)
    };

    public async Task<TafsirAyahResponse?> GetAsync(int surahNumber, int ayahNumber, CancellationToken cancellationToken = default)
    {
        var cacheKey = $"tafsir:{surahNumber}:{ayahNumber}";
        if (cache.TryGetValue(cacheKey, out TafsirAyahResponse? cached))
        {
            return cached;
        }

        var tafsir = await inner.GetAsync(surahNumber, ayahNumber, cancellationToken);
        if (tafsir is not null)
        {
            cache.Set(cacheKey, tafsir, CacheOptions);
        }

        return tafsir;
    }
}
