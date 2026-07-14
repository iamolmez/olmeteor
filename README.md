# OlMeteor

Paper ve Folia sunuculari icin tamamen ozellestirilebilir meteor etkinligi eklentisi.

**Version:** 1.3.0 | **Java:** 21 | **Paper/Folia:** 1.21.1+

---

## Ozellikler

| Ozellik | Aciklama |
|---|---|
| 5 Meteor Sinifi | Small, Medium, Large, Epic, Legendary |
| Schematic Sistemi | Oyun ici gorsel setup, FAWE/WorldEdit destegi |
| Dalga Sistemi | Asamali yaratik akini ve MythicMobs boss |
| Ganimet Sistemi | Kisisel/ortak ganimet, ItemsAdder/NBT/PDC korumasi |
| Otomatik Sistem | Zamanlanmis meteorlar, TPS korumasi, claim kacinma |
| Rollback | Arazi geri yukleme, cokme kurtarma |
| PlaceholderAPI | 14+ placeholder ile scoreboard entegrasyonu |
| Entegrasyonlar | WorldGuard, Towny, MythicMobs, ItemsAdder, NBTAPI, FancyHolograms |

---

## Hizli Kurulum

### Gereksinimler

| Bilesen | Durum |
|---|---|
| Paper veya Folia 1.21.1+ | Zorunlu |
| Java 21 | Zorunlu |
| CommandAPI | Zorunlu |
| FAWE / WorldEdit | Onerilen |

### Kurulum Adimlari

```
1. Sunucuyu tamamen kapatin
2. OlMeteor-1.3.0.jar ve CommandAPI JAR'ini plugins/ klasorune koyun
3. Istege bagli entegrasyonlari yukleyin
4. Sunucuyu baslatin
5. /olmeteor debug ile entegrasyonlari kontrol edin
```

---

## Hizli Baslangic

```
1. /olmeteor setup small        -- Ilk meteor setup
2. /olmeteor loot small         -- Ganimet ekle
3. /olmeteor spawnat small ~ ~ ~ world normal  -- Test et
4. /olmeteor auto ac            -- Otomatik meteorlari ac
```

---

## Dokumantasyon

Tum wiki sayfalari icin [wiki/Index.md](wiki/Index.md) sayfasina gidin.

**Dogrudan baslangic:**
- [Turkce Ana Sayfa](wiki/Home.md)
- [English Home](wiki/en/Home.md)

---

## Entegrasyonlar

| Eklenti | Turu | Ozellik |
|---|---|---|
| CommandAPI | Zorunlu | Komut agaci |
| FAWE / WorldEdit | Onerilen | Schematic ve rollback |
| WorldGuard | Istege bagli | Bolge korumasi |
| Towny | Istege bagli | Town korumasi |
| MythicMobs | Istege bagli | Ozel mob/boss |
| PlaceholderAPI | Istege bagli | Placeholderlar |
| ItemsAdder | Istege bagli | Ozel esya korumasi |
| FancyHolograms | Istege bagli | Hologram destegi |
| NBTAPI | Istege bagli | Ek NBT entegrasyonu |

---

## Meteor Siniflari

| Sinif | Zorluk | Yaricap | Boss | Dusus |
|---|---|---|---|---|
| Small | Kolay | 15 blok | Yok | Normal |
| Medium | Normal | 25 blok | Yok | Normal |
| Large | Zor | 40 blok | Var | Normal |
| Epic | Epik | 60 blok | Var | Slow |
| Legendary | Efsanevi | 80 blok | Var | Slow |

---

## Build

Proje Gradle ile yonetilmektedir:

```bash
./gradlew build
```

Cikti: `build/libs/OlMeteor-1.3.0.jar`

---

## Onemli Notlar

- `/reload` kullanmayin - CommandAPI, WorldGuard, FAWE ve Folia zamanlayicilari canli reload'da guvenli degildir
- Java 21 zorunludur
- Spigot/CraftBukkit desteklenmez, yalnizca Paper/Folia 1.21.1+
- JAR guncellemesinde eski JAR'i tamamen kaldirin

---

## Lisans

Bu proje ozel bir lisans ile korunmaktadir. Tum haklari saklidir.
