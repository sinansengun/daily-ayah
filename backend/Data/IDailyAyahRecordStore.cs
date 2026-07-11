using DailyAyah.Api.Models;

namespace DailyAyah.Api.Data;

public interface IDailyAyahRecordStore
{
    bool IsConfigured { get; }

    Task InitializeAsync(CancellationToken cancellationToken = default);

    Task UpsertAsync(DailyAyahRecord record, CancellationToken cancellationToken = default);

    Task<DailyAyahRecord?> GetLatestAsync(CancellationToken cancellationToken = default);

    Task<IReadOnlyList<DailyAyahRecord>> GetHistoryAsync(int days, CancellationToken cancellationToken = default);
}