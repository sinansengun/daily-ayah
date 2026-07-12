namespace DiyanetTafsirCrawler;

public sealed record CrawlerOptions(
    bool DryRun,
    int? Surah,
    int FromSurah,
    int ToSurah,
    bool SurahsOnly,
    bool VerifySurahs,
    bool SkipExisting,
    TimeSpan Delay,
    int MaxRetries,
    int? PurgeFromSurah,
    int? PurgeToSurah,
    bool ShowHelp)
{
    public static CrawlerOptions Parse(string[] args)
    {
        var dryRun = false;
        int? surah = null;
        var fromSurah = 1;
        var toSurah = 114;
        var surahsOnly = false;
        var verifySurahs = false;
        var skipExisting = false;
        var delayMs = 750;
        var maxRetries = 3;
        int? purgeFromSurah = null;
        int? purgeToSurah = null;
        var showHelp = false;

        for (var index = 0; index < args.Length; index++)
        {
            var arg = args[index];
            switch (arg)
            {
                case "--dry-run":
                    dryRun = true;
                    break;
                case "--skip-existing":
                    skipExisting = true;
                    break;
                case "--surahs-only":
                    surahsOnly = true;
                    break;
                case "--verify-surahs":
                    verifySurahs = true;
                    break;
                case "--help":
                case "-h":
                    showHelp = true;
                    break;
                case "--surah":
                    surah = ReadInt(args, ref index, arg);
                    break;
                case "--from-surah":
                    fromSurah = ReadInt(args, ref index, arg);
                    break;
                case "--to-surah":
                    toSurah = ReadInt(args, ref index, arg);
                    break;
                case "--delay-ms":
                    delayMs = ReadInt(args, ref index, arg);
                    break;
                case "--max-retries":
                    maxRetries = ReadInt(args, ref index, arg);
                    break;
                case "--purge-from-surah":
                    purgeFromSurah = ReadInt(args, ref index, arg);
                    break;
                case "--purge-to-surah":
                    purgeToSurah = ReadInt(args, ref index, arg);
                    break;
                default:
                    throw new ArgumentException($"Unknown argument: {arg}");
            }
        }

        if (surah is not null)
        {
            fromSurah = surah.Value;
            toSurah = surah.Value;
        }

        if (fromSurah is < 1 or > 114 || toSurah is < 1 or > 114 || fromSurah > toSurah)
        {
            throw new ArgumentException("Surah range must be between 1 and 114, and from-surah must be <= to-surah.");
        }

        if (delayMs < 0)
        {
            throw new ArgumentException("--delay-ms cannot be negative.");
        }

        if (maxRetries < 1)
        {
            throw new ArgumentException("--max-retries must be at least 1.");
        }

        if (purgeFromSurah is not null || purgeToSurah is not null)
        {
            purgeFromSurah ??= fromSurah;
            purgeToSurah ??= toSurah;

            if (purgeFromSurah is < 1 or > 114 || purgeToSurah is < 1 or > 114 || purgeFromSurah > purgeToSurah)
            {
                throw new ArgumentException("Purge surah range must be between 1 and 114, and purge-from-surah must be <= purge-to-surah.");
            }
        }

        return new CrawlerOptions(dryRun, surah, fromSurah, toSurah, surahsOnly, verifySurahs, skipExisting, TimeSpan.FromMilliseconds(delayMs), maxRetries, purgeFromSurah, purgeToSurah, showHelp);
    }

    public static string HelpText => """
        Diyanet Tafsir Crawler

        Usage:
          dotnet run --project tools/DiyanetTafsirCrawler -- [options]

        Options:
          --dry-run              Parse pages and print counters without writing to PostgreSQL.
          --surah <1-114>        Crawl a single surah.
          --from-surah <1-114>   First surah number to crawl. Default: 1.
          --to-surah <1-114>     Last surah number to crawl. Default: 114.
                    --surahs-only          Import only surah metadata without crawling ayah tafsir pages.
          --verify-surahs        Print tafsir_surahs row counts and exit.
          --skip-existing        Skip ayahs that already exist in PostgreSQL.
          --delay-ms <number>    Delay between HTTP requests. Default: 750.
          --max-retries <number> HTTP retry attempts per page. Default: 3.
                    --purge-from-surah <n> Delete imported ayah rows starting from this surah before crawling.
                    --purge-to-surah <n>   Delete imported ayah rows through this surah before crawling.
          --help                 Show this help text.
        """;

    private static int ReadInt(string[] args, ref int index, string name)
    {
        if (index + 1 >= args.Length || !int.TryParse(args[index + 1], out var value))
        {
            throw new ArgumentException($"{name} requires an integer value.");
        }

        index++;
        return value;
    }
}