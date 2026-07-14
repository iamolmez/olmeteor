# Meteor Setup Rehberi

Setup sistemi; schematic, yaratik noktalari, ganimet noktalari, hologramlar ve merkez blogunu birlikte kaydeder. Oyuncunun normal envanteri setup boyunca yedeklenir ve cikista geri verilir.

---

## Setup Baslatma

```
/olmeteor setup <tip>
```

Komut iki secenek gosterir:

| Secenek | Aciklama |
|---|---|
| Sifirdan olustur | Bos bir setup oturumu acar |
| Kayitli schematic kullan | Var olan bir schematic'i tipe baglar |

### Var olan yapiyi duzenleme:
```
/olmeteor editschematic <tip> <schematic>
```

Bu komut yapiyi oyuncunun yakinina gecici olarak yapistirir. Onceden kaydedilen mob, ganimet ve hologram noktalari da geri cagrilir.

---

## Setup Araclari

Setup moduna girdiginizde envanterinize ozel araclar gelir:

| Arac | Gorev | Kullanim Detayi |
|---|---|---|
| Blaze Rod (Turuncu) | Bolge secimi | Sol tik: 1. koseyi belirler. Sag tik: 2. koseyi belirler. |
| Recovery Compass (Yesil) | Ana kok (origin) | Schematic'in meteor merkezine hizalanacagi ana kok blogunu secer. |
| End Rod (Mor) | Mob noktasi | Sag tik: mob dogma noktasi ekler. Ayni noktaya tekrar tiklamak kaldirir. Yaratik sansi editorunu acar. |
| Chest (Kahverengi) | Ganimet noktasi | Bosa sag tik: loot GUI acar. Blog'a sag tik: bitisik konuma ganimet blogu ekler/kaldirir. Shift + container tik: icindekileri loot tablosuna aktarir. |
| Name Tag (Mavi) | Hologram | Sag tiklanan blogun 2 blok ustune hologram/yazi noktasi ekler. |
| Barrier (Kirmizi) | Guvenli cikis | Kaydetmeden cikar, gecici goruntuleri temizler ve envanteri geri verir. |
| Bedrock (Siyah) | Kaydet | Ayarlari/schematic'i kaydeder ve cikis temizligi secimini gosterir. |

### Setup Actionbar Gostergesi:
```
P1 saved | P2 saved | Mob: 5 | Chest: 3 | Hologram: 2
```

---

## MythicMobs Agirligi Nedir?

Agirlik (Weight), bir mobun diger secili moblara gore dogma olasiligidir.

- 0 --> Bu mob dogmaz
- Daha yuksek deger --> Secimde daha sik tercih edilir
- Agirliklarin toplaminin 100 olmasi zorunlu degildir; degerler birbirine oranlanir

Ornek:
```
Zombi  : 75  -- Dogumlarin %75'i Zombi
Iskelet: 25  -- Dogumlarin %25'i Iskelet
```

Editorde degerler `0 -> 25 -> 50 -> 75 -> 100` seklinde degisir.

---

## Birden Fazla Nokta

Ayni schematic'e birden fazla mob, ganimet ve hologram noktasi eklenebilir. Butun noktalar ana kok bloguna gore offset olarak kaydedilir.

Ornek yerlestim:
```
    Hologram
    (ana kok + 0, +2, 0)

  Chest           Chest
  (kok -2)       (kok +2)

    Ana Kok Blogu
  (Recovery Compass ile secilen)

  Mob             Mob
  (kok -3)       (kok +3)
```

Boylece yapi baska koordinata yapistirildiginda noktalar da dogru yere tasinir.

---

## Kaydetme ve Temizleme

Bedrock aracina bastiktan sonra:

| Secenek | Ne Olur |
|---|---|
| Sil | Setup icin yerlestirilen gecici arazi snapshot'tan geri alinir |
| Birak | Yapi dunyada kalir; setup araclari ve oturum temizlenir |

Not: Gecerli schematic isimlerinde yalnizca guvenli dosya karakterleri kullanin (harf, rakam, _, -). Schematic islemleri icin FAWE veya uyumlu WorldEdit kurulumu gereklidir.

---

## Adim Adim Setup

```
1. /olmeteor setup small
   -> "Sifirdan olustur" secenegine tikla

2. Blaze Rod ile koseleri belirle
   -> Sol tik: 1. kose
   -> Sag tik: 2. kose

3. Recovery Compass ile ana koku sec
   -> Yapinin merkezindeki blog'a tikla

4. End Rod ile mob noktalari ekle
   -> Istedigin yerlere sag tikla

5. Chest ile ganimet noktalari ekle
   -> Bloklarin yanina sag tikla

6. Name Tag ile hologram metni ekle
   -> /olmeteor settext "Hos Geldiniz!"

7. Bedrock'a tikla -> KAYDET
   -> "Sil" veya "Birak" secenegini sec

8. Ganimeti duzenle
   -> /olmeteor loot small
```

---

## Sik Yapilan Hatalar

| Sorun | Cozum |
|---|---|
| Schematic kaydedilmiyor | FAWE/WorldEdit hook'unu /olmeteor debug ile kontrol edin |
| Noktalar yanlis yerde | Ana kok blogunu Recovery Compass ile yeniden secin |
| Moblar dogmuyor | MythicMobs ID'sini kontrol edin, agirligi 0 olmadigindan emin olun |
| Setup'tan cikilamiyor | /olmeteor setup <tip> yazarak cikis menusunu tekrar acin |
