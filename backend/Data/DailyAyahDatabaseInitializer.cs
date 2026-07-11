namespace DailyAyah.Api.Data;

public sealed class DailyAyahDatabaseInitializer(IDailyAyahRecordStore store)
{
    public Task InitializeAsync(CancellationToken cancellationToken = default)
    {
        return store.InitializeAsync(cancellationToken);
    }
}