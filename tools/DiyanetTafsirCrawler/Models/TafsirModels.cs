namespace DiyanetTafsirCrawler.Models;

public sealed record TafsirSurahSeed(
    int SurahNumber,
    string Name,
    string Slug,
    int TotalAyahCount)
{
    public Uri SourceUri => new($"https://kuran.diyanet.gov.tr/tefsir/sure/{SurahNumber}-{Slug}-suresi");
}

public sealed record TafsirSurahSummary(
    int SurahNumber,
    string Name,
    string Slug,
    int TotalAyahCount,
    Uri SourceUri);

public sealed record TafsirSurah(
    int SurahNumber,
    string Name,
    string Slug,
    int TotalAyahCount,
    int? MushafOrder,
    int? NuzulOrder,
    string? AboutText,
    string? NuzulText,
    string? SubjectText,
    string? VirtueText,
    Uri SourceUri,
    IReadOnlyList<TafsirAyahLink> AyahLinks)
{
    public string ContentHash => Hashing.HashText(string.Join('\n', [
        SurahNumber.ToString(),
        Name,
        Slug,
        TotalAyahCount.ToString(),
        MushafOrder?.ToString() ?? string.Empty,
        NuzulOrder?.ToString() ?? string.Empty,
        AboutText ?? string.Empty,
        NuzulText ?? string.Empty,
        SubjectText ?? string.Empty,
        VirtueText ?? string.Empty,
        SourceUri.ToString()
    ]));
}

public sealed record TafsirAyahLink(
    Uri SourceUri,
    int StartAyahNumber,
    int EndAyahNumber)
{
    public IEnumerable<int> AyahNumbers => Enumerable.Range(StartAyahNumber, EndAyahNumber - StartAyahNumber + 1);
}

public sealed record TafsirAyah(
    int SurahNumber,
    string SurahName,
    int AyahNumber,
    int AyahRangeStart,
    int AyahRangeEnd,
    string? ArabicText,
    string MealText,
    string TafsirText,
    string? SourceReference,
    Uri SourceUri)
{
    public string ContentHash => Hashing.HashText(string.Join('\n', [
        SurahNumber.ToString(),
        SurahName,
        AyahNumber.ToString(),
        AyahRangeStart.ToString(),
        AyahRangeEnd.ToString(),
        ArabicText ?? string.Empty,
        MealText,
        TafsirText,
        SourceReference ?? string.Empty,
        SourceUri.ToString()
    ]));
}