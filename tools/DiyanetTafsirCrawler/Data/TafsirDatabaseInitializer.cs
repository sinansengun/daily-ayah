using Npgsql;

namespace DiyanetTafsirCrawler.Data;

public sealed class TafsirDatabaseInitializer(string connectionString)
{
    public async Task InitializeAsync(CancellationToken cancellationToken = default)
    {
        await using var connection = new NpgsqlConnection(connectionString);
        await connection.OpenAsync(cancellationToken);

        await using var command = connection.CreateCommand();
        command.CommandText = """
            CREATE TABLE IF NOT EXISTS tafsir_surahs (
                surah_number integer PRIMARY KEY,
                name text NOT NULL,
                slug text NOT NULL,
                total_ayah_count integer NOT NULL,
                mushaf_order integer NULL,
                nuzul_order integer NULL,
                about_text text NULL,
                nuzul_text text NULL,
                subject_text text NULL,
                virtue_text text NULL,
                source_url text NOT NULL,
                content_hash text NOT NULL,
                created_at_utc timestamptz NOT NULL DEFAULT now(),
                updated_at_utc timestamptz NOT NULL DEFAULT now()
            );

            CREATE TABLE IF NOT EXISTS tafsir_ayahs (
                surah_number integer NOT NULL,
                ayah_number integer NOT NULL,
                ayah_range_start integer NOT NULL,
                ayah_range_end integer NOT NULL,
                surah_name text NOT NULL,
                arabic_text text NULL,
                meal_text text NOT NULL,
                tafsir_text text NOT NULL,
                source_reference text NULL,
                source_url text NOT NULL,
                content_hash text NOT NULL,
                created_at_utc timestamptz NOT NULL DEFAULT now(),
                updated_at_utc timestamptz NOT NULL DEFAULT now(),
                PRIMARY KEY (surah_number, ayah_number)
            );

            CREATE INDEX IF NOT EXISTS ix_tafsir_ayahs_surah_ayah
                ON tafsir_ayahs (surah_number, ayah_number);

            ALTER TABLE tafsir_ayahs
                ADD COLUMN IF NOT EXISTS ayah_range_start integer;

            ALTER TABLE tafsir_ayahs
                ADD COLUMN IF NOT EXISTS ayah_range_end integer;

            UPDATE tafsir_ayahs
            SET ayah_range_start = ayah_number
            WHERE ayah_range_start IS NULL;

            UPDATE tafsir_ayahs
            SET ayah_range_end = ayah_number
            WHERE ayah_range_end IS NULL;

            ALTER TABLE tafsir_ayahs
                ALTER COLUMN ayah_range_start SET NOT NULL;

            ALTER TABLE tafsir_ayahs
                ALTER COLUMN ayah_range_end SET NOT NULL;

            CREATE TABLE IF NOT EXISTS tafsir_crawl_runs (
                id bigserial PRIMARY KEY,
                started_at_utc timestamptz NOT NULL DEFAULT now(),
                finished_at_utc timestamptz NULL,
                dry_run boolean NOT NULL DEFAULT false,
                from_surah integer NOT NULL,
                to_surah integer NOT NULL,
                surahs_seen integer NOT NULL DEFAULT 0,
                surahs_written integer NOT NULL DEFAULT 0,
                ayahs_seen integer NOT NULL DEFAULT 0,
                ayahs_written integer NOT NULL DEFAULT 0,
                ayahs_skipped integer NOT NULL DEFAULT 0,
                failed_surahs integer NOT NULL DEFAULT 0,
                failed_ayahs integer NOT NULL DEFAULT 0,
                error_summary text NULL
            );
            """;

        await command.ExecuteNonQueryAsync(cancellationToken);
    }
}