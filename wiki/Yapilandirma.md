# Yapilandirma

Ana dosya `plugins/OlMeteor/config.yml` yolundadir. Degisikliklerden sonra `/olmeteor reload` kullanilabilir; JAR veya bagimlilik guncellemesinde tam yeniden baslatma gerekir.

---

## Ana Bolumler

| Bolum | Gorev |
|---|---|
| locale | Dil secimi: auto, tr, en |
| integrations | NBT ve diger istege bagli entegrasyon anahtarlari |
| command-help | Yardim menusu kategorileri, sirasi ve metinleri |
| automatic-events | Auto zamanlama, TPS korumasi, dunyalar ve konum cooldown'u |
| schematic | Varsayilan schematic dosyasi |
| location-finder | Yuzey/Y araligi/claim kurallari ve presetler |
| event | Dalga, bilet, kurtarma, takip, temizleme, hazard ve vault ayarlari |
| meteor-types | Her meteor tipinin bagimsiz gorunum, sure, loot, mob ve odul ayarlari |
| messages | Eski config ici mesajlar; normalde lang/messages_tr.yml kullanilir |

---

## Dil

```yaml
locale: tr
locale-config-overrides: false
```

- `locale: auto` --> JVM sistem diline gore otomatik algilama
- `locale: tr` --> Turkce
- `locale: en` --> Ingilizce

`locale-config-overrides: false` oldugunda `lang/messages_tr.yml` kullanilir. Kendi dilinizi eklemek icin `lang/messages_XX.yml` olusturabilirsiniz.

---

## Dalga Sistemi

```yaml
event:
  waves:
    count: 3
    interval-seconds: 15
```

Normal yaratiklar belirlenen sayida dalga halinde gelir. Boss son dalgadan sonra dogar.

---

## Meteor Bileti

```yaml
event:
  tickets:
    material: "FIRE_CHARGE"
    cooldown-seconds: 300
```

Biletler PDC ile imzalanir; yalnizca adi degistirilmis sahte esyalar meteor baslatamaz. Bu sistem bilet kopyalamayi ve hileli kullanimi engeller.

---

## Temizleme ve Kurtarma

```yaml
event:
  recovery:
    enabled: true
  completion:
    cleanup-delay-seconds: 60
    unattended-timeout-minutes: 30
```

| Ayar | Aciklama |
|---|---|
| cleanup-delay-seconds | Moblar oldukten ve bitis sartlari saglandiktan sonra temizleme beklemesi |
| unattended-timeout-minutes | Kimse katilmazsa eventin en fazla acik kalacagi sure |
| recovery.enabled | Sunucu cokmesi/restart sonrasinda yarim kalan snapshotlari geri yukler |

Her meteor icin `restore-structure-on-finish: true` oldugunda schematic alani, oyuncularin kirdigi veya ekledigi bloklar dahil eski haline doner.

---

## Dusus Animasyonu

```yaml
event:
  fall:
    normal-height: 80
    slow-height: 120
    show-impact-core: false

meteor-types:
  small:
    fall-mode: "normal"
    normal-fall-duration-seconds: 8
    slow-fall-duration-seconds: 18
```

---

## Tip Bazli Ayarlar

Her meteor tipi (small, medium, large, epic, legendary) asagidaki ozelliklere sahiptir:

### Reward Slot Sayilari (Kod ici)

| Tip | Min - Max Slot | Zorluk |
|---|---|---|
| Small | 10 - 25 | Easy |
| Medium | 20 - 50 | Normal |
| Large | 35 - 75 | Hard |
| Epic | 50 - 100 | Epic |
| Legendary | 75 - 150 | Legendary |

Bu degerler her tipin ganimet sandiginda kac farkli esya olabilecegini belirler. Rastgele bir sayi secilir.

### Temel Ayarlar
```yaml
meteor-types:
  small:
    restore-structure-on-finish: true
    fall-mode: "normal"
    impact-radius: 15
    pre-impact-duration-seconds: 30
    event-duration-seconds: 300
    rollback-duration-seconds: 30
    boss-health-multiplier: 1.0
```

