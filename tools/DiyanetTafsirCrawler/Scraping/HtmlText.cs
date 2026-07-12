using System.Text.RegularExpressions;
using HtmlAgilityPack;

namespace DiyanetTafsirCrawler.Scraping;

public static partial class HtmlText
{
    public static string Normalize(string? text)
    {
        if (string.IsNullOrWhiteSpace(text))
        {
            return string.Empty;
        }

        var decoded = HtmlEntity.DeEntitize(text).Replace('\u00a0', ' ');
        return WhitespaceRegex().Replace(decoded, " ").Trim();
    }

    public static string? NullIfEmpty(string? text)
    {
        var normalized = Normalize(text);
        return string.IsNullOrWhiteSpace(normalized) ? null : normalized;
    }

    public static string FromNode(HtmlNode? node)
    {
        if (node is null)
        {
            return string.Empty;
        }

        var paragraphs = node.SelectNodes(".//p")
            ?.Select(paragraph => Normalize(paragraph.InnerText))
            .Where(paragraph => !string.IsNullOrWhiteSpace(paragraph))
            .ToArray();

        return paragraphs is { Length: > 0 }
            ? string.Join(Environment.NewLine + Environment.NewLine, paragraphs)
            : Normalize(node.InnerText);
    }

    public static string RemoveVerseNumbers(string text)
    {
        return Normalize(VerseNumberRegex().Replace(text, " "));
    }

    [GeneratedRegex(@"\s+")]
    private static partial Regex WhitespaceRegex();

    [GeneratedRegex(@"[﴿﴾]\s*[\d\u0660-\u0669]+\s*[﴿﴾]|\A\s*[﴿﴾(]?\s*[\d\u0660-\u0669]+\s*[﴿﴾)]?\s*|[﴿﴾۝]")]
    private static partial Regex VerseNumberRegex();
}