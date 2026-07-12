using DiyanetTafsirCrawler;
using DiyanetTafsirCrawler.Data;
using DiyanetTafsirCrawler.Scraping;

var options = CrawlerOptions.Parse(args);
if (options.ShowHelp)
{
    Console.WriteLine(CrawlerOptions.HelpText);
    return 0;
}

try
{
    using var httpClient = new HttpClient
    {
        BaseAddress = new Uri("https://kuran.diyanet.gov.tr"),
        Timeout = TimeSpan.FromSeconds(45)
    };
    httpClient.DefaultRequestHeaders.UserAgent.ParseAdd("DailyAyahTafsirCrawler/1.0 (+https://github.com/sinansengun/daily-ayah)");

    var pageClient = new DiyanetPageClient(httpClient, options.Delay, options.MaxRetries);
    TafsirStore? store = null;

    if (!options.DryRun)
    {
        var connectionString = TafsirDatabaseConfig.ResolveConnectionString();
        if (string.IsNullOrWhiteSpace(connectionString))
        {
            Console.Error.WriteLine("DATABASE_URL or ConnectionStrings__DailyAyahDb must be set unless --dry-run is used.");
            return 2;
        }

        store = new TafsirStore(connectionString);
        await new TafsirDatabaseInitializer(connectionString).InitializeAsync();

        if (options.VerifySurahs)
        {
            var stats = await store.GetSurahTableStatsAsync();
            Console.WriteLine($"tafsir_surahs rows: {stats.TotalCount}");
            Console.WriteLine($"tafsir_surahs rows with about_text: {stats.AboutCount}");
            Console.WriteLine($"surah_number range: {stats.MinSurah?.ToString() ?? "-"}-{stats.MaxSurah?.ToString() ?? "-"}");
            return 0;
        }
    }

    if (options.VerifySurahs)
    {
        Console.Error.WriteLine("DATABASE_URL or ConnectionStrings__DailyAyahDb must be set for --verify-surahs.");
        return 2;
    }

    var crawler = new TafsirCrawler(
        new TafsirIndexScraper(pageClient),
        new TafsirSurahScraper(pageClient),
        new TafsirAyahScraper(pageClient),
        store,
        options);

    var result = await crawler.RunAsync();
    Console.WriteLine(result.ToSummary());
    return result.FailedAyahs == 0 && result.FailedSurahs == 0 ? 0 : 1;
}
catch (Exception ex)
{
    Console.Error.WriteLine(ex.Message);
    Console.Error.WriteLine(ex.StackTrace);
    return 1;
}