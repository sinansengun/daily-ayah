using System.Security.Cryptography;
using System.Text;

namespace DiyanetTafsirCrawler;

public static class Hashing
{
    public static string HashText(string text)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(text));
        return Convert.ToHexString(bytes).ToLowerInvariant();
    }
}