# iOS Widget Hazirlik Notlari

Bu belge, 20 Eylul 2026 tarihinde Android widget'larinda tamamlanan davranislari ve bunlarin iOS WidgetKit tarafina aktarim planini kaydeder.

## Hedef Widget Katalogu

Android tarafinda bes bagimsiz widget secenegi bulunuyor:

1. **Ayet + Vakit**
2. **Zikirmatik**
3. **Gunun Ayeti**
4. **Namaz Vakti**
5. **Ayet + Vakit + Zikir**

Mevcut iOS projesinde yalnizca Gunun Ayeti ve Zikirmatik widget'lari var. Namaz Vakti, Ayet + Vakit ve uc bolumlu Ayet + Vakit + Zikir widget'lari yeni `Widget` tanimlari olarak eklenmeli.

> WidgetKit, Android launcher gibi `5x1` ve `5x2` hucre olculeri sunmaz. En yakin karsiliklar `systemMedium` ve `systemLarge` aileleridir. Tasarimlar birebir hucre olcusu yerine bu ailelere uyarlanmalidir.

## Tamamlanan Android Davranislari

### Boyuta gore tipografi

- Tek satirli/kucuk widget varyantlarinda okunabilirlik icin fontlar buyutuldu.
- Iki satirli/buyuk varyantlarda daha buyuk font seti kullaniliyor.
- Uclu widget'ta buyuk gorunumde ayet, namaz sayaci ve zikir sayaci birlikte buyuyor.
- `5x1` uclu widget'ta namaz bolumu daraltildi, acilan yatay alan ayet bolumune aktarildi.

Android uclu widget bolum agirliklari:

- Ayet: `2.0`
- Namaz: `0.9`
- Zikir: `0.8`

WidgetKit karsiligi icin `GeometryReader` veya sabit oranli `HStack` kullanilabilir. Onerilen oran yaklasik olarak `%54 / %24 / %22`.

### Ayet tasma kontrolu

Ayet metni sinirsiz birakildiginda kucuk widget'ta referans ve komsu bolumlerin ustune tasiyordu. Uygulanan satir butceleri:

| Widget | Kucuk/tek satir | Buyuk/iki satir |
| --- | ---: | ---: |
| Ayet + Vakit | 4 | 9 |
| Gunun Ayeti | 3 | 9 |
| Ayet + Vakit + Zikir | 4 | 9 |

Metin sigmazsa son satirda ellipsis gosteriliyor. iOS tarafinda `.lineLimit(...)`, `.truncationMode(.tail)` ve gerekirse `.minimumScaleFactor(...)` birlikte kullanilmali. Referans her zaman tek satir kalmali.

### Arka plan opakligi

Bes kademe kullaniliyor:

- `%0`
- `%25`
- `%50`
- `%75`
- `%100`

Her widget kendi secimini ayri sakliyor. iOS'ta bu ayar `AppIntentConfiguration` ile widget bazinda sunulabilir. Intent parametresi bir enum olmali; serbest sayisal deger yerine ayni bes kademe korunmali.

### Otomatik kontrast

Arka plan opakligina gore metin rengi otomatik seciliyor:

- Opaklik `%0` veya `%25`: beyaz metin
- Opaklik `%50`, `%75` veya `%100`: koyu metin

Koyu metin icin Android'de yaklasik `#283038`, ikincil metin icin `#56616B` kullaniliyor. iOS'ta ayni davranis `Color.white`, `Color(red: 40/255, green: 48/255, blue: 56/255)` ve ikincil renk ile kurulabilir.

### Metin golgesi

- Widget bazinda acilip kapatilabilir.
- Varsayilan deger **kapali**.
- Acikken siyah, dusuk yaricapli ve hafif asagi kaydirilmis golge kullaniliyor.
- Android'de dinamik `RemoteViews.setTextAppearance` Samsung Launcher tarafindan desteklenmedigi icin XML tabanli iki gorunum kullanildi.

WidgetKit'te bu kisit yoktur. SwiftUI tarafinda kosullu modifier uygulanabilir:

```swift
.shadow(
    color: configuration.textShadowEnabled ? .black.opacity(0.8) : .clear,
    radius: configuration.textShadowEnabled ? 2 : 0,
    y: configuration.textShadowEnabled ? 1 : 0
)
```

### Zikir progress bar kontrasti

Progress rengi arka planla ayni otomatik kontrast kuralini izliyor:

- Dusuk opaklikta dolu kisim beyaz.
- Yuksek opaklikta dolu kisim koyu.
- Bos kanal ayni rengin dusuk alfa degeriyle gosteriliyor.

Bu davranis hem bagimsiz Zikirmatik hem de uclu widget icin gecerlidir. iOS'ta `ProgressView.tint(...)` veya ozel `Capsule` tabanli progress gorunumu kullanilabilir. Kanal rengi icin ana rengin yaklasik `%27` opakligi uygundur.

### Namaz sayaci

- Sonraki namazin adi gosteriliyor.
- Kalan zaman `H:MM dk` formatinda.
- Sayac sifira geldiginde negatif deger gostermek yerine sonraki namaza geciyor.
- Konum, sayacin altinda kucuk ve normal agirlikta gosteriliyor.
- Ag olmadiginda son basarili namaz verisi kullaniliyor.

