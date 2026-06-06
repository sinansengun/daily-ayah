using System.Globalization;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using DailyAyah.Api.Abstractions;
using DailyAyah.Api.Config;
using DailyAyah.Api.Models;
using HtmlAgilityPack;

namespace DailyAyah.Api.Scraper;

public sealed class DiyanetScraper(HttpClient httpClient) : IDiyanetScraper
{
    private static readonly Regex ReferenceRegex = new(@"\(([^()]{2,140})\)", RegexOptions.Compiled);

    public async Task<ScrapedAyah> FetchDailyAyahAsync(CancellationToken cancellationToken = default)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, AppConstants.DiyanetHomepageUrl);
        request.Headers.TryAddWithoutValidation("Accept", "text/html");
        request.Headers.TryAddWithoutValidation("User-Agent", "DiyanetDailyAyah/1.0 (+contact: admin@example.com)");

        using var response = await httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
        response.EnsureSuccessStatusCode();

        var html = await response.Content.ReadAsStringAsync(cancellationToken);
        return ParseDailyAyahFromHtml(html);
    }

    public static ScrapedAyah ParseDailyAyahFromHtml(string html)
    {
        var document = new HtmlDocument();
        document.LoadHtml(html);

        if (TryParseFromAyetBlock(document, out var blockParsed))
        {
            return blockParsed;
        }

        var heading = document.DocumentNode
            .SelectNodes("//h3")?
            .FirstOrDefault(node => NormalizeForSearch(NormalizeText(node.InnerText)).Contains("gunun ayeti", StringComparison.Ordinal));

        if (heading is null)
        {
            throw new InvalidOperationException("Gunun Ayeti heading not found in source HTML.");
        }

        if (TryParseFromHeading(heading, out var headingParsed))
        {
            return headingParsed;
        }

        throw new InvalidOperationException("Unable to parse daily ayah text/reference from source HTML.");
    }

    private static bool TryParseFromAyetBlock(HtmlDocument document, out ScrapedAyah parsed)
    {
        var ayetBox = document.DocumentNode.SelectSingleNode(
            "//div[contains(@class,'ayet-hadis-dua')]//div[contains(@class,'ahd-boxes')][.//div[contains(@class,'ayet')]]"
        );

        if (ayetBox is null)
        {
            parsed = default!;
            return false;
        }

        var verseNode = ayetBox.SelectSingleNode(".//div[contains(@class,'ayet')]//p[contains(@class,'ahd-content-text')]");
        var referenceNode = ayetBox.SelectSingleNode(".//div[contains(@class,'aht-bottom')]//p[contains(@class,'alt-sure-title')]");

        var verse = NormalizeText(verseNode?.InnerText ?? string.Empty);
        var referenceRaw = NormalizeText(referenceNode?.InnerText ?? string.Empty);
        var reference = ExtractReference(referenceRaw);

        if (string.IsNullOrWhiteSpace(reference))
        {
            reference = NormalizeText(referenceRaw.Trim('(', ')'));
        }

        if (string.IsNullOrWhiteSpace(verse) || string.IsNullOrWhiteSpace(reference))
        {
            parsed = default!;
            return false;
        }

        parsed = new ScrapedAyah(verse, reference);
        return true;
    }

    private static bool TryParseFromHeading(HtmlNode heading, out ScrapedAyah parsed)
    {
        var verse = string.Empty;
        var candidateAnchors = heading.SelectNodes("following-sibling::a")?.Take(6) ?? Enumerable.Empty<HtmlNode>();

        foreach (var anchor in candidateAnchors)
        {
            var text = NormalizeText(anchor.InnerText);
            if (string.IsNullOrWhiteSpace(text) || text == "" || text.StartsWith('(') || text.Length < 12)
            {
                continue;
            }

            verse = text;
            break;
        }

        var blockTexts = new List<string>();
        for (var node = heading.NextSibling; node is not null; node = node.NextSibling)
        {
            if (node.NodeType != HtmlNodeType.Element)
            {
                continue;
            }

            if (string.Equals(node.Name, "h3", StringComparison.OrdinalIgnoreCase))
            {
                break;
            }

            var text = NormalizeText(node.InnerText);
            if (!string.IsNullOrWhiteSpace(text) && text != "")
            {
                blockTexts.Add(text);
            }
        }

        var combined = NormalizeText(string.Join(' ', blockTexts));
        var reference = ExtractReference(combined);

        if (string.IsNullOrWhiteSpace(verse))
        {
            verse = RemoveReference(combined);
        }

        if (string.IsNullOrWhiteSpace(verse) || string.IsNullOrWhiteSpace(reference))
        {
            parsed = default!;
            return false;
        }

        parsed = new ScrapedAyah(verse, reference);
        return true;
    }

    private static string NormalizeText(string value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return string.Empty;
        }

        var decoded = WebUtility.HtmlDecode(value);
        return Regex.Replace(decoded.Replace('\u00A0', ' '), @"\s+", " ").Trim();
    }

    private static string ExtractReference(string text)
    {
        var match = ReferenceRegex.Match(text);
        return match.Success ? NormalizeText(match.Groups[1].Value) : string.Empty;
    }

    private static string RemoveReference(string text)
    {
        return NormalizeText(ReferenceRegex.Replace(text, " "));
    }

    private static string NormalizeForSearch(string value)
    {
        var normalized = value.Normalize(NormalizationForm.FormD);
        var sb = new StringBuilder(normalized.Length);

        foreach (var c in normalized)
        {
            var category = CharUnicodeInfo.GetUnicodeCategory(c);
            if (category != UnicodeCategory.NonSpacingMark)
            {
                sb.Append(char.ToLowerInvariant(c));
            }
        }

        return sb.ToString().Normalize(NormalizationForm.FormC);
    }
}
