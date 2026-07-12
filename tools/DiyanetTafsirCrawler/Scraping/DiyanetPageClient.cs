using HtmlAgilityPack;

namespace DiyanetTafsirCrawler.Scraping;

public sealed class DiyanetPageClient(HttpClient httpClient, TimeSpan delay, int maxRetries)
{
    private int _requestCount;

    public async Task<HtmlDocument> GetDocumentAsync(Uri uri, CancellationToken cancellationToken = default)
    {
        Exception? lastException = null;

        for (var attempt = 1; attempt <= maxRetries; attempt++)
        {
            if (_requestCount > 0 && delay > TimeSpan.Zero)
            {
                await Task.Delay(delay, cancellationToken);
            }

            _requestCount++;

            try
            {
                using var response = await httpClient.GetAsync(uri, cancellationToken);
                response.EnsureSuccessStatusCode();
                var html = await response.Content.ReadAsStringAsync(cancellationToken);
                var document = new HtmlDocument();
                document.LoadHtml(html);
                return document;
            }
            catch (Exception ex) when (attempt < maxRetries)
            {
                lastException = ex;
                await Task.Delay(TimeSpan.FromMilliseconds(500 * attempt), cancellationToken);
            }
        }

        throw lastException ?? new InvalidOperationException($"Could not fetch {uri}.");
    }

    public Uri ToAbsoluteUri(string href)
    {
        return new Uri(httpClient.BaseAddress!, href);
    }
}