namespace DailyAyah.Api.Models;

public sealed record ScrapedAyah(string Text, string Reference);

public sealed record DailyAyahRecord(
    string Text,
    string Reference,
    string Source,
    string PublishedDateTR,
    string FetchedAt,
    string Hash
);

public sealed record DailyAyahApiResponse(
    string Text,
    string Reference,
    string Source,
    string PublishedDateTR,
    string FetchedAt,
    string Hash,
    bool IsStale
);

public sealed record ServiceSnapshot(bool HasData, string? LastFetchedAt);
