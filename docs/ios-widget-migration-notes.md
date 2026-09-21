# iOS Widget Hazirlik Notlari

Bu belge, 20-21 Eylul 2026 tarihlerinde Android uygulamasi ve widget'larinda tamamlanan davranislari ve bunlarin iOS/WidgetKit tarafina aktarim planini kaydeder.

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

- Sayac sirasi `Gunes -> Ogle -> Ikindi -> Aksam -> Yatsi -> ertesi gun Gunes`.
- Imsak API verisinde ve vakit listesindeki yerini korur ancak widget geri sayim hedefi degildir.
- Yatsi gectikten sonra sayac ertesi gunun Gunes vaktine doner.
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
    let gunes: String
    let ogle: String
    let ikindi: String
    let aksam: String
    let yatsi: String
    let fetchedAt: Date
}
```

Sonraki namaz hesaplamasi uygulama ve widget tarafinda ayni ortak dosyada tutulmali. Widget sayacinda Imsak atlanmali; gece Yatsi sonrasi ertesi gunun Gunes vaktine gecis ozellikle test edilmeli.

## 21 Eylul Android Uygulama Tasarimi

Android'in uc ana sayfasi ve Tefsir/Sure detay yuzeyi bastan tasarlandi. iOS uygulamasi yenilenirken ayni bilgi hiyerarsisi korunmali; platformun kendi navigation, sheet ve accessibility davranislari tercih edilmeli.

### Gorsel sistem

- Yalnizca acik tema uygulandi.
- Tuval: kirik beyaz `#F5F6F2`.
- Ana yuzey: `#FCFCF8`.
- Ana metin: koyu komur `#202926`.
- Ikincil metin: `#59635F`.
- Ana vurgu: zumrut `#146B52`.
- Acik zumrut: `#D8E9E0`.
- Ikincil vurgu: altin `#9A6C1F`.
- Acik altin: `#F3E8CE`.
- Baslik ve ayet metinlerinde `Newsreader`, arayuz ve sayilarda `Manrope` kullanildi.
- Kartlar yalnizca gercek odak yuzeylerinde kullaniliyor; bolumler kart icinde kart yapmiyor.
- Kose yaricaplari kontrollu: `6`, `8` ve en fazla `12` dp.

iOS karsiligi icin fontlar projeye eklenebilir veya yakin sistem fontlari secilebilir. Dynamic Type test edilmeli; metin boyutu ekran genisligine baglanmamali.

### Uygulama kabugu

- Alt navigasyon: `Gunun Ayeti`, `Zikir`, `Vakitler`.
- Android deep link `dailyayah://zikirmatik` mevcut davranisini koruyor.
- Tefsir/Sure detayi acikken alt navigasyon gizleniyor ve detay tam ekrani kullaniyor.
- Ana sekmelerdeki kaydirma konumu detaydan donuste korunuyor.

iOS'ta `TabView` ana kabuk olabilir. Detaylar tab icinde katman olarak cizilmemeli; `NavigationStack.navigationDestination` ile acilmali.

### Gunun Ayeti sayfasi

- Ayet ana editoryal okuma yuzeyi olarak gosteriliyor.
- Referans, ayet metni ve kaynak belirgin bir tipografik siraya sahip.
- Tefsir, sure bilgisi, kopyala ve paylas islemleri menu altinda.
- Son 15 gun arasinda ileri/geri gecis ve bugune donus korunuyor.
- Hadis ve dua, ic ice kart yerine tam genislik okuma bolumleri.
- Loading ve hata durumlari erisilebilir live-region mantigiyla sunuluyor.

### Namaz Vakitleri sayfasi

- Sehir secimi ve yenileme korunuyor.
- Alti vakit tam genislik satir tablosunda: Imsak, Gunes, Ogle, Ikindi, Aksam, Yatsi.
- Uygulama sayfasinda siradaki vakit vurgulaniyor; bu liste widget sayaci kuralindan bagimsiz olarak Imsak'i gostermeye devam ediyor.
- Saat kolonu sabit ve taranabilir bir hiyerarsi kullaniyor.

### Tefsir ve Sure detayi

- Detay tam ekran okuma yuzeyi ve belirgin geri cubugu kullaniyor.
- Tefsirde Arapca metin, meal ve tefsir ayri bolumler.
- Sure bilgisinde ayet sayisi, Mushaf sirasi ve Nuzul sirasi toplu metadata yuzeyinde.
- Yapay alt bosluk kaldirildi; ana bottom bar detay sirasinda gizleniyor.

## Kalici Zikir Profilleri

Zikir sayfasi tek ayari ezmek yerine birden fazla kalici profil sakliyor.

Her profil su alanlara sahip:

```swift
struct ZikirProfile: Codable, Identifiable {
    let id: UUID
    var name: String
    var target: Int
    var groupCount: Int
    var count: Int
}
```

Davranislar:

- Yeni profil ad, tur hedefi ve tur sayisiyla kaydedilir.
- Her profil kendi `count` degerini korur.
- Listeden profile dokunmak aktif profili degistirir.
- Aktif profil listede isaretlenir.
- Birden fazla profil varsa profil silinebilir; son profil silinemez.
- Eski tekli ayar ilk acilista otomatik olarak ilk profile donusturulur.
- Widget her zaman aktif profili gosterir.
- Alt sekme ve kullaniciya gorunen sayfa adi `Zikir`; dahili Android sinif adlari geriye uyumluluk icin `Zikirmatik` kalabilir.

iOS'ta profiller App Group icindeki `Codable` bir liste olarak saklanmali. `activeProfileId` ayri tutulmali; widget timeline'i profil secimi, sayim, sifirlama ve silme sonrasinda yenilenmeli.

### Zikir sayaci geometrisi

- Ana dokunma alani ve progress halkasi ayni `276dp` daireyi kullanir.
- Icerik padding'i sifirdir; aksi halde progress halkasi arka plan dairesinden kucuk kalir.
- Halka kalinligi `9dp`, uclari yuvarlaktir.
- Sayac artirma tum buyuk daireye dokunarak yapilir.
- Azaltma ve sifirlama altta ayri ikon eylemleridir.
- Sayac, progress bilgisi ve butonlar erisilebilir aciklamalara sahiptir.

SwiftUI'da arka plan ve progress ayni frame'i paylasmali:

```swift
ZStack {
    Circle().fill(Color.primaryContainer)
    Circle()
        .trim(from: 0, to: progress)
        .stroke(Color.primary, style: StrokeStyle(lineWidth: 9, lineCap: .round))
        .rotationEffect(.degrees(-90))
    counterContent
}
.frame(width: 276, height: 276)
.contentShape(Circle())
```

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
2. Sonraki namaz hesaplamasini `Gunes -> Ogle -> Ikindi -> Aksam -> Yatsi -> Gunes` sirasiyla ve gece gecisiyle test et.
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
- Widget sayaci Imsak'i hedef olarak gostermemeli.
- Gece yarisi ve Yatsi sonrasi ertesi gun Gunes gecisi dogru calismali.
- WidgetKit preview'lari `systemSmall`, `systemMedium` ve `systemLarge` icin kontrol edilmeli.

## Android Uygulamasindan Cikarilan Notlar

- Widget host'lar dinamik UI API'lerinin tamamini desteklemeyebilir. Android'de `setTextAppearance` Samsung Launcher'da `RemoteViews.ActionException` uretti.
- Widget yapilandirmasi tamamlanmadan once gecerli ilk gorunum yayinlanmali; aksi halde launcher bos gorunumu "Widget eklenemedi" olarak yorumlayabilir.
- Boyut kararinda yalnizca bildirilen minimum yukseklige guvenmemek gerekir. Samsung tarafinda gercek options degerleri `97dp` ve `215dp` olarak geldi.
- Metin satir sayisini tamamen sinirsiz yapmak kucuk widget'ta tasma yaratti. Boyuta bagli satir butcesi daha guvenli.
- Dakikalik namaz yenilemesinde her dakika ag istegi yapilmamali; cache uzerinden hesaplama yapilmali.
