namespace DailyAyah.Api.Models;

public sealed record ScrapedAyah(
    string Text,
    string Reference,
    string? HadithText,
    string? HadithReference,
    string? DuaText,
    string? DuaReference
);

public sealed record DailyAyahRecord(
    string Text,
    string Reference,
    int? SurahNumber,
    int? AyahNumber,
    string? HadithText,
    string? HadithReference,
    string? DuaText,
    string? DuaReference,
    string Source,
    string PublishedDateTR,
    string FetchedAt,
    string Hash
);

public sealed record DailyAyahApiResponse(
    string Text,
    string Reference,
    int? SurahNumber,
    int? AyahNumber,
    string? HadithText,
    string? HadithReference,
    string? DuaText,
    string? DuaReference,
    string Source,
    string PublishedDateTR,
    string FetchedAt,
    string Hash,
    bool IsStale
);

public sealed record TafsirAyahResponse(
    int SurahNumber,
    string SurahName,
    int TotalAyahCount,
    int? MushafOrder,
    int? NuzulOrder,
    string? AboutText,
    int AyahNumber,
    int AyahRangeStart,
    int AyahRangeEnd,
    string? ArabicText,
    string MealText,
    string TafsirText,
    string? SourceReference,
    string SourceUrl
);

public sealed record ServiceSnapshot(bool HasData, string? LastFetchedAt);
