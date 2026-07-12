using DiyanetTafsirCrawler.Data;
using DiyanetTafsirCrawler.Models;
using DiyanetTafsirCrawler.Scraping;

namespace DiyanetTafsirCrawler;

public sealed class TafsirCrawler(
    TafsirIndexScraper indexScraper,
    TafsirSurahScraper surahScraper,
    TafsirAyahScraper ayahScraper,
    TafsirStore? store,
    CrawlerOptions options)
{
    public async Task<CrawlResult> RunAsync(CancellationToken cancellationToken = default)
    {
        var result = new CrawlResult();
        long? runId = null;

        if (store is not null)
        {
            if (options.PurgeFromSurah is not null && options.PurgeToSurah is not null)
            {
                var deleted = await store.DeleteAyahsInSurahRangeAsync(options.PurgeFromSurah.Value, options.PurgeToSurah.Value, cancellationToken);
                Console.WriteLine($"Purged {deleted} ayah rows for surahs {options.PurgeFromSurah}-{options.PurgeToSurah}.");
            }

            runId = await store.StartRunAsync(options, cancellationToken);
            Console.WriteLine($"Started crawl run {runId}.");
        }

        try
        {
            var summaries = (await indexScraper.GetSurahsAsync(cancellationToken))
                .Where(summary => summary.SurahNumber >= options.FromSurah && summary.SurahNumber <= options.ToSurah)
                .OrderBy(summary => summary.SurahNumber)
                .ToArray();

            foreach (var summary in summaries)
            {
                await CrawlSurahAsync(summary, result, cancellationToken);
            }
        }
        finally
        {
            if (store is not null && runId is not null)
            {
                await store.FinishRunAsync(runId.Value, result, cancellationToken);
            }
        }

        return result;
    }

    private async Task CrawlSurahAsync(TafsirSurahSummary summary, CrawlResult result, CancellationToken cancellationToken)
    {
        TafsirSurah surah;
        try
        {
            Console.WriteLine($"Surah {summary.SurahNumber}: {summary.Name}");
            surah = await surahScraper.GetSurahAsync(summary, validateAyahLinks: !options.SurahsOnly, cancellationToken);
            result.SurahsSeen++;

            if (!options.DryRun && store is not null)
            {
                if (await store.UpsertSurahAsync(surah, cancellationToken))
                {
                    result.SurahsWritten++;
                }
            }
        }
        catch (Exception ex)
        {
            result.FailedSurahs++;
            result.Errors.Add($"Surah {summary.SurahNumber} failed: {ex.Message}");
            return;
        }

        var existingAyahNumbers = !options.DryRun && options.SkipExisting && store is not null
            ? await store.GetExistingAyahNumbersAsync(surah.SurahNumber, cancellationToken)
            : new HashSet<int>();

        if (options.SurahsOnly)
        {
            return;
        }

        foreach (var ayahLink in surah.AyahLinks)
        {
            await CrawlAyahAsync(surah, ayahLink, existingAyahNumbers, result, cancellationToken);
        }
    }

    private async Task CrawlAyahAsync(TafsirSurah surah, TafsirAyahLink ayahLink, IReadOnlySet<int> existingAyahNumbers, CrawlResult result, CancellationToken cancellationToken)
    {
        try
        {
            if (!options.DryRun && options.SkipExisting && store is not null && ayahLink.AyahNumbers.All(existingAyahNumbers.Contains))
            {
                result.AyahsSkipped += ayahLink.EndAyahNumber - ayahLink.StartAyahNumber + 1;
                return;
            }

            var ayahs = await ayahScraper.GetAyahsAsync(surah, ayahLink, cancellationToken);
            result.AyahsSeen += ayahs.Count;

            if (!options.DryRun && store is not null)
            {
                foreach (var ayah in ayahs)
                {
                    if (await store.UpsertAyahAsync(ayah, cancellationToken))
                    {
                        result.AyahsWritten++;
                    }
                    else
                    {
                        result.AyahsSkipped++;
                    }
                }
            }
        }
        catch (Exception ex)
        {
            result.FailedAyahs++;
            result.Errors.Add($"Ayah failed {ayahLink.SourceUri}: {ex.Message}");
        }
    }
}