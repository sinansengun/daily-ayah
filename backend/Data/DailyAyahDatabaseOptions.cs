namespace DailyAyah.Api.Data;

public sealed record DailyAyahDatabaseOptions(string? ConnectionString)
{
    public bool IsConfigured => !string.IsNullOrWhiteSpace(ConnectionString);
}