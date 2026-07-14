# Hizli Baslangic

Bu ornekte kucuk (small) bir meteor yapisi olusturacak, ganimet ekleyecek ve belirli bir noktada test edecegiz.

---

## Adim 1: Setup Modunu Acin

```
/olmeteor setup small
```

Sohbette gelen iki secenek:

| Secenek | Aciklama |
|---|---|
| Sifirdan olustur | Bos bir setup oturumu baslatir |
| Kayitli schematic kullan | Var olan bir schematic'i tipe baglar |

Sifirdan olustur secenegine tiklayin. Envanteriniz gecici olarak yedeklenir ve size setup araclari verilir. Ekranin ust kisminda bir actionbar durum gostergesi belirir.

---

## Adim 2: Yapiyi Hazirlayin

Setup sirasinda asagidaki araclar kullanilir:

| Arac | Gorev | Kullanim |
|---|---|---|
| Blaze Rod | Bolge secimi | Sol tik: 1. kose - Sag tik: 2. kose |
| Recovery Compass | Ana kok blogu | Schematic'in hizalanacagi merkez blogu secer |
| End Rod | Yaratik noktasi | Sag tik: mob dogma noktasi ekler/kaldirir |
| Chest | Ganimet noktasi | Blog'a sag tik: ganimet noktasi ekle/kaldir |
| Name Tag | Hologram | Blogun 2 blok ustune yazi noktasi ekler |
| Barrier | Guvenli cikis | Kaydetmeden cikar, gecici yapiyi temizler |
| Bedrock | Kaydet | Tum ayarlari kaydeder ve temizlik secenegi sunar |

### Adim Adim:

```
1. Blaze Rod ile sol tiklayarak 1. koseyi belirleyin.
2. Blaze Rod ile sag tiklayarak 2. koseyi belirleyin.
3. Recovery Compass ile ana kok blogunu (merkez) secin.
4. End Rod ile yaratik dogma noktalari ekleyin.
5. Chest ile bir veya daha fazla ganimet noktasi ekleyin.
6. Name Tag ile hologram noktalari ekleyin.
```

Not: Ayni yaratik veya ganimet noktasina tekrar tiklamak o noktayi kaldirir.

---

## Adim 3: Kaydedin

Bedrock aracina tiklayin. Bolge sectiyseniz yapi `.schem` dosyasi olarak kaydedilir. Ardindan gecici yapinin araziden silinmesini veya birakilmasini secebilirsiniz.

---

## Adim 4: Ganimeti Duzenleyin

```
/olmeteor loot small
```

Ganimet GUI'sinde:

| Islem | Nasil Yapilir |
|---|---|
| Esya ekle | Envanterden GUI'ye surukleyin |
| Sans duzenle | Esyaya sol tik |
| Miktar duzenle | Esyaya sag tik |
| Kilit ac/kapa | Esyaya Shift + tik |
| Esya sil | Esyaya Q tusu |
| Kaydet | Alt kisimdaki yesil Kaydet dugmesi |
| Sifirla | Varsayilana Dondur dugmesi |

ItemsAdder ozel esyalarinin verileri (PDC, NBT, Data Components) korunur.

---

## Adim 5: Test Meteorunu Baslatin

### Bulundugunuz noktada:
```
/olmeteor spawnat small ~ ~ ~ normal
```

### Belirli koordinatta:
```
/olmeteor spawnat small 125 80 -340 world slow
```

### Dusus Modlari:

| Mod | Aciklama |
|---|---|
| instant | Dogrudan carpma, animasyon yok |
| normal | Normal animasyonlu dusus (8-12 saniye) |
| slow | Uzun, sinematik dusus (18-25 saniye) |

---

## Adim 6: Sonucu Dogrulayin

```
/olmeteor list              -- Aktif meteorlari listele
/olmeteor info <eventId>    -- Meteor detaylarini goster
/olmeteor history 10        -- Son 10 meteor gecmisi
```

### Olay Akisi:

1. Meteor geliyor! (uyari suresi)
2. Meteor carpti! (carpma ani)
3. Dalga 1/3 basladi! (yaratik dalgalari)
4. Boss dogdu! (son dalgadan sonra)
5. Boss oldu! Ganimet acildi!
6. Alan temizleniyor... (rollback)
7. Alan geri yuklendi!

Moblar ve boss oldukten sonra hak kazanan oyuncular ganimet bloklarini acabilir. Temizleme suresi bitince schematic, sonradan kirilan veya eklenen bloklar dahil snapshot'a geri dondurulur.

---

## Ek: Onizleme Komutu

Meteor alanini blok yerlestirmeden onizleyebilirsiniz:

```
/olmeteor preview <tip>
```

15 saniyelik bir onizleme baslatir. Renk kodlari:
- **End Rod** parcaciklari: Meteor siniri
- **Alev (Flame)** parcaciklari: Yaratik dogma noktalari
- **Yesil mutluluk (Happy Villager)** parcaciklari: Ganimet noktalari

---

## Ek: Setup Komut Engelleyici

Setup modundayken tehlikeli veya aksatici komutlar otomatik olarak engellenir:

**Engellenen komutlar:** /clear, /tp, /spawn, /home, /fly, /gamemode, /gm, /kill, /stop, /reload, /pl
**Izin verilen komutlar:** /olmeteor, /msg, /tell, /r, /reply

Bu sistem setup sirasinda yanlislikla envanter kaybi, isinlanma veya sunucu durmasini onler.

**Envanter Yedekleme:**
Setup baslatildiginda envanteriniz (armor, offhand, XP seviyesi, can ve yemek dahil) `data/inventories/<UUID>.yml` dosyasina kaydedilir. Basarili bir sekilde setup'tan cikinca yedek otomatik silinir. Sunucu cokerse yedek dosyasi kalir ve geri yuklenebilir.