WidgetKit zaman cizelgesi Android'deki kesin dakika alarmi gibi garanti vermez. Onerilen iOS yaklasimi:

1. Namaz vakitlerini App Group icinde cache'le.
2. Timeline'a sonraki namaza kadar dakikalik entry'ler ekle.
3. Namaz sinirinda sonraki vakti gosteren ayri entry olustur.
4. Timeline sonuna `.after(...)` yenileme politikasi ekle.
5. Uygulama acildiginda veri yenileyip `WidgetCenter.shared.reloadTimelines(...)` cagir.

Dakikalik timeline entry sayisi ve sistem yenileme butcesi test edilmelidir. WidgetKit'in tam dakikada yenileme garantisi olmadigi UI metninde hesaba katilmalidir.

## iOS Veri Modeli Ihtiyaclari

Mevcut App Group store ayet ve zikir verisini paylasiyor. Namaz widget'lari icin asagidaki cache modeli eklenmeli:

```swift
struct CachedPrayerTimes: Codable {
    let city: String
    let timeZone: String
    let date: String
    let imsak: String
    let ogle: String
    let ikindi: String
    let aksam: String
    let yatsi: String
    let fetchedAt: Date
}
```

Sonraki namaz hesaplamasi uygulama ve widget tarafinda ayni ortak dosyada tutulmali. Gece Yatsi sonrasi ertesi gunun Imsak vaktine gecis ozellikle test edilmeli.

Widget gorunum ayarlari icin onerilen intent alanlari:

```swift
@Parameter(title: "Arka plan")
var backgroundOpacity: WidgetOpacity

@Parameter(title: "Metin golgesi", default: false)
var textShadowEnabled: Bool
```

## Onerilen iOS Dosya Yapisi

- `ios/Widget/Models/PrayerWidgetEntry.swift`
- `ios/Widget/Models/WidgetAppearance.swift`
- `ios/Widget/Intents/WidgetAppearanceIntent.swift`
- `ios/Widget/PrayerTimelineProvider.swift`
- `ios/Widget/CombinedTimelineProvider.swift`
- `ios/Widget/PrayerWidgetView.swift`
- `ios/Widget/AyahPrayerWidgetView.swift`
- `ios/Widget/AllInOneWidgetView.swift`
- `ios/App/Storage/SharedPrayerTimesStore.swift`
- `ios/App/Services/PrayerTimesClient.swift`

Ortak renk, opacity, golge ve tipografi hesaplari tek bir `WidgetAppearance` yardimcisinda tutulmali. Ayet, namaz ve zikir widget'lari ayni kontrast kararini kullanmali.

## Uygulama Sirasi

1. Namaz API modeli ve App Group cache katmanini ekle.
2. Sonraki namaz hesaplamasini ve gece gecisini test et.
3. Bes kademeli opacity ile varsayilan kapali golgeyi `AppIntent` olarak tanimla.
4. Bagimsiz Namaz Vakti widget'ini ekle.
5. Ayet + Vakit `systemMedium` widget'ini ekle.
6. Ayet + Vakit + Zikir icin `systemMedium` ve `systemLarge` gorunumleri ekle.
7. Mevcut Ayet ve Zikirmatik widget'larini ortak gorunum ayarlarina gecir.
8. Ayet satir butcelerini widget ailesine gore uygula.
9. Progress bar kontrastini opacity ayarina bagla.
10. Uygulama veri degisikliklerinde ilgili timeline'lari yenile.

## Kabul Kontrolleri

- Her widget digerlerinden bagimsiz eklenebilmeli ve kaldirilabilmeli.
- Opacity ayari widget bazinda saklanmali.
- Golge varsayilan olarak kapali olmali.
- Dusuk opacity'de beyaz, yuksek opacity'de koyu metin kullanilmali.
- Zikir progress bar her iki kontrast modunda gorunur olmali.
- Ayet referansi veya komsu bolumlerin ustune tasmamali.
- Buyuk ailede ayet icin daha fazla satir ve daha buyuk font kullanilmali.
- Namaz sayaci negatif olmamali; vakit dolunca sonraki namaza gecmeli.
- Konum sayacin altinda gorunmeli.
- Ag kesildiginde son basarili ayet ve namaz verileri kullanilmali.
- Gece yarisi ve Yatsi sonrasi Imsak gecisi dogru calismali.
- WidgetKit preview'lari `systemSmall`, `systemMedium` ve `systemLarge` icin kontrol edilmeli.

## Android Uygulamasindan Cikarilan Notlar

- Widget host'lar dinamik UI API'lerinin tamamini desteklemeyebilir. Android'de `setTextAppearance` Samsung Launcher'da `RemoteViews.ActionException` uretti.
- Widget yapilandirmasi tamamlanmadan once gecerli ilk gorunum yayinlanmali; aksi halde launcher bos gorunumu "Widget eklenemedi" olarak yorumlayabilir.
- Boyut kararinda yalnizca bildirilen minimum yukseklige guvenmemek gerekir. Samsung tarafinda gercek options degerleri `97dp` ve `215dp` olarak geldi.
- Metin satir sayisini tamamen sinirsiz yapmak kucuk widget'ta tasma yaratti. Boyuta bagli satir butcesi daha guvenli.
- Dakikalik namaz yenilemesinde her dakika ag istegi yapilmamali; cache uzerinden hesaplama yapilmali.
