# Diyanet Tefsir Crawler

This repository includes an independent console crawler for importing Diyanet Quran tafsir content into PostgreSQL. It is intentionally separate from the backend API so a large import cannot slow down API startup or runtime traffic.

## Project

```sh
dotnet run --project tools/DiyanetTafsirCrawler -- --help
```

The crawler reads from `https://kuran.diyanet.gov.tr/Tefsir`, then follows surah pages and ayah tafsir pages.

## Tables

The crawler creates these tables if they do not exist:

- `tafsir_surahs`
- `tafsir_ayahs`
- `tafsir_crawl_runs`

Ayahs are keyed by `(surah_number, ayah_number)`, so rerunning the crawler updates existing rows instead of creating duplicates.

## Connection

Set one of these environment variables before writing to PostgreSQL:

```sh
export DATABASE_URL='postgresql://USER:PASSWORD@HOST:PORT/DB'
```

or:

```sh
export ConnectionStrings__DailyAyahDb='Host=localhost;Port=5433;Database=dailyayah;Username=postgres;Password=postgres'
```

Do not commit live connection strings or `.env` files. The repo ignores `.env*` files.

## Safe Run Order

Start with dry runs and a single surah before running the full import.

```sh
dotnet run --project tools/DiyanetTafsirCrawler -- --dry-run --surah 1 --delay-ms 250
```

Then write only Fatiha:

```sh
dotnet run --project tools/DiyanetTafsirCrawler -- --surah 1 --delay-ms 750
```

Then a small range:

```sh
dotnet run --project tools/DiyanetTafsirCrawler -- --from-surah 1 --to-surah 3 --delay-ms 750
```

If only `tafsir_surahs` metadata needs to be backfilled or refreshed, run the surah-only mode. This writes surah rows, including `about_text`, without visiting every ayah tafsir page:

```sh
dotnet run --project tools/DiyanetTafsirCrawler -- --surahs-only --from-surah 1 --to-surah 114 --delay-ms 250
```

Finally the full crawl:

```sh
dotnet run --project tools/DiyanetTafsirCrawler -- --from-surah 1 --to-surah 114 --delay-ms 750
```

## Options

- `--dry-run`: parse pages and print counters without writing to PostgreSQL.
- `--surah 1`: crawl a single surah.
- `--from-surah 1 --to-surah 114`: crawl a range.
- `--surahs-only`: import only surah metadata without crawling ayah tafsir pages.
- `--skip-existing`: skip ayahs that already exist in PostgreSQL.
- `--delay-ms 750`: delay between HTTP requests.
- `--max-retries 3`: HTTP retry attempts per page.
- `--purge-from-surah 3 --purge-to-surah 114`: delete imported ayah rows in a surah range before crawling it again.

Diyanet sometimes uses one tafsir page for an ayah range, such as `3-4`. The crawler stores one row per ayah and keeps `ayah_range_start` / `ayah_range_end` so those grouped source pages remain traceable.

## Verification

After `--surah 1`, PostgreSQL should contain:

```sql
select count(*) from tafsir_surahs where surah_number = 1;
select count(*) from tafsir_ayahs where surah_number = 1;
```

Expected result: one surah row and seven ayah rows.

After a full crawl:

```sql
select count(*) from tafsir_surahs;
select count(*) from tafsir_ayahs;
select * from tafsir_crawl_runs order by id desc limit 1;
```

Expected surah count is 114. Ayah count should match the Diyanet pages parsed by the run; any failed URL is included in the crawl run summary and console output.

## Source Note

The imported text is sourced from Diyanet's Quran tafsir pages. Keep request rates low and preserve source attribution when exposing this data elsewhere.