# Otomatik Meteorlar

Otomatik sistem her tetiklemede sure, dunya, uzaklik, yuzey ve claim kurallarina uygun yeni bir konum bulur. Birden fazla meteor tipi tek kurala eklenebilir.

---

## Genel Bakis

Otomatik Meteor Dongusu:
1. Zamanlayici tetiklenir (min-max araligi)
2. Uygun dunya secilir
3. Konum arayici devreye girer
4. Yuzey/Y araligi/claim kontrolleri yapilir
5. Uygun konum bulunursa meteor baslatilir
6. Konum cooldown'a eklenir
7. Bir sonraki zaman planlanir

---

## Oyun Ici Menu

```
/olmeteor auto
```

Bu komut oyuncuda auto ayar GUI'sini acar. Konsolda calistirilirsa mevcut durum gosterilir.

---

## Her Seyi Tek Komutta Ayarlama

```
/olmeteor auto ayarla <meteorlar> <dunya> <preset> <ganimetBlogu> <minDakika> <maxDakika> <minUzaklik> <maxUzaklik> [minY] [maxY]
```

### Parametreler

| Parametre | Aciklama | Ornek |
|---|---|---|
| meteorlar | Tek tip, virgulle ayrilmis birden fazla tip veya rastgele | small,medium,large |
| dunya | Meteorlarin aranacagi yuklu dunya | world |
| preset | Yuzey ve yukseklik kurali | flat_surface |
| ganimetBlogu | Ganimet blogu (virgulle ayirarak tiplerle eslestirilebilir) | CHEST,BARREL,ANCIENT_DEBRIS |
| minDakika | Iki meteor arasi minimum sure (dakika) | 30 |
| maxDakika | Iki meteor arasi maksimum sure (dakika) | 60 |
| minUzaklik | Dunya spawn'ina minimum uzaklik (blok) | 100 |
| maxUzaklik | Dunya spawn'ina maksimum uzaklik (blok) | 5000 |
| minY | Istege bagli minimum Y seviyesi | 120 |
| maxY | Istege bagli maksimum Y seviyesi | 220 |

---

### Ornek Kullanimlar

#### Ornek 1: Temel Kurulum
```
/olmeteor auto ayarla small,medium,large world grass_surface CHEST,BARREL,ANCIENT_DEBRIS 30 60 500 5000
```

- Meteor tipleri: Small, Medium, Large (rastgele secilir)
- Dunya: world
- Preset: Cimen yuzey
- Ganimet bloklari: Sirayla eslesir: small->CHEST, medium->BARREL, large->ANCIENT_DEBRIS
- Sure: 30-60 dakika arasi rastgele
- Uzaklik: Spawn'dan 500-5000 blok

#### Ornek 2: Rastgele Tipler
```
/olmeteor auto ayarla rastgele world any_surface CHEST 20 45 300 4000
```

#### Ornek 3: Havada Meteor
```
/olmeteor auto ayarla epic world air END_PORTAL_FRAME 45 90 1000 8000 120 220
```

---

## Hazir Konum Presetleri

| Preset | Aciklama |
|---|---|
| flat_surface | Duz ve kuru yuzey |
| water_surface | Su yuzeyi |
| underground | Yeralti |
| air | Havada (Y araligi gerekir) |
| any_surface | Duzluk gerektirmeyen yuzey |
| grass_surface | Cimen/toprak benzeri |
| desert_surface | Col yuzeyi |
| nether_surface | Nether yuzeyi |
| end_surface | End yuzeyi |

Yeni presetler `location-finder.presets` altinda olusturulabilir. `allowed-floor-blocks` listesine `GRASS_BLOCK`, `SAND` gibi Bukkit materyal adlari yazilir.

---

## Guvenlik Kontrolleri

| Ayar | Aciklama |
|---|---|
| towny-require-wilderness: true | Towny town arazilerini reddeder |
| worldguard-check-claims: true | WorldGuard bolgelerini reddeder |
| tps-guard.enabled: true | Dusuk TPS'de eventi erteler |
| location-cooldown.enabled: true | Yakin gecmiste kullanilan alani tekrar secmez |
| max-active-events: 1 | Ayni anda calisabilecek maksimum auto meteor sayisi |

WorldGuard veya Towny kurulu degilse ilgili kontrol guvenli bicimde atlanir. Claim korumasi isteniyorsa entegrasyonun sunucuda etkin oldugundan `/olmeteor debug` ile emin olun.

---

## Yonetim Komutlari

```
/olmeteor auto ac          -- Otomatik sistemi aktiflestir
/olmeteor auto kapat       -- Otomatik sistemi kapat
/olmeteor auto durum       -- Plani ve siradaki zamani goster
/olmeteor auto simdi       -- Hemen rastgele konumda meteor baslat
```

### Durum Ciktisi Ornegi:
```
=== Otomatik Meteor Durumu ===
Aktif: Evet
Meteorlar: small, medium, large
Dunya: world
Preset: flat_surface
Aralik: 30 - 60 dk
Sonraki: 24 dakika sonra
```

---

## Sik Karsilasilan Sorunlar

| Sorun | Cozum |
|---|---|
| Auto meteor baslamiyor | /olmeteor auto ac ile aktiflestirdiginizden emin olun |
| Konum bulunamiyor | Min/max uzaklik degerlerini kontrol edin, preset uygunlugunu test edin |
| Hep ayni tip geliyor | Meteor tipleri listesinde sadece bir tip oldugunu kontrol edin |
| Town/WG bolgesine dusuyor | towny-require-wilderness ve worldguard-check-claims acik mi kontrol edin |
