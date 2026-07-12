using Microsoft.EntityFrameworkCore;

namespace DailyAyah.Api.Data;

public sealed class DailyAyahDbContext(DbContextOptions<DailyAyahDbContext> options) : DbContext(options)
{
    public DbSet<TafsirAyahEntity> TafsirAyahs => Set<TafsirAyahEntity>();
    public DbSet<TafsirSurahEntity> TafsirSurahs => Set<TafsirSurahEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<TafsirAyahEntity>(entity =>
        {
            entity.ToTable("tafsir_ayahs");
            entity.HasKey(ayah => new { ayah.SurahNumber, ayah.AyahNumber });
            entity.Property(ayah => ayah.SurahNumber).HasColumnName("surah_number");
            entity.Property(ayah => ayah.AyahNumber).HasColumnName("ayah_number");
            entity.Property(ayah => ayah.AyahRangeStart).HasColumnName("ayah_range_start");
            entity.Property(ayah => ayah.AyahRangeEnd).HasColumnName("ayah_range_end");
            entity.Property(ayah => ayah.SurahName).HasColumnName("surah_name");
            entity.Property(ayah => ayah.ArabicText).HasColumnName("arabic_text");
            entity.Property(ayah => ayah.MealText).HasColumnName("meal_text");
            entity.Property(ayah => ayah.TafsirText).HasColumnName("tafsir_text");
            entity.Property(ayah => ayah.SourceReference).HasColumnName("source_reference");
            entity.Property(ayah => ayah.SourceUrl).HasColumnName("source_url");
            entity.Property(ayah => ayah.ContentHash).HasColumnName("content_hash");
            entity.Property(ayah => ayah.CreatedAtUtc).HasColumnName("created_at_utc");
            entity.Property(ayah => ayah.UpdatedAtUtc).HasColumnName("updated_at_utc");
        });

        modelBuilder.Entity<TafsirSurahEntity>(entity =>
        {
            entity.ToTable("tafsir_surahs");
            entity.HasKey(surah => surah.SurahNumber);
            entity.Property(surah => surah.SurahNumber).HasColumnName("surah_number");
            entity.Property(surah => surah.Name).HasColumnName("name");
            entity.Property(surah => surah.Slug).HasColumnName("slug");
            entity.Property(surah => surah.TotalAyahCount).HasColumnName("total_ayah_count");
            entity.Property(surah => surah.MushafOrder).HasColumnName("mushaf_order");
            entity.Property(surah => surah.NuzulOrder).HasColumnName("nuzul_order");
            entity.Property(surah => surah.AboutText).HasColumnName("about_text");
            entity.Property(surah => surah.NuzulText).HasColumnName("nuzul_text");
            entity.Property(surah => surah.SubjectText).HasColumnName("subject_text");
            entity.Property(surah => surah.VirtueText).HasColumnName("virtue_text");
            entity.Property(surah => surah.SourceUrl).HasColumnName("source_url");
            entity.Property(surah => surah.ContentHash).HasColumnName("content_hash");
            entity.Property(surah => surah.CreatedAtUtc).HasColumnName("created_at_utc");
            entity.Property(surah => surah.UpdatedAtUtc).HasColumnName("updated_at_utc");
        });
    }
}

public sealed class TafsirAyahEntity
{
    public int SurahNumber { get; set; }
    public int AyahNumber { get; set; }
    public int AyahRangeStart { get; set; }
    public int AyahRangeEnd { get; set; }
    public string SurahName { get; set; } = string.Empty;
    public string? ArabicText { get; set; }
    public string MealText { get; set; } = string.Empty;
    public string TafsirText { get; set; } = string.Empty;
    public string? SourceReference { get; set; }
    public string SourceUrl { get; set; } = string.Empty;
    public string ContentHash { get; set; } = string.Empty;
    public DateTimeOffset CreatedAtUtc { get; set; }
    public DateTimeOffset UpdatedAtUtc { get; set; }
}

public sealed class TafsirSurahEntity
{
    public int SurahNumber { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Slug { get; set; } = string.Empty;
    public int TotalAyahCount { get; set; }
    public int? MushafOrder { get; set; }
    public int? NuzulOrder { get; set; }
    public string? AboutText { get; set; }
    public string? NuzulText { get; set; }
    public string? SubjectText { get; set; }
    public string? VirtueText { get; set; }
    public string SourceUrl { get; set; } = string.Empty;
    public string ContentHash { get; set; } = string.Empty;
    public DateTimeOffset CreatedAtUtc { get; set; }
    public DateTimeOffset UpdatedAtUtc { get; set; }
}
