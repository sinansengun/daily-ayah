namespace DiyanetTafsirCrawler;

public sealed class CrawlResult
{
    public int SurahsSeen { get; set; }
    public int SurahsWritten { get; set; }
    public int AyahsSeen { get; set; }
    public int AyahsWritten { get; set; }
    public int AyahsSkipped { get; set; }
    public int FailedSurahs { get; set; }
    public int FailedAyahs { get; set; }
    public List<string> Errors { get; } = [];

    public string ToSummary()
    {
        var lines = new List<string>
        {
            "Tafsir crawl completed.",
            $"Surahs seen: {SurahsSeen}",
            $"Surahs written: {SurahsWritten}",
            $"Ayahs seen: {AyahsSeen}",
            $"Ayahs written: {AyahsWritten}",
            $"Ayahs skipped: {AyahsSkipped}",
            $"Failed surahs: {FailedSurahs}",
            $"Failed ayahs: {FailedAyahs}"
        };

        if (Errors.Count > 0)
        {
            lines.Add("Errors:");
            lines.AddRange(Errors.Take(25).Select(error => $"- {error}"));
            if (Errors.Count > 25)
            {
                lines.Add($"- ... {Errors.Count - 25} more");
            }
        }

        return string.Join(Environment.NewLine, lines);
    }
}