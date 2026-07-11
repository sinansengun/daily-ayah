using Npgsql;

namespace DailyAyah.Api.Data;

public static class DailyAyahDatabaseConfig
{
    public static string? ResolveConnectionString(IConfiguration configuration)
    {
        var configured = configuration.GetConnectionString("DailyAyahDb")
            ?? configuration["DATABASE_URL"]
            ?? Environment.GetEnvironmentVariable("DATABASE_URL");

        if (string.IsNullOrWhiteSpace(configured))
        {
            return null;
        }

        if (!Uri.TryCreate(configured, UriKind.Absolute, out var uri)
            || (uri.Scheme != "postgres" && uri.Scheme != "postgresql"))
        {
            return configured;
        }

        var credentials = uri.UserInfo.Split(':', 2);
        var builder = new NpgsqlConnectionStringBuilder
        {
            Host = uri.Host,
            Port = uri.Port > 0 ? uri.Port : 5432,
            Database = uri.AbsolutePath.TrimStart('/'),
            Username = credentials.Length > 0 ? Uri.UnescapeDataString(credentials[0]) : string.Empty,
            Password = credentials.Length > 1 ? Uri.UnescapeDataString(credentials[1]) : string.Empty,
            SslMode = SslMode.Require
        };

        return builder.ConnectionString;
    }
}