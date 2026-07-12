using DiyanetTafsirCrawler.Models;

namespace DiyanetTafsirCrawler.Scraping;

public static class SurahSeeds
{
    public static IReadOnlyList<TafsirSurahSeed> All { get; } = [
        new(1, "Fâtiha", "fatiha", 7), new(2, "Bakara", "bakara", 286), new(3, "Âl-i İmrân", "al-i-imran", 200),
        new(4, "Nisâ", "nisa", 176), new(5, "Mâide", "maide", 120), new(6, "En'âm", "enam", 165),
        new(7, "A'râf", "araf", 206), new(8, "Enfâl", "enfal", 75), new(9, "Tevbe", "tevbe", 129),
        new(10, "Yûnus", "yunus", 109), new(11, "Hûd", "hud", 123), new(12, "Yûsuf", "yusuf", 111),
        new(13, "Ra'd", "rad", 43), new(14, "İbrâhîm", "ibrahim", 52), new(15, "Hicr", "hicr", 99),
        new(16, "Nahl", "nahl", 128), new(17, "İsrâ", "isra", 111), new(18, "Kehf", "kehf", 110),
        new(19, "Meryem", "meryem", 98), new(20, "Tâhâ", "taha", 135), new(21, "Enbiyâ", "enbiya", 112),
        new(22, "Hac", "hac", 78), new(23, "Mü'minûn", "muminun", 118), new(24, "Nûr", "nur", 64),
        new(25, "Furkân", "furkan", 77), new(26, "Şuarâ", "suara", 227), new(27, "Neml", "neml", 93),
        new(28, "Kasas", "kasas", 88), new(29, "Ankebût", "ankebut", 69), new(30, "Rûm", "rum", 60),
        new(31, "Lokmân", "lokman", 34), new(32, "Secde", "secde", 30), new(33, "Ahzâb", "ahzab", 73),
        new(34, "Sebe'", "sebe", 54), new(35, "Fâtır", "fatir", 45), new(36, "Yâsîn", "yasin", 83),
        new(37, "Sâffât", "saffat", 182), new(38, "Sâd", "sad", 88), new(39, "Zümer", "zumer", 75),
        new(40, "Mü'min", "mumin", 85), new(41, "Fussilet", "fussilet", 54), new(42, "Şûrâ", "sura", 53),
        new(43, "Zuhruf", "zuhruf", 89), new(44, "Duhân", "duhan", 59), new(45, "Câsiye", "casiye", 37),
        new(46, "Ahkâf", "ahkaf", 35), new(47, "Muhammed", "muhammed", 38), new(48, "Fetih", "fetih", 29),
        new(49, "Hucurât", "hucurat", 18), new(50, "Kâf", "kaf", 45), new(51, "Zâriyât", "zariyat", 60),
        new(52, "Tûr", "tur", 49), new(53, "Necm", "necm", 62), new(54, "Kamer", "kamer", 55),
        new(55, "Rahmân", "rahman", 78), new(56, "Vâkıa", "vakia", 96), new(57, "Hadîd", "hadid", 29),
        new(58, "Mücâdele", "mucadele", 22), new(59, "Haşr", "hasr", 24), new(60, "Mümtehine", "mumtehine", 13),
        new(61, "Saff", "saff", 14), new(62, "Cum'a", "cuma", 11), new(63, "Münâfikûn", "munafikun", 11),
        new(64, "Teğâbün", "tegabun", 18), new(65, "Talâk", "talak", 12), new(66, "Tahrîm", "tahrim", 12),
        new(67, "Mülk", "mulk", 30), new(68, "Kalem", "kalem", 52), new(69, "Hâkka", "hakka", 52),
        new(70, "Meâric", "mearic", 44), new(71, "Nûh", "nuh", 28), new(72, "Cin", "cin", 28),
        new(73, "Müzzemmil", "muzzemmil", 20), new(74, "Müddessir", "muddessir", 56), new(75, "Kıyâmet", "kiyame", 40),
        new(76, "İnsân", "insan", 31), new(77, "Mürselât", "murselat", 50), new(78, "Nebe'", "nebe", 40),
        new(79, "Nâziât", "naziat", 46), new(80, "Abese", "abese", 42), new(81, "Tekvîr", "tekvir", 29),
        new(82, "İnfitâr", "infitar", 19), new(83, "Mutaffifîn", "mutaffifin", 36), new(84, "İnşikâk", "insikak", 25),
        new(85, "Bürûc", "buruc", 22), new(86, "Târık", "tarik", 17), new(87, "A'lâ", "ala", 19),
        new(88, "Gâşiye", "gasiye", 26), new(89, "Fecr", "fecr", 30), new(90, "Beled", "beled", 20),
        new(91, "Şems", "sems", 15), new(92, "Leyl", "leyl", 21), new(93, "Duhâ", "duha", 11),
        new(94, "İnşirâh", "insirah", 8), new(95, "Tîn", "tin", 8), new(96, "Alak", "alak", 19),
        new(97, "Kadir", "kadir", 5), new(98, "Beyyine", "beyyine", 8), new(99, "Zilzâl", "zilzal", 8),
        new(100, "Âdiyât", "adiyat", 11), new(101, "Kâria", "karia", 11), new(102, "Tekâsür", "tekasur", 8),
        new(103, "Asr", "asr", 3), new(104, "Hümeze", "humeze", 9), new(105, "Fîl", "fil", 5),
        new(106, "Kureyş", "kureys", 4), new(107, "Mâûn", "maun", 7), new(108, "Kevser", "kevser", 3),
        new(109, "Kâfirûn", "kafirun", 6), new(110, "Nasr", "nasr", 3), new(111, "Tebbet", "tebbet", 5),
        new(112, "İhlâs", "ihlas", 4), new(113, "Felak", "felak", 5), new(114, "Nâs", "nas", 6)
    ];
}