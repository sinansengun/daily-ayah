using DailyAyah.Api.Models;
using Npgsql;

namespace DailyAyah.Api.Data;

public sealed class PostgresDailyAyahRecordStore(DailyAyahDatabaseOptions options) : IDailyAyahRecordStore
{
    public bool IsConfigured => options.IsConfigured;

    public async Task InitializeAsync(CancellationToken cancellationToken = default)
    {
        if (!IsConfigured)
        {
            return;
        }

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = """
            CREATE TABLE IF NOT EXISTS daily_ayahs (
                published_date_tr text PRIMARY KEY,
                ayah_reference text NOT NULL,
                ayah_text text NOT NULL,
                hadith_reference text NULL,
                hadith_text text NULL,
                dua_reference text NULL,
                dua_text text NULL,
                source text NOT NULL,
                fetched_at text NOT NULL,
                hash text NOT NULL,
                created_at_utc timestamptz NOT NULL DEFAULT now(),
                updated_at_utc timestamptz NOT NULL DEFAULT now()
            );

            CREATE INDEX IF NOT EXISTS ix_daily_ayahs_published_date_tr_desc
                ON daily_ayahs (published_date_tr DESC);
            """;

        await command.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task UpsertAsync(DailyAyahRecord record, CancellationToken cancellationToken = default)
    {
        if (!IsConfigured)
        {
            return;
        }

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = """
            INSERT INTO daily_ayahs (
                published_date_tr,
                ayah_reference,
                ayah_text,
                hadith_reference,
                hadith_text,
                dua_reference,
                dua_text,
                source,
                fetched_at,
                hash
            )
            VALUES (
                @published_date_tr,
                @ayah_reference,
                @ayah_text,
                @hadith_reference,
                @hadith_text,
                @dua_reference,
                @dua_text,
                @source,
                @fetched_at,
                @hash
            )
            ON CONFLICT (published_date_tr) DO UPDATE SET
                ayah_reference = EXCLUDED.ayah_reference,
                ayah_text = EXCLUDED.ayah_text,
                hadith_reference = EXCLUDED.hadith_reference,
                hadith_text = EXCLUDED.hadith_text,
                dua_reference = EXCLUDED.dua_reference,
                dua_text = EXCLUDED.dua_text,
                source = EXCLUDED.source,
                fetched_at = EXCLUDED.fetched_at,
                hash = EXCLUDED.hash,
                updated_at_utc = now();
            """;

        AddRecordParameters(command, record);
        await command.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task<DailyAyahRecord?> GetLatestAsync(CancellationToken cancellationToken = default)
    {
        if (!IsConfigured)
        {
            return null;
        }

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = $"""
            {SelectColumnsSql}
            ORDER BY published_date_tr DESC
            LIMIT 1;
            """;

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        return await reader.ReadAsync(cancellationToken) ? ReadRecord(reader) : null;
    }

    public async Task<IReadOnlyList<DailyAyahRecord>> GetHistoryAsync(int days, CancellationToken cancellationToken = default)
    {
        if (!IsConfigured)
        {
            return Array.Empty<DailyAyahRecord>();
        }

        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = $"""
            {SelectColumnsSql}
            ORDER BY published_date_tr DESC
            LIMIT @days;
            """;
        command.Parameters.AddWithValue("days", days);

        var records = new List<DailyAyahRecord>();
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            records.Add(ReadRecord(reader));
        }

        return records;
    }

    private const string SelectColumnsSql = """
        SELECT
            ayah_text,
            ayah_reference,
            hadith_text,
            hadith_reference,
            dua_text,
            dua_reference,
            source,
            published_date_tr,
            fetched_at,
            hash
        FROM daily_ayahs
        """;

    private async Task<NpgsqlConnection> OpenConnectionAsync(CancellationToken cancellationToken)
    {
        var connection = new NpgsqlConnection(options.ConnectionString);
        await connection.OpenAsync(cancellationToken);
        return connection;
    }

    private static void AddRecordParameters(NpgsqlCommand command, DailyAyahRecord record)
    {
        command.Parameters.AddWithValue("published_date_tr", record.PublishedDateTR);
        command.Parameters.AddWithValue("ayah_reference", record.Reference);
        command.Parameters.AddWithValue("ayah_text", record.Text);
        command.Parameters.AddWithValue("hadith_reference", DbValue(record.HadithReference));
        command.Parameters.AddWithValue("hadith_text", DbValue(record.HadithText));
        command.Parameters.AddWithValue("dua_reference", DbValue(record.DuaReference));
        command.Parameters.AddWithValue("dua_text", DbValue(record.DuaText));
        command.Parameters.AddWithValue("source", record.Source);
        command.Parameters.AddWithValue("fetched_at", record.FetchedAt);
        command.Parameters.AddWithValue("hash", record.Hash);
    }

    private static DailyAyahRecord ReadRecord(NpgsqlDataReader reader)
    {
        return new DailyAyahRecord(
            reader.GetString(reader.GetOrdinal("ayah_text")),
            reader.GetString(reader.GetOrdinal("ayah_reference")),
            GetNullableString(reader, "hadith_text"),
            GetNullableString(reader, "hadith_reference"),
            GetNullableString(reader, "dua_text"),
            GetNullableString(reader, "dua_reference"),
            reader.GetString(reader.GetOrdinal("source")),
            reader.GetString(reader.GetOrdinal("published_date_tr")),
            reader.GetString(reader.GetOrdinal("fetched_at")),
            reader.GetString(reader.GetOrdinal("hash"))
        );
    }

    private static string? GetNullableString(NpgsqlDataReader reader, string name)
    {
        var ordinal = reader.GetOrdinal(name);
        return reader.IsDBNull(ordinal) ? null : reader.GetString(ordinal);
    }

    private static object DbValue(string? value)
    {
        return string.IsNullOrWhiteSpace(value) ? DBNull.Value : value;
    }
}