using System.Text.RegularExpressions;
using DiyanetTafsirCrawler.Models;

namespace DiyanetTafsirCrawler.Scraping;

public sealed partial class TafsirIndexScraper(DiyanetPageClient pageClient)
{
    private static readonly Uri IndexUri = new("https://kuran.diyanet.gov.tr/Tefsir");

    public async Task<IReadOnlyList<TafsirSurahSummary>> GetSurahsAsync(CancellationToken cancellationToken = default)
    {
        var document = await pageClient.GetDocumentAsync(IndexUri, cancellationToken);
        var discovered = document.DocumentNode
            .SelectNodes("//a[contains(@href, '/tefsir/sure/')]")
            ?.Select(link => ToSummary(link.GetAttributeValue("href", string.Empty), HtmlText.Normalize(link.InnerText)))
            .Where(summary => summary is not null)
            .Select(summary => summary!)
            .GroupBy(summary => summary.SurahNumber)
            .ToDictionary(group => group.Key, group => group.First())
            ?? [];

        foreach (var seed in SurahSeeds.All)
        {
            discovered.TryAdd(seed.SurahNumber, new TafsirSurahSummary(
                seed.SurahNumber,
                seed.Name,
                seed.Slug,
                seed.TotalAyahCount,
                seed.SourceUri));
        }

        return discovered.Values.OrderBy(summary => summary.SurahNumber).ToArray();
    }

    private TafsirSurahSummary? ToSummary(string href, string label)
    {
        var match = SureHrefRegex().Match(href);
        if (!match.Success || !int.TryParse(match.Groups[1].Value, out var surahNumber))
        {
            return null;
        }

        var seed = SurahSeeds.All.FirstOrDefault(item => item.SurahNumber == surahNumber);
        var slug = match.Groups[2].Value;
        var name = seed?.Name ?? ExtractName(label, slug);
        var totalAyahCount = seed?.TotalAyahCount ?? ExtractTotalAyahCount(label);

        return new TafsirSurahSummary(
            surahNumber,
            name,
            slug,
            totalAyahCount,
            pageClient.ToAbsoluteUri(href));
    }

    private static string ExtractName(string label, string slug)
    {
        var ayetIndex = label.IndexOf("Ayet:", StringComparison.OrdinalIgnoreCase);
        var name = ayetIndex >= 0 ? label[..ayetIndex] : label;
        name = Regex.Replace(name, @"^\s*\d+\s*-\s*", string.Empty).Trim();
        return string.IsNullOrWhiteSpace(name) ? slug.Replace('-', ' ') : name;
    }

    private static int ExtractTotalAyahCount(string label)
    {
        var match = Regex.Match(label, @"Ayet:\s*(\d+)", RegexOptions.IgnoreCase);
        return match.Success && int.TryParse(match.Groups[1].Value, out var count) ? count : 0;
    }

    [GeneratedRegex(@"/tefsir/sure/(\d+)-([^/?#]+)", RegexOptions.IgnoreCase)]
    private static partial Regex SureHrefRegex();
}