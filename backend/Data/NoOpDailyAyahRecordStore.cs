using DailyAyah.Api.Models;

namespace DailyAyah.Api.Data;

public sealed class NoOpDailyAyahRecordStore : IDailyAyahRecordStore
{
    public bool IsConfigured => false;

    public Task InitializeAsync(CancellationToken cancellationToken = default) => Task.CompletedTask;

    public Task UpsertAsync(DailyAyahRecord record, CancellationToken cancellationToken = default) => Task.CompletedTask;

    public Task<DailyAyahRecord?> GetLatestAsync(CancellationToken cancellationToken = default) => Task.FromResult<DailyAyahRecord?>(null);

    public Task<IReadOnlyList<DailyAyahRecord>> GetHistoryAsync(int days, CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<DailyAyahRecord>>(Array.Empty<DailyAyahRecord>());
    }
}