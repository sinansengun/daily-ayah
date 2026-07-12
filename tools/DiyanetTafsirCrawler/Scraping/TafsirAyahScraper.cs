using System.Text.RegularExpressions;
using DiyanetTafsirCrawler.Models;
using HtmlAgilityPack;

namespace DiyanetTafsirCrawler.Scraping;

public sealed partial class TafsirAyahScraper(DiyanetPageClient pageClient)
{
    public async Task<IReadOnlyList<TafsirAyah>> GetAyahsAsync(TafsirSurah surah, TafsirAyahLink ayahLink, CancellationToken cancellationToken = default)
    {
        var document = await pageClient.GetDocumentAsync(ayahLink.SourceUri, cancellationToken);
        var surahName = HtmlText.Normalize(document.DocumentNode.SelectSingleNode("//h1")?.InnerText).Replace(" Suresi", string.Empty, StringComparison.OrdinalIgnoreCase);
        var arabicText = HtmlText.NullIfEmpty(HtmlText.RemoveVerseNumbers(HtmlText.FromNode(SelectByClass(document, "arabic-text"))));
        var mealText = HtmlText.RemoveVerseNumbers(HtmlText.FromNode(SelectByClass(document, "meal-text")));
        var tefsirRaw = HtmlText.FromNode(SelectByClass(document, "tefsir-text"));
        var sourceReference = ExtractSourceReference(tefsirRaw);
        var tefsirText = RemoveSourceReference(tefsirRaw);

        if (!SameSurah(surah.Name, surahName))
        {
            throw new InvalidOperationException($"Expected surah {surah.Name}, but page returned {surahName}: {ayahLink.SourceUri}");
        }

        var pageRange = ReadAyahRange(document, ayahLink.SourceUri);
        if (pageRange.Start != ayahLink.StartAyahNumber || pageRange.End != ayahLink.EndAyahNumber)
        {
            throw new InvalidOperationException($"Expected ayah range {ayahLink.StartAyahNumber}-{ayahLink.EndAyahNumber}, but page returned {pageRange.Start}-{pageRange.End}: {ayahLink.SourceUri}");
        }

        if (string.IsNullOrWhiteSpace(mealText))
        {
            throw new InvalidOperationException($"Meal text not found: {ayahLink.SourceUri}");
        }

        if (string.IsNullOrWhiteSpace(tefsirText))
        {
            throw new InvalidOperationException($"Tafsir text not found: {ayahLink.SourceUri}");
        }

        return ayahLink.AyahNumbers
            .Select(ayahNumber => new TafsirAyah(
                surah.SurahNumber,
                string.IsNullOrWhiteSpace(surahName) ? surah.Name : surahName,
                ayahNumber,
                ayahLink.StartAyahNumber,
                ayahLink.EndAyahNumber,
                arabicText,
                mealText,
                tefsirText,
                sourceReference,
                ayahLink.SourceUri))
            .ToArray();
    }

    private static HtmlNode? SelectByClass(HtmlDocument document, string className)
    {
        return document.DocumentNode.SelectSingleNode($"//*[contains(concat(' ', normalize-space(@class), ' '), ' {className} ')]");
    }

    private static (int Start, int End) ReadAyahRange(HtmlDocument document, Uri ayahUri)
    {
        var currentVerse = HtmlText.Normalize(document.DocumentNode.SelectSingleNode("//*[contains(concat(' ', normalize-space(@class), ' '), ' current-verse ')]")?.InnerText);
        var match = AyahRangeRegex().Match(currentVerse);
        if (match.Success && int.TryParse(match.Groups[1].Value, out var startFromText))
        {
            var endFromText = match.Groups[2].Success && int.TryParse(match.Groups[2].Value, out var parsedEnd)
                ? parsedEnd
                : startFromText;
            return (startFromText, endFromText);
        }

        match = AyahUriRegex().Match(ayahUri.AbsolutePath);
        if (match.Success && int.TryParse(match.Groups[1].Value, out var startFromUri))
        {
            var endFromUri = match.Groups[2].Success && int.TryParse(match.Groups[2].Value, out var parsedEnd)
                ? parsedEnd
                : startFromUri;
            return (startFromUri, endFromUri);
        }

        throw new InvalidOperationException($"Ayah number not found: {ayahUri}");
    }

    private static string? ExtractSourceReference(string tefsirText)
    {
        var match = SourceRegex().Match(tefsirText);
        return match.Success ? HtmlText.NullIfEmpty(match.Groups[1].Value) : null;
    }

    private static string RemoveSourceReference(string tefsirText)
    {
        return HtmlText.Normalize(SourceRegex().Replace(tefsirText, string.Empty));
    }

    private static bool SameSurah(string expected, string actual)
    {
        return SlugifyName(expected) == SlugifyName(actual);
    }

    private static string SlugifyName(string value)
    {
        return string.Concat(HtmlText.Normalize(value).ToLowerInvariant().Where(char.IsLetterOrDigit));
    }

    [GeneratedRegex(@"/(\d+)(?:-(\d+))?-ayet-tefsiri", RegexOptions.IgnoreCase)]
    private static partial Regex AyahUriRegex();

    [GeneratedRegex(@"(\d+)(?:\s*-\s*(\d+))?", RegexOptions.IgnoreCase)]
    private static partial Regex AyahRangeRegex();

    [GeneratedRegex(@"\s*Kaynak:\s*(.+?)\s*$", RegexOptions.IgnoreCase | RegexOptions.Singleline)]
    private static partial Regex SourceRegex();
}