# Entegrasyonlar

OlMeteor istege bagli eklentileri guvenli hook sistemiyle yukler. Bir entegrasyon bulunamazsa onun API siniflari ana eklenti baslatilirken zorla yuklenmez, boylece NoClassDefFoundError hatalari onlenir.

---

## Desteklenen Entegrasyonlar

| Eklenti | Sagladigi Ozellik | Zorunlu mu? |
|---|---|---|
| CommandAPI | /olmeteor komut agaci ve oneriler | Zorunlu |
| FAWE / WorldEdit | Schematic kaydetme, yapistirma, snapshot ve rollback | Kuvvetle onerilir |
| WorldGuard | Auto konumunda bolge cakismasi kontrolu | Hayir |
| Towny | Auto konumunda wilderness kontrolu | Hayir |
| MythicMobs | Secilebilir moblar ve bosslar | Hayir |
| PlaceholderAPI | %olmeteor_*% placeholderlari | Hayir |
| ItemsAdder | Ozel ganimet esyasi verilerini koruma | Hayir |
| FancyHolograms | Hologram saglayici algilama | Hayir |
| NBTAPI | Ek NBT okuma/yazma entegrasyonu | Hayir |

---

## WorldGuard ve Towny

```yaml
location-finder:
  towny-require-wilderness: true
  worldguard-check-claims: true
```

Auto Meteor Konum Arayici su sekilde calisir:
1. WorldGuard kontrolu (aktifse) - Region icindeki konumlari reddeder
2. Towny kontrolu (aktifse) - Town arazisindeki konumlari reddeder
3. Uygun konum bulunursa meteor baslatilir

Manuel `/olmeteor spawnat` komutu yoneticinin verdigi kesin konumu kullanir; otomatik konum filtresine tabi degildir. Claim korumasi isteniyorsa entegrasyonun aktif oldugundan `/olmeteor debug` ile emin olun.

---

## MythicMobs

MythicMobs yukluyse:
- `/olmeteor selectmob <id>` onerileri MythicMobs kayitlarindan gelir
- Setup icindeki End Rod editoruyle moblarin agirliklari ayarlanabilir
- Her meteor tipi icin boss-mythicmob ayri belirlenebilir

### Ornek Konfigurasyon:
```yaml
meteor-types:
  epic:
    boss-mythicmob: "EpicMeteorBoss"
    mythicmobs:
      - "SkeletalKnight"
      - "FireDemon"
    mythicmob-chances:
      SkeletalKnight: 50
      FireDemon: 30
```

---

## ItemsAdder ve NBT

Loot editorune gercek esyayi suruklemek en guvenli yontemdir. OlMeteor esya verisini seri hale getirirken PDC, NBT ve Data Components icerigini korur.

ItemsAdder komut odulu gerekiyorsa siralama odullerine saglayicinin konsol komutu eklenebilir:
```yaml
ranking-rewards:
  "1":
    items: []
    commands:
      - "iagive %player% namespace:ozel_kiyafet 1"
```

---

## FancyHolograms

| Durum | Davranis |
|---|---|
| Yuklu | OlMeteor otomatik algilar ve FancyHolograms API'sini kullanir |
| Yuklu degil | Varsayilan hologram sistemi (ArmorStand) kullanilir |

---

## Hook Kontrolu

```
/olmeteor debug
```

Ornek cikti:
```
=== OlMeteor Debug ===
Plugin: OlMeteor 1.4.0
Sunucu: Paper 1.21.3

Entegrasyonlar:
[AKTIF] CommandAPI
[AKTIF] WorldEdit (FAWE)
[AKTIF] WorldGuard
[PASIF] Towny (kurulu degil)
[AKTIF] MythicMobs
[AKTIF] PlaceholderAPI
[PASIF] ItemsAdder
[PASIF] FancyHolograms
[AKTIF] NBTAPI

Kurtarma:
[AKTIF] Snapshot kurtarma aktif
Bekleyen snapshot: 0
```

---

## Varsayilan Meteor Schematic

FAWE/WorldEdit yukluyse, OlMeteor ilk baslatilista otomatik olarak **varsayilan bir meteor schematic** olusturur. Bu schematic:
- `schematics/meteor_crater.schem` yolunda saklanir
- Blackstone, Magma, Obsidian ve Crying Obsidian bloklarindan olusur
- 13x6x13 boyutlarinda bir krater goruntusudur
- Herhangi bir meteor tipi icin kullanilabilir

---

## Crash Kurtarma Sistemi

OlMeteor, sunucu cokmesi veya beklenmedik kapanma durumlarinda yarim kalan etkinliklerin arazi degisikliklerini kurtarabilir:

1. Her meteor carpmasinda, alanin **snapshot** i diskteki bir .schem dosyasina kaydedilir
2. Sunucu yeniden baslatildiginda `MeteorEventManager.loadActiveEvents()` bekleyen snapshotlari tespit eder
3. Bekleyen snapshotlar otomatik olarak geri yukleme kuyruguna alinir
4. `/olmeteor debug` ile bekleyen kurtarma sayisi goruntulenebilir

---

## Sorun Giderme

Bir entegrasyon sunucuda bulundugu halde pasif gorunuyorsa:
1. Eklentinin dogru surumunu kullandiginizdan emin olun
2. Bagimlilik eklentisinin OlMeteor'dan once basariyla acildigini konsoldan dogrulayin
3. /reload yerine tam restart yapin
4. Ilk hata satirindan itibaren tam stack trace'i inceleyin

### Yaygin Nedenler:

| Sorun | Cozum |
|---|---|
| Eklenti reload sonrasi calismiyor | Tam sunucu restart i yapin |
| Eski JAR kullaniliyor | Son surumu indirin |
| Surum uyumsuzlugu | Sunucu surumune uygun eklenti surumu kullanin |
| Bagimlilik siralamasi | OlMeteor'a bagli eklentilerin once yuklendiginden emin olun |
