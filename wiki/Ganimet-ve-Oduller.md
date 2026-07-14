# Ganimet ve Oduller

OlMeteor uc odul katmanini birlikte destekler: ganimet GUI'si, siralama odulleri ve konsol komutlari.

---

## Ganimet GUI'si

```
/olmeteor loot <tip>
```

### Kullanim:
| Islem | Nasil Yapilir |
|---|---|
| Esya ekle | Normal/ozel esyayi GUI'ye surukleyin |
| Sans duzenle | Esyaya sol tik |
| Miktar duzenle | Esyaya sag tik |
| Kilit ac/kapa | Esyaya Shift + tik |
| Esya sil | Esyaya Q tusu |
| Kaydet | Yesil Kaydet dugmesi |
| Iptal | Kirmizi Iptal dugmesi |
| Temizle | Temizle dugmesi |
| Sifirla | Varsayilana Dondur dugmesi |

### Hizli Aktarim:
Setup sirasinda Chest araciyla bir container'a Shift + sag tiklamak, icindeki esyalari tabloya aktarir.

### Veri Korumasi:
ItemsAdder verileri, PDC, vanilla/custom NBT ve 1.21 Data Components korunur. NBTAPI yukluyse ek entegrasyon kullanilir ancak temel veri korumasi NBTAPI'ye bagli degildir.

---

## Kisisel ve Ortak Ganimet

Her meteor tipi icin ganimet ayarlari:

```yaml
meteor-types:
  small:
    loot:
      block: "CHEST"
      access-mode: "AUTO"
      personal: true
      inventory-title: "Kucuk Meteor Odulu"
```

### Erisim Modlari

| Mod | Davranis |
|---|---|
| AUTO | Sandik/varil -> sag tik | Normal blok -> kazma |
| INTERACT | Her zaman sag tik ile acilir |
| BREAK | Blok kirilmaya calisilinca acilir |
| BOTH | Hem sag tik hem kazma ile acilir |

### Kisisel vs Ortak:

| Ozellik | personal: true | personal: false |
|---|---|---|
| Her oyuncu kendi envanterini gorur | Evet | Hayir |
| Tum oyuncular ayni sandigi gorur | Hayir | Evet |
| Ilk alan goturur | Kisisel | Ortak |

### Kullanilabilir Bloklar:
```yaml
block: "CHEST"
block: "BARREL"
block: "ANCIENT_DEBRIS"
block: "CRYING_OBSIDIAN"
block: "RESPAWN_ANCHOR"
block: "ENDER_CHEST"
block: "SHULKER_BOX"
```

Herhangi bir blok kullanilabilir. Eger blok dogal envantere sahip degilse BREAK veya BOTH modunda kazilinca acilir.

---

## Kimler Odul Alabilir?

```yaml
event:
  vault:
    boss-damage-threshold-percent: 10
    reward-top-count: 3
```

| Ayar | Aciklama |
|---|---|
| boss-damage-threshold-percent | Oyuncunun boss'a vermesi gereken minimum hasar yuzdesi |
| reward-top-count | Hasar siralamasinda kac oyuncunun odul blogunu acabilecegi |

Ornek: 10 oyuncu meteor boss'una saldirdi. reward-top-count=3 oldugunda, sadece hasar siralamasinda ilk 3 oyuncu odul sandigini acabilir. Boss'a en az %10 hasar vermeyen oyuncular siralamaya girmez.

---

## Siralama Odulleri

```yaml
meteor-types:
  legendary:
    ranking-rewards:
      "1":
        items:
          - "NETHERITE_INGOT:2"
        commands:
          - "eco give %player% 5000"
      "2":
        items:
          - "DIAMOND:8"
        commands: []
      "3":
        items:
          - "DIAMOND:4"
        commands: []
```

### Ornek Odul Konfigurasyonlari:

Birincilik:
```yaml
"1":
  items:
    - "NETHERITE_INGOT:3"
    - "DIAMOND:16"
  commands:
    - "eco give %player% 10000"
    - "minecraft:xp add %player% 500 points"
```

Ikincilik:
```yaml
"2":
  items:
    - "DIAMOND:8"
  commands:
    - "eco give %player% 5000"
```

### Odul Komutlari Ipuclari:

Onerilen - Namespace kullanimi:
```yaml
commands:
  - "minecraft:xp add %player% 100 points"
```

Kacinilmasi gereken - EssentialsX ile cakisma:
```yaml
commands:
  - "xp give %player% 100"
```

`%player%` kazananin adiyla degistirilir. XP icin Essentials'in `xp` komutu yerine vanilla ad alani kullanmaniz onerilir. Bu kullanim, EssentialsX dil paketi/komut cakismalarindan dogan `MissingResourceException` hatalarini onler.

---

## Geri Bildirimleri Kapatma

Genel veya meteor tipi ozelinde asagidakiler kapatilabilir:

```yaml
combat-feedback:
  damage-actionbar: true
  kill-actionbar: true
  broadcast-leaderboard: true
  leaderboard-size: 5
```

### Actionbar Formatlari:
```yaml
damage-actionbar-text: "Hasar: %damage% | Sira: #%rank%"
kill-actionbar-text: "Mob olduruldu! Kalan: %remaining% | Sira: #%rank%"
```

### Siralama Formatlari:
```yaml
leaderboard-title: "Meteor Hasar Siralamasi"
leaderboard-entry: "#%rank% %player% - %damage% hasar"
leaderboard-empty: "Bu meteor icin oyuncu hasari kaydedilmedi."
```

---

## Ozet: Odul Katmanlari

| Katman | Kaynak | Tetikleyici |
|---|---|---|
| 1 | Ganimet GUI'si | /olmeteor loot <tip> ile duzenlenir, sandik acilinca verilir |
| 2 | Siralama Odulleri | ranking-rewards ile tanimlanir, boss olunce otomatik verilir |
| 3 | rewards-commands | rewards-commands ile tanimlanir, sandik acilinca calisir |

---

## Kalici Oyuncu Istatistikleri

Oyuncularin meteor etkinliklerindeki basarilari `player-stats.yml` dosyasinda kalici olarak saklanir:

| Istatistik | Aciklama | Placeholder |
|---|---|---|
| damage | Toplam verilen hasar | %olmeteor_player_damage% |
| kills | Oldurulen meteor yaratik sayisi | %olmeteor_player_kills% |
| loot-claims | Alinan ganimet sayisi | %olmeteor_player_loot% |
| ranking-rewards | Alinan siralama odulu sayisi | (eklenti placeholderi yok) |

Not: `/olmeteor stats [oyuncu]` komutu tum 4 istatistigi gosterir:
- Toplam hasar, mob oldurme, ganimet sayisi ve siralama odulu sayisi
