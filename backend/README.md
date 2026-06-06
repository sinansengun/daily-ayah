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
- Scheduler refreshes at 00:05 and 12:05 in `Europe/Istanbul`
- In-memory fallback returns stale data when source fetch fails

## Railway Deploy

Project root contains `Dockerfile` and `railway.json` for Railway deployment.

1. Push this repository to GitHub.
2. In Railway, create a new project from that GitHub repo.
3. Railway will build using the Dockerfile automatically.
4. After deploy, verify `GET /health` on your Railway domain.
