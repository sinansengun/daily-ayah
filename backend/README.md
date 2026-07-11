# Daily Ayah Backend (.NET)

This backend was migrated to ASP.NET Core minimal API.

## Requirements

- .NET SDK 10+

## Run

```bash
dotnet build DailyAyah.Api.csproj
dotnet run --project DailyAyah.Api.csproj --urls http://127.0.0.1:8787
```

## Endpoints

- `GET /health`
- `GET /daily-ayah`
- `GET /daily-ayah/history?days=7`

## Notes

- Scraper source: Diyanet homepage (`https://www.diyanet.gov.tr/tr-TR`)
- Scheduler refreshes once per day at 00:15 in `Europe/Istanbul`
- Crawled records are persisted to Postgres when `DATABASE_URL` or `ConnectionStrings__DailyAyahDb` is configured
- One row is stored per Turkey date (`published_date_tr`); repeated crawls for the same day update that row
- In-memory and database fallback returns stale data when source fetch fails

## Database

Production uses Railway Postgres. Add a Postgres service to the Railway project and expose its `DATABASE_URL` to the API service. On startup, the API creates the `daily_ayahs` table if it does not exist.

The table stores ayah, hadith, and dua as separate reference/text pairs:

- `ayah_reference`, `ayah_text`
- `hadith_reference`, `hadith_text`
- `dua_reference`, `dua_text`

For local development, either set Railway-style `DATABASE_URL` or a raw Npgsql connection string:

```bash
export ConnectionStrings__DailyAyahDb="Host=localhost;Port=5432;Database=daily_ayah;Username=postgres;Password=postgres"
dotnet run --project DailyAyah.Api.csproj --urls http://127.0.0.1:8787
```

If no database connection string is configured, the API still runs with in-memory data, but crawled records are not persisted.

## Railway Deploy

Project root contains `Dockerfile` and `railway.json` for Railway deployment.

1. Push this repository to GitHub.
2. In Railway, create a new project from that GitHub repo.
3. Add a Railway Postgres service and make sure `DATABASE_URL` is available to the API service.
4. Railway will build using the Dockerfile automatically.
5. After deploy, verify `GET /health` on your Railway domain.