### Ganimet Ayarlari
```yaml
    loot:
      block: "CHEST"
      access-mode: "AUTO"
      personal: true
      inventory-title: "Kucuk Meteor Odulu"
```

### MythicMobs Ayarlari
```yaml
    boss-mythicmob: ""
    mythicmobs: []
    mythicmob-chances: {}
```

### Siralama Odulleri
```yaml
    ranking-rewards:
      "1":
        items: ["NETHERITE_INGOT:2"]
        commands: ["eco give %player% 5000"]
      "2":
        items: ["DIAMOND:8"]
        commands: []
```

### Nokta Offsetleri
```yaml
    mob-spawn-offsets: []
    hologram-offsets: []
    chest-offsets: []
```

---

## Ornek Konfigurasyonlar

### Ornek 1: Kolay Baslangic (Small)
```yaml
meteor-types:
  small:
    restore-structure-on-finish: true
    impact-radius: 15
    event-duration-seconds: 300
    fall-mode: "normal"
    loot:
      block: "CHEST"
      personal: true
    ranking-rewards:
      "1":
        items: ["DIAMOND:5", "IRON_INGOT:10"]
        commands: ["xp give %player% 100 points"]
```

### Ornek 2: Zorlu Boss Mucadelesi (Epic)
```yaml
meteor-types:
  epic:
    restore-structure-on-finish: true
    impact-radius: 60
    event-duration-seconds: 600
    fall-mode: "slow"
    loot:
      block: "CRYING_OBSIDIAN"
      personal: true
    boss-mythicmob: "EpicMeteorBoss"
    boss-health-multiplier: 3.0
```

---

## Event Yasam Dongusu (EventPhase)

Her meteor asama asama ilerler:

| Asama | Aciklama | Sure |
|---|---|---|
| SCHEDULED | Meteor zamanlandi, henuz baslamadi | Kisa |
| PRE_IMPACT | On uyari asamasi, gorsel efektler, ekran titremesi | 30 saniye (config) |
| IMPACT | Meteor carpti, schematic yapistiriliyor, dalgalar basliyor | ~2 saniye |
| ACTIVE | Aktif etkinlik - hazardlar acik, boss dogdu, ganimet erisilebilir | 300 saniye (config) |
| ROLLBACK | Alan temizleniyor ve eski haline donuyor | 30 saniye (config) |
| COMPLETED | Etkinlik basariyla tamamlandi | - |
| CANCELLED | Etkinlik iptal edildi veya basarisiz oldu | - |

Her asama kendi davranislarini belirler:
- **Hazards (Tehlikeler)**: Sadece ACTIVE asamasinda etkindir
- **Vault (Ganimet)**: Sadece ACTIVE asamasinda erisilebilir
- **Boss**: Sadece ACTIVE asamasinda dogar

---

## BossBar Takip Sistemi

Aktif meteor etkinligi sirasinda oyunculara otomatik BossBar gosterilir:

```yaml
event:
  tracking:
    enabled: true
    max-distance: 2000
```

BossBar'da sunlar gosterilir:
- Meteor tipi ve rengi
- Oyuncuya olan mesafe (metre cinsinden)
- Yon bilgisi (Dogu/Bati/Kuzey/Guney)
- Kalan yaratik sayisi
- Kalan sure (progress bar)

---

## Komut Yardim Kategorileri (command-categories.yml)

Ayri bir `command-categories.yml` dosyasindan yardim kategorileri okunur. Her kategori:
- `enabled: false` ile gizlenebilir
- `title` ile baslik metni belirlenir
- `commands` ile gosterilecek komutlar listelenir

---

## Onemli Uyarilar

- YAML girintilerinde sekme kullanmayin; her seviye icin 2 bosluk kullanin
- Degisiklikten once dosyanin yedegini alin
- Dosya boyutu cok buyurse yorum satirlarini temizleyebilirsiniz
