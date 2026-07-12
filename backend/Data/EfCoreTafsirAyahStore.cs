using DailyAyah.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace DailyAyah.Api.Data;

public sealed class EfCoreTafsirAyahStore(DailyAyahDbContext dbContext) : ITafsirAyahStore
{
    public async Task<TafsirAyahResponse?> GetAsync(int surahNumber, int ayahNumber, CancellationToken cancellationToken = default)
    {
        return await (
            from ayah in dbContext.TafsirAyahs.AsNoTracking()
            join surah in dbContext.TafsirSurahs.AsNoTracking()
                on ayah.SurahNumber equals surah.SurahNumber into surahGroup
            from surah in surahGroup.DefaultIfEmpty()
            where ayah.SurahNumber == surahNumber && ayah.AyahNumber == ayahNumber
            select new TafsirAyahResponse(
                ayah.SurahNumber,
                surah == null ? ayah.SurahName : surah.Name,
                surah == null ? 0 : surah.TotalAyahCount,
                surah == null ? null : surah.MushafOrder,
                surah == null ? null : surah.NuzulOrder,
                surah == null ? null : surah.AboutText,
                ayah.AyahNumber,
                ayah.AyahRangeStart,
                ayah.AyahRangeEnd,
                ayah.ArabicText,
                ayah.MealText,
                ayah.TafsirText,
                ayah.SourceReference,
                ayah.SourceUrl))
            .FirstOrDefaultAsync(cancellationToken);
    }
}
