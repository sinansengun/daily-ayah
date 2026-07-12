using System.Globalization;
using System.Text;
using System.Text.RegularExpressions;

namespace DailyAyah.Api.Models;

public static partial class AyahReferenceParser
{
    private static readonly IReadOnlyDictionary<string, int> SurahNumbers = new Dictionary<string, int>
    {
        ["fatiha"] = 1, ["bakara"] = 2, ["aliimran"] = 3, ["alimran"] = 3, ["nisa"] = 4,
        ["maide"] = 5, ["enam"] = 6, ["araf"] = 7, ["enfal"] = 8, ["tevbe"] = 9,
        ["yunus"] = 10, ["hud"] = 11, ["yusuf"] = 12, ["rad"] = 13, ["ibrahim"] = 14,
        ["hicr"] = 15, ["nahl"] = 16, ["isra"] = 17, ["kehf"] = 18, ["meryem"] = 19,
        ["taha"] = 20, ["enbiya"] = 21, ["hac"] = 22, ["muminun"] = 23, ["nur"] = 24,
        ["furkan"] = 25, ["suara"] = 26, ["neml"] = 27, ["kasas"] = 28, ["ankebut"] = 29,
        ["rum"] = 30, ["lokman"] = 31, ["secde"] = 32, ["ahzab"] = 33, ["sebe"] = 34,
        ["fatir"] = 35, ["yasin"] = 36, ["saffat"] = 37, ["sad"] = 38, ["zumer"] = 39,
        ["mumin"] = 40, ["fussilet"] = 41, ["sura"] = 42, ["zuhruf"] = 43, ["duhan"] = 44,
        ["casiye"] = 45, ["ahkaf"] = 46, ["muhammed"] = 47, ["fetih"] = 48, ["hucurat"] = 49,
        ["kaf"] = 50, ["zariyat"] = 51, ["tur"] = 52, ["necm"] = 53, ["kamer"] = 54,
        ["rahman"] = 55, ["vakia"] = 56, ["hadid"] = 57, ["mucadele"] = 58, ["hasr"] = 59,
        ["mumtehine"] = 60, ["saff"] = 61, ["cuma"] = 62, ["munafikun"] = 63, ["tegabun"] = 64,
        ["talak"] = 65, ["tahrim"] = 66, ["mulk"] = 67, ["kalem"] = 68, ["hakka"] = 69,
        ["mearic"] = 70, ["nuh"] = 71, ["cin"] = 72, ["muzzemmil"] = 73, ["muddessir"] = 74,
        ["kiyame"] = 75, ["insan"] = 76, ["murselat"] = 77, ["nebe"] = 78, ["naziat"] = 79,
        ["abese"] = 80, ["tekvir"] = 81, ["infitar"] = 82, ["mutaffifin"] = 83, ["insikak"] = 84,
        ["buruc"] = 85, ["tarik"] = 86, ["ala"] = 87, ["gasiye"] = 88, ["fecr"] = 89,
        ["beled"] = 90, ["sems"] = 91, ["leyl"] = 92, ["duha"] = 93, ["insirah"] = 94,
        ["tin"] = 95, ["alak"] = 96, ["kadir"] = 97, ["beyyine"] = 98, ["zilzal"] = 99,
        ["adiyat"] = 100, ["karia"] = 101, ["tekasur"] = 102, ["asr"] = 103, ["humeze"] = 104,
        ["fil"] = 105, ["kureys"] = 106, ["maun"] = 107, ["kevser"] = 108, ["kafirun"] = 109,
        ["nasr"] = 110, ["tebbet"] = 111, ["ihlas"] = 112, ["felak"] = 113, ["nas"] = 114
    };

    public static (int? SurahNumber, int? AyahNumber) Parse(string reference)
    {
        if (string.IsNullOrWhiteSpace(reference))
        {
            return (null, null);
        }

        var ayahMatch = AyahNumberRegex().Match(reference);
        var ayahNumber = ayahMatch.Success && int.TryParse(ayahMatch.Groups[1].Value, out var parsedAyah)
            ? parsedAyah
            : (int?)null;

        var normalized = Normalize(reference);
        var surahNumber = SurahNumbers
            .OrderByDescending(item => item.Key.Length)
            .FirstOrDefault(item => normalized.Contains(item.Key, StringComparison.Ordinal)).Value;

        return (surahNumber == 0 ? null : surahNumber, ayahNumber);
    }

    private static string Normalize(string value)
    {
        var normalized = value.Normalize(NormalizationForm.FormD);
        var builder = new StringBuilder(normalized.Length);

        foreach (var character in normalized)
        {
            var category = CharUnicodeInfo.GetUnicodeCategory(character);
            if (category != UnicodeCategory.NonSpacingMark && char.IsLetterOrDigit(character))
            {
                builder.Append(char.ToLowerInvariant(character));
            }
        }

        return builder.ToString().Normalize(NormalizationForm.FormC);
    }

    [GeneratedRegex(@"/(\d+)")]
    private static partial Regex AyahNumberRegex();
}