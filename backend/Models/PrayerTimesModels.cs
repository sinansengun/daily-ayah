namespace DailyAyah.Api.Models;

public sealed record PrayerTimesResponse(
    string City,
    string Country,
    string Date,
    string TimeZone,
    string Imsak,
    string Gunes,
    string Ogle,
    string Ikindi,
    string Aksam,
    string Yatsi,
    string Source,
    string FetchedAt
);