# OlMeteor Wiki

OlMeteor; Paper ve Folia sunuculari icin hazirlanmis, tamamen ozellestirilebilir bir meteor etkinligi eklentisidir. Schematic tabanli carpma alanlari, animasyonlu dusus, dalgali yaratik sistemi, kisisel ganimetler, hasar siralamasi, otomatik etkinlikler ve guvenli arazi geri yukleme ozelliklerini tek bir pakette sunar.

> Bu dokumantasyon **OlMeteor 1.4.0** ve **Paper/Folia 1.21.1+** icin hazirlanmistir. Sunucuda **Java 21** kullanilmalidir.

---

## Ozellikler

### Meteor Siniflari

| Sinif | Zorluk | Etki Yaricapi |
|---|---|---|
| Small | Kolay | 15 blok |
| Medium | Normal | 25 blok |
| Large | Zor | 40 blok |
| Epic | Epik | 60 blok |
| Legendary | Efsanevi | 80 blok |

### Setup ve Schematic Sistemi
- Oyun icinden gorsel schematic olusturma ve kayitli schematic duzenleme
- Tek bir schematic icine birden fazla yaratik, hologram ve ganimet noktasi ekleme
- FAWE/WorldEdit ile anlik snapshot ve geri yukleme (rollback)

### Savas ve Dalga Sistemi
- MythicMobs yaratik ve boss destegi
- Dalga sistemi ile asamali yaratik akinlari
- Ture ozel dusus animasyonlari (instant, normal, slow)
- Hasar/oldurme ActionBar bildirimi ve odullu liderlik siralamasi

### Ganimet ve Odul Sistemi
- Kisisel veya ortak ganimet sandigi
- Sandik, varil, antik kalinti gibi istenilen blok kullanimi
- ItemsAdder, PDC, NBT ve 1.21 Data Components verilerini koruyan ganimet editoru
- Siralama bazli oduller (esya + komut)

### Otomatik Sistem
- Dunya, yuzey, Y araligi, sure ve yaricap kontrollu otomatik meteorlar
- WorldGuard ve Towny bolgelerinden kacinma
- TPS korumasi ile dusuk performansta erteleme

### Guvenlik ve Geri Yukleme
- Etkinlik sonunda oyuncu degisiklikleri dahil araziyi geri yukleme
- Cokme veya yeniden baslatma sonrasinda snapshot kurtarma
- PDC imzali meteor biletleri ile guvenli cagirma

### Entegrasyonlar

| Eklenti | Turu |
|---|---|
| CommandAPI | Zorunlu |
| FAWE / WorldEdit | Onerilen |
| WorldGuard | Istege bagli |
| Towny | Istege bagli |
| MythicMobs | Istege bagli |
| PlaceholderAPI | Istege bagli |
| ItemsAdder | Istege bagli |
| FancyHolograms | Istege bagli |
| NBTAPI | Istege bagli |

---

## Hizli Baslangic

1. **Kurulum** sayfasindaki bagimliliklari yukleyin.
2. `/olmeteor setup small` ile ilk meteor yapinizi olusturun.
3. `/olmeteor loot small` ile ganimet tablosunu duzenleyin.
4. `/olmeteor spawnat small ~ ~ ~ world normal` ile test edin.
5. Hazir oldugunuzda otomatik meteor sistemini acin.

---

## Wiki Sayfalari

### Baslangic
- **Kurulum** - Gereksinimler, kurulum adimlari ve guncelleme
- **Hizli Baslangic** - Ilk meteorunuzu olusturma ve test etme

### Yonetim
- **Komutlar ve Yetkiler** - Tum komutlar, yetkiler ve kullanim ornekleri
- **Meteor Setup Rehberi** - Gorsel setup sistemi ve araclari
- **Otomatik Meteorlar** - Otomatik zamanlama ve konum bulma
- **Ganimet ve Oduller** - Loot tablosu, siralama ve oduller
- **Yapilandirma** - Config.yml detayli aciklamalari

### Entegrasyon ve API
- **Entegrasyonlar** - Desteklenen eklentiler ve hook sistemi
- **PlaceholderAPI** - Tum placeholderlar ve kullanim ornekleri
- **Gelistirici API'si** - Plugin gelistiricileri icin API dokumantasyonu

### Guncel
- **Surum Notlari** - Guncel degisiklikler ve surum gecmisi

### Destek
- **SSS** - Sik sorulan sorular
- **Sorun Giderme** - Sik karsilasilan sorunlar ve cozumleri

---

## Ek Ozellikler

### Ayarlanabilir Zorluk Sistemi
Her meteor tipinin kod ici zorluk seviyesi (Easy, Normal, Hard, Epic, Legendary) ve reward slot sayilari (Small: 10-25, Legendary: 75-150) bulunur.

### Dusus Modu Alias'lari
Dusus modlari icin birden fazla isim destegi:
- `instant` = aninda, fast
- `normal` = varsayilan
- `slow` = cinematic, yavas

### BossBar Takip
Aktif meteorlar oyunculara otomatik BossBar ile gosterilir: mesafe, yon, kalan yaratik sayisi ve sure bilgisi.

### Setup Komut Engelleyici
Setup modunda /clear, /tp, /spawn, /home gibi tehlikeli komutlar engellenir. /olmeteor ve /msg gibi komutlara izin verilir.

### Envanter Yedekleme
Setup baslangicinda envanter (armor, offhand, XP, can, yemek dahil) diske yedeklenir. Cikista otomatik geri yuklenir ve silinir.

### Crash Kurtarma
Sunucu cokmesi durumunda yarim kalan snapshotlar otomatik tespit edilir ve geri yuklenir.

### Kalici Istatistikler
4 farkli istatistik kalici olarak saklanir: toplam hasar, mob oldurme, ganimet sayisi, siralama odulu sayisi.

### Varsayilan Meteor Schematic
FAWE yukluyse ilk baslatista 13x6x13 boyutunda varsayilan bir krater schematic otomatik olusturulur.

---

> **ONEMLI:** Eklentiyi veya bagimliliklarini degistirdikten sonra `/reload` kullanmayin. Sunucuyu tamamen kapatip yeniden baslatin. CommandAPI, WorldGuard, FAWE ve Folia zamanlayicilari canli reload sirasinda guvenli degildir.
