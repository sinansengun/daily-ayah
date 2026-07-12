using System.Text.RegularExpressions;
using DiyanetTafsirCrawler.Models;
using HtmlAgilityPack;

namespace DiyanetTafsirCrawler.Scraping;

public sealed partial class TafsirSurahScraper(DiyanetPageClient pageClient)
{
    public async Task<TafsirSurah> GetSurahAsync(TafsirSurahSummary summary, bool validateAyahLinks = true, CancellationToken cancellationToken = default)
    {
        var document = await pageClient.GetDocumentAsync(summary.SourceUri, cancellationToken);
        var title = HtmlText.NullIfEmpty(document.DocumentNode.SelectSingleNode("//h1")?.InnerText) ?? summary.Name;
        var totalAyahCount = ReadInfoNumber(document, "Toplam Ayet") ?? summary.TotalAyahCount;
        var mushafOrder = ReadInfoNumber(document, "Mushaf");
        var nuzulOrder = ReadInfoNumber(document, "Nüzul");
        var ayahLinks = document.DocumentNode
            .SelectNodes("//a[contains(@href, '-ayet-tefsiri')]")
            ?.Select(ToAyahLink)
            .Where(link => link is not null)
            .Select(link => link!)
            .GroupBy(link => link.SourceUri.ToString())
            .Select(group => group.First())
            .OrderBy(link => link.StartAyahNumber)
            .ToArray()
            ?? [];

        if (ayahLinks.Length == 0)
        {
            throw new InvalidOperationException($"No ayah links found for surah {summary.SurahNumber}: {summary.SourceUri}");
        }

        var expandedAyahCount = ayahLinks.Sum(link => link.EndAyahNumber - link.StartAyahNumber + 1);
        if (validateAyahLinks && totalAyahCount > 0 && expandedAyahCount != totalAyahCount)
        {
            throw new InvalidOperationException($"Expected {totalAyahCount} ayahs for surah {summary.SurahNumber}, but Diyanet links expand to {expandedAyahCount}.");
        }

        return new TafsirSurah(
            summary.SurahNumber,
            title.Replace(" Suresi", string.Empty, StringComparison.OrdinalIgnoreCase),
            summary.Slug,
            totalAyahCount,
            mushafOrder,
            nuzulOrder,
            ReadSection(document, "Hakkında"),
            ReadSection(document, "Nüzul"),
            ReadSection(document, "Konusu"),
            ReadSection(document, "Fazileti"),
            summary.SourceUri,
            ayahLinks);
    }

    private static int? ReadInfoNumber(HtmlDocument document, string labelPrefix)
    {
        var label = document.DocumentNode
            .SelectNodes("//*[contains(concat(' ', normalize-space(@class), ' '), ' info-label ')]")
            ?.FirstOrDefault(node => HtmlText.Normalize(node.InnerText).StartsWith(labelPrefix, StringComparison.OrdinalIgnoreCase));

        var text = HtmlText.Normalize(label?.ParentNode?.InnerText);
        var match = Regex.Match(text, @"(\d+)");
        return match.Success && int.TryParse(match.Groups[1].Value, out var value) ? value : null;
    }

    private static string? ReadSection(HtmlDocument document, string title)
    {
        var heading = document.DocumentNode
            .SelectNodes("//*[contains(concat(' ', normalize-space(@class), ' '), ' section-title ')]")
            ?.FirstOrDefault(node => HtmlText.Normalize(node.InnerText).Equals(title, StringComparison.OrdinalIgnoreCase));

        if (heading is null)
        {
            return null;
        }

        var text = HtmlText.FromNode(heading.ParentNode);
        if (text.StartsWith(title, StringComparison.OrdinalIgnoreCase))
        {
            text = text[title.Length..].Trim();
        }

        return HtmlText.NullIfEmpty(text);
    }

    private TafsirAyahLink? ToAyahLink(HtmlNode link)
    {
        var href = link.GetAttributeValue("href", string.Empty);
        var match = AyahUriRegex().Match(href);
        if (!match.Success || !int.TryParse(match.Groups[1].Value, out var startAyah))
        {
            return null;
        }

        var endAyah = match.Groups[2].Success && int.TryParse(match.Groups[2].Value, out var parsedEnd)
            ? parsedEnd
            : startAyah;

        return new TafsirAyahLink(pageClient.ToAbsoluteUri(href), startAyah, endAyah);
    }

    [GeneratedRegex(@"/\d+/(\d+)(?:-(\d+))?-ayet-tefsiri", RegexOptions.IgnoreCase)]
    private static partial Regex AyahUriRegex();
}