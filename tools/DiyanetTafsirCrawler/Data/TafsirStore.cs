using DiyanetTafsirCrawler.Models;
using Npgsql;

namespace DiyanetTafsirCrawler.Data;

public sealed class TafsirStore(string connectionString)
{
    public async Task<long> StartRunAsync(CrawlerOptions options, CancellationToken cancellationToken = default)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = """
            INSERT INTO tafsir_crawl_runs (dry_run, from_surah, to_surah)
            VALUES (@dry_run, @from_surah, @to_surah)
            RETURNING id;
            """;
        command.Parameters.AddWithValue("dry_run", options.DryRun);
        command.Parameters.AddWithValue("from_surah", options.FromSurah);
        command.Parameters.AddWithValue("to_surah", options.ToSurah);
        return (long)(await command.ExecuteScalarAsync(cancellationToken) ?? 0L);
    }

    public async Task FinishRunAsync(long runId, CrawlResult result, CancellationToken cancellationToken = default)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = """
            UPDATE tafsir_crawl_runs SET
                finished_at_utc = now(),
                surahs_seen = @surahs_seen,
                surahs_written = @surahs_written,
                ayahs_seen = @ayahs_seen,
                ayahs_written = @ayahs_written,
                ayahs_skipped = @ayahs_skipped,
                failed_surahs = @failed_surahs,
                failed_ayahs = @failed_ayahs,
                error_summary = @error_summary
            WHERE id = @id;
            """;
        command.Parameters.AddWithValue("id", runId);
        command.Parameters.AddWithValue("surahs_seen", result.SurahsSeen);
        command.Parameters.AddWithValue("surahs_written", result.SurahsWritten);
        command.Parameters.AddWithValue("ayahs_seen", result.AyahsSeen);
        command.Parameters.AddWithValue("ayahs_written", result.AyahsWritten);
        command.Parameters.AddWithValue("ayahs_skipped", result.AyahsSkipped);
        command.Parameters.AddWithValue("failed_surahs", result.FailedSurahs);
        command.Parameters.AddWithValue("failed_ayahs", result.FailedAyahs);
        command.Parameters.AddWithValue("error_summary", DbValue(result.Errors.Count == 0 ? null : string.Join(Environment.NewLine, result.Errors)));
        await command.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task<bool> AyahExistsAsync(int surahNumber, int ayahNumber, CancellationToken cancellationToken = default)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = """
            SELECT EXISTS (
                SELECT 1 FROM tafsir_ayahs
                WHERE surah_number = @surah_number AND ayah_number = @ayah_number
            );
            """;
        command.Parameters.AddWithValue("surah_number", surahNumber);
        command.Parameters.AddWithValue("ayah_number", ayahNumber);
        return (bool)(await command.ExecuteScalarAsync(cancellationToken) ?? false);
    }

    public async Task<IReadOnlySet<int>> GetExistingAyahNumbersAsync(int surahNumber, CancellationToken cancellationToken = default)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = """
            SELECT ayah_number
            FROM tafsir_ayahs
            WHERE surah_number = @surah_number;
            """;
        command.Parameters.AddWithValue("surah_number", surahNumber);

        var numbers = new HashSet<int>();
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            numbers.Add(reader.GetInt32(0));
        }

        return numbers;
    }

    public async Task<int> DeleteAyahsInSurahRangeAsync(int fromSurah, int toSurah, CancellationToken cancellationToken = default)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = """
            DELETE FROM tafsir_ayahs
            WHERE surah_number BETWEEN @from_surah AND @to_surah;
            """;
        command.Parameters.AddWithValue("from_surah", fromSurah);
        command.Parameters.AddWithValue("to_surah", toSurah);
        return await command.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task<bool> UpsertSurahAsync(TafsirSurah surah, CancellationToken cancellationToken = default)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);
        var existingHash = await GetExistingHashAsync(connection, "tafsir_surahs", "surah_number = @surah_number", command =>
        {
            command.Parameters.AddWithValue("surah_number", surah.SurahNumber);
        }, cancellationToken);

        if (existingHash == surah.ContentHash)
        {
            return false;
        }

        await using var upsert = connection.CreateCommand();
        upsert.CommandText = """
            INSERT INTO tafsir_surahs (
                surah_number, name, slug, total_ayah_count, mushaf_order, nuzul_order,
                about_text, nuzul_text, subject_text, virtue_text, source_url, content_hash
            )
            VALUES (
                @surah_number, @name, @slug, @total_ayah_count, @mushaf_order, @nuzul_order,
                @about_text, @nuzul_text, @subject_text, @virtue_text, @source_url, @content_hash
            )
            ON CONFLICT (surah_number) DO UPDATE SET
                name = EXCLUDED.name,
                slug = EXCLUDED.slug,
                total_ayah_count = EXCLUDED.total_ayah_count,
                mushaf_order = EXCLUDED.mushaf_order,
                nuzul_order = EXCLUDED.nuzul_order,
                about_text = EXCLUDED.about_text,
                nuzul_text = EXCLUDED.nuzul_text,
                subject_text = EXCLUDED.subject_text,
                virtue_text = EXCLUDED.virtue_text,
                source_url = EXCLUDED.source_url,
                content_hash = EXCLUDED.content_hash,
                updated_at_utc = now();
            """;
        upsert.Parameters.AddWithValue("surah_number", surah.SurahNumber);
        upsert.Parameters.AddWithValue("name", surah.Name);
        upsert.Parameters.AddWithValue("slug", surah.Slug);
        upsert.Parameters.AddWithValue("total_ayah_count", surah.TotalAyahCount);
        upsert.Parameters.AddWithValue("mushaf_order", DbValue(surah.MushafOrder));
        upsert.Parameters.AddWithValue("nuzul_order", DbValue(surah.NuzulOrder));
        upsert.Parameters.AddWithValue("about_text", DbValue(surah.AboutText));
        upsert.Parameters.AddWithValue("nuzul_text", DbValue(surah.NuzulText));
        upsert.Parameters.AddWithValue("subject_text", DbValue(surah.SubjectText));
        upsert.Parameters.AddWithValue("virtue_text", DbValue(surah.VirtueText));
        upsert.Parameters.AddWithValue("source_url", surah.SourceUri.ToString());
        upsert.Parameters.AddWithValue("content_hash", surah.ContentHash);
        await upsert.ExecuteNonQueryAsync(cancellationToken);
        return true;
    }

    public async Task<SurahTableStats> GetSurahTableStatsAsync(CancellationToken cancellationToken = default)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);
        await using var command = connection.CreateCommand();
        command.CommandText = """
            SELECT
                count(*)::integer AS total_count,
                count(*) FILTER (WHERE NULLIF(btrim(about_text), '') IS NOT NULL)::integer AS about_count,
                min(surah_number) AS min_surah,
                max(surah_number) AS max_surah
            FROM tafsir_surahs;
            """;

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        if (!await reader.ReadAsync(cancellationToken))
        {
            return new SurahTableStats(0, 0, null, null);
        }

        return new SurahTableStats(
            reader.GetInt32(reader.GetOrdinal("total_count")),
            reader.GetInt32(reader.GetOrdinal("about_count")),
            reader.IsDBNull(reader.GetOrdinal("min_surah")) ? null : reader.GetInt32(reader.GetOrdinal("min_surah")),
            reader.IsDBNull(reader.GetOrdinal("max_surah")) ? null : reader.GetInt32(reader.GetOrdinal("max_surah")));
    }

    public sealed record SurahTableStats(int TotalCount, int AboutCount, int? MinSurah, int? MaxSurah);

    public async Task<bool> UpsertAyahAsync(TafsirAyah ayah, CancellationToken cancellationToken = default)
    {
        await using var connection = await OpenConnectionAsync(cancellationToken);
        var existingHash = await GetExistingHashAsync(connection, "tafsir_ayahs", "surah_number = @surah_number AND ayah_number = @ayah_number", command =>
        {
            command.Parameters.AddWithValue("surah_number", ayah.SurahNumber);
            command.Parameters.AddWithValue("ayah_number", ayah.AyahNumber);
        }, cancellationToken);

        if (existingHash == ayah.ContentHash)
        {
            return false;
        }

        await using var upsert = connection.CreateCommand();
        upsert.CommandText = """
            INSERT INTO tafsir_ayahs (
                surah_number, ayah_number, ayah_range_start, ayah_range_end, surah_name, arabic_text, meal_text,
                tafsir_text, source_reference, source_url, content_hash
            )
            VALUES (
                @surah_number, @ayah_number, @ayah_range_start, @ayah_range_end, @surah_name, @arabic_text, @meal_text,
                @tafsir_text, @source_reference, @source_url, @content_hash
            )
            ON CONFLICT (surah_number, ayah_number) DO UPDATE SET
                ayah_range_start = EXCLUDED.ayah_range_start,
                ayah_range_end = EXCLUDED.ayah_range_end,
                surah_name = EXCLUDED.surah_name,
                arabic_text = EXCLUDED.arabic_text,
                meal_text = EXCLUDED.meal_text,
                tafsir_text = EXCLUDED.tafsir_text,
                source_reference = EXCLUDED.source_reference,
                source_url = EXCLUDED.source_url,
                content_hash = EXCLUDED.content_hash,
                updated_at_utc = now();
            """;
        upsert.Parameters.AddWithValue("surah_number", ayah.SurahNumber);
        upsert.Parameters.AddWithValue("ayah_number", ayah.AyahNumber);
        upsert.Parameters.AddWithValue("ayah_range_start", ayah.AyahRangeStart);
        upsert.Parameters.AddWithValue("ayah_range_end", ayah.AyahRangeEnd);
        upsert.Parameters.AddWithValue("surah_name", ayah.SurahName);
        upsert.Parameters.AddWithValue("arabic_text", DbValue(ayah.ArabicText));
        upsert.Parameters.AddWithValue("meal_text", ayah.MealText);
        upsert.Parameters.AddWithValue("tafsir_text", ayah.TafsirText);
        upsert.Parameters.AddWithValue("source_reference", DbValue(ayah.SourceReference));
        upsert.Parameters.AddWithValue("source_url", ayah.SourceUri.ToString());
        upsert.Parameters.AddWithValue("content_hash", ayah.ContentHash);
        await upsert.ExecuteNonQueryAsync(cancellationToken);
        return true;
    }

    private async Task<NpgsqlConnection> OpenConnectionAsync(CancellationToken cancellationToken)
    {
        Exception? lastException = null;

        for (var attempt = 1; attempt <= 5; attempt++)
        {
            var connection = new NpgsqlConnection(connectionString);
            try
            {
                await connection.OpenAsync(cancellationToken);
                return connection;
            }
            catch (Exception ex) when (attempt < 5)
            {
                lastException = ex;
                await connection.DisposeAsync();
                await Task.Delay(TimeSpan.FromSeconds(attempt * 2), cancellationToken);
            }
            catch
            {
                await connection.DisposeAsync();
                throw;
            }
        }

        throw lastException ?? new InvalidOperationException("Could not open PostgreSQL connection.");
    }

    private static async Task<string?> GetExistingHashAsync(NpgsqlConnection connection, string table, string whereSql, Action<NpgsqlCommand> addParameters, CancellationToken cancellationToken)
    {
        await using var command = connection.CreateCommand();
        command.CommandText = $"SELECT content_hash FROM {table} WHERE {whereSql};";
        addParameters(command);
        return await command.ExecuteScalarAsync(cancellationToken) as string;
    }

    private static object DbValue(string? value)
    {
        return string.IsNullOrWhiteSpace(value) ? DBNull.Value : value;
    }

    private static object DbValue(int? value)
    {
        return value.HasValue ? value.Value : DBNull.Value;
    }
}