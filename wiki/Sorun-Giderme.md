# Sorun Giderme

## Hizli Tanilama

Sorun yasadiginizda ilk olarak asagidaki komutlari calistirin:

```
/olmeteor debug       -- Sistem ve entegrasyon durumu
/olmeteor list        -- Aktif meteorlar
/olmeteor auto durum  -- Otomatik meteor plani
```

---

## Plugin Acilmiyor

Kontrol listesi:
- Java 21 kullaniliyor mu?
- Paper/Folia 1.21.1+ kullaniliyor mu?
- CommandAPI yuklu ve once yukleniyor mu?
- /reload sonrasi mi oldu? (Sunucuyu tamamen yeniden baslatin)
- Eski JAR'lar temizlenmis mi?
- Konsol loglari kontrol edilmis mi?

---

## NoClassDefFoundError

Hata:
```
java.lang.NoClassDefFoundError: com.sk89q.worldguard...
```

Nedenleri:
1. Eski JAR - OlMeteor'un guncel olmayan bir surumu
2. Bozuk WorldGuard kurulumu - WorldGuard JAR'i hasarli veya uyumsuz
3. Canli reload - /reload sonrasi olusan sinif yukleme sorunu

Cozum:
1. Sunucuyu kapatin
2. Eski OlMeteor JAR'larini kaldirin (yalnizca son surumu birakin)
3. WorldGuard ve WorldEdit/FAWE surumlerinin uyumlu oldugunu dogrulayin
4. Sunucuyu tam baslatin (PlugMan kullanmayin)

---

## Schematic Cikmiyor veya Geri Yuklenmiyor

Kontrol listesi:
- FAWE/WorldEdit hook'u /olmeteor debug ile aktif mi?
- Meteor tipinde schematic dosya adi dogru mu?
- Setup sirasinda iki kose ve ana kok blogu secildi mi?
- restore-structure-on-finish: true ayari acik mi?
- Snapshot/recovery klasorune yazma izni var mi?

Eventi zorla durdurmak yerine normal bitisi veya /olmeteor stop <id> akisini test edin. Schematic dosyalarini plugins/OlMeteor/schematics/ klasorune koyun. Dosya adinda Turkce karakter ve bosluk kullanmayin.

---

## Moblar Yanlis Yerde Doguyor

1. /olmeteor editschematic <tip> <schematic> ile yapiyi acin
2. Ana kok blogunu Recovery Compass ile yeniden secin
3. End Rod ile mob noktalarini yeniden kaydedin
4. Ayni noktaya tekrar tiklamanin noktayi sildigini unutmayin
5. MythicMobs mob ID'sinin birebir dogru oldugunu kontrol edin

| Sorun | Cozum |
|---|---|
| Ana kok yanlis secilmis | Recovery Compass ile yeniden secin |
| Mob noktasi silinmis | End Rod ile tekrar ekleyin |
| MythicMobs ID hatali | /olmeteor selectmob ile dogru ID'yi secin |
| Agirlik 0 | Editorde agirligi 0'dan buyuk yapin |

---

## Ganimet Blogu Gorunmuyor veya Acilamiyor

Adim adim kontrol:
1. Setup icinde Chest araciyla ganimet noktasi ekleyin
2. loot.block degerinin gecerli Bukkit materyali oldugunu dogrulayin
3. Moblarin/bossun oldugunu kontrol edin
4. Oyuncunun hasar siralamasinda hak kazandigini kontrol edin
5. reward-top-count ve boss-damage-threshold-percent degerlerini kontrol edin

| Durum | Cozum |
|---|---|
| Ganimet noktasi eklenmemis | Setup'ta Chest araciyla ekleyin |
| Blok adi yanlis | CHEST gibi gecerli bir Bukkit ID'si kullanin |
| Boss henuz oldurulmedi | Once boss'u oldurun |
| Oyuncu siralamada degil | Daha fazla hasar verin |

---

## Meteor Town veya WorldGuard Bolgesine Dustu

Bu kontroller otomatik konum bulucu icindir; manuel spawnat kesin yonetici konumunu kullanir.

Cozum:
1. towny-require-wilderness: true ayarini acin
2. worldguard-check-claims: true ayarini acin
3. /olmeteor debug ciktisinda iki hook'un da aktif oldugunu dogrulayin
4. Auto eventin guncel kuralla calistigini /olmeteor auto durum ile kontrol edin

---

## EssentialsX XP Hatasi

Odul komutunda `xp` baska bir plugin tarafindan ele gecirilebilir.

Dogru kullanim:
```yaml
commands:
  - "minecraft:xp add %player% 100 points"
```

Yanlis kullanim:
```yaml
commands:
  - "xp give %player% 100"
```

---

## Auto Meteor Baslamiyor

Kontrol listesi:
- /olmeteor auto ac ile sistem aktiflestirilmis mi?
- Min/max sure degerleri dogru sirada mi?
- air presetinde minY ve maxY verilmis mi?
- TPS guard siniri mevcut TPS'den yuksek degil mi?
- Claim, yuzey ve cooldown kurallari uygun nokta birakiyor mu?
- Tanilama icin /olmeteor auto simdi kullanilmis mi?

---

## Destek Alirken

Sorun bildirirken asagidaki bilgileri ekleyin:

1. Sunucu surumu (Paper 1.21.3 vb.)
2. OlMeteor surumu
3. Yuklu entegrasyon surumleri
4. Hatanin ilk satirindan son 'Caused by' bolumune kadar TAM LOG
5. /olmeteor debug ciktisi
