# Guncel Notlar

Bu sayfa OlMeteor'un guncel ve gecmis surumlerindeki degisiklikleri icerir.

---

## Surum 1.3.0 (Guncel)

**Yayinlanma:** 2024 | **Gereken Java:** 21 | **Sunucu:** Paper/Folia 1.21.1+

### Yeni Ozellikler

#### Meteor Etkinlik Sistemi
- 5 farkli meteor sinifi: Small, Medium, Large, Epic, Legendary
- Her sinifin kendi etki yaricapi, reward slot sayisi, boss can carpani ve zorluk seviyesi
- 7 asamali event yasam dongusu: SCHEDULED, PRE_IMPACT, IMPACT, ACTIVE, ROLLBACK, COMPLETED, CANCELLED
- Her asamanin kendi davranislari (hazard durumu, vault erisimi, boss durumu)

#### Dusus Animasyon Sistemi
- 3 dusus modu: instant, normal, slow
- Genisletilmis alias destegi: fast, aninda, cinematic, yavas
- Yapilandirilabilir dusus yuksekligi ve suresi
- Carpma oncesi gorsel efektler (gok isigi, partikul, ses)
- Ekran sarsintisi efekti

#### Setup Sistemi
- Oyun ici gorsel schematic olusturma
- 7 ozel setup araci (Blaze Rod, Recovery Compass, End Rod, Chest, Name Tag, Barrier, Bedrock)
- Yaratik, ganimet ve hologram noktalari ekleme
- Ana kok blogu offset sistemi
- MythicMobs agirlik editoru
- Setup komut engelleyici (/tp, /spawn, /fly vb. engellenir)
- Envanter yedekleme (armor, offhand, XP, can, yemek dahil)

#### Loot Sistemi
- Gorsel ganimet GUI editoru
- Sans, miktar araligi ve kilit ayarlari
- ItemsAdder, PDC, NBT ve 1.21 Data Components korumasi
- Container'dan hizli aktarim (Shift + sag tik)
- Kişisel ve ortak ganimet modu
- 4 erisim modu: AUTO, INTERACT, BREAK, BOTH

#### Otomatik Meteorlar
- Zamanlanmis otomatik meteor sistemi
- Oyun ici ayar GUI'si (AutoSetupGUI)
- Dunya bazli agirliklandirma sistemi
- 9 hazir konum preseti (flat_surface, water_surface, underground, air, vb.)
- TPS korumasi ile dusuk performansta erteleme
- Konum cooldown sistemi
- Maksimum aktif event limiti

#### Tehlike Sistemi (Hazards)
- Radyasyon hasari (Wither efekti + direkt hasar, yesil partikul)
- Wind Charge dalgalari (Sonic Boom partikulu, itme)
- EMP alani (Elytra devre disi, Ender Pearl engelleme)
- Mavi toz partikulleri ile gorsel EMP gostergesi
- olmeteor.bypass.hazards yetkisi

#### Bilet Sistemi
- PDC imzali guvenli meteor cagirma biletleri
- Bekleme suresi (cooldown)
- Basarisiz cagirmada otomatik iade
- Baglanti kesilince iade (yeniden giriste teslim)

#### Odul Sistemi
- 3 katmanli odul: loot tablosu, siralama odulleri, rewards-commands
- BossBar takip sistemi (mesafe, yon, kalan yaratik, sure)
- Hasar siralamasi ve liderlik tablosu
- ActionBar bildirimleri (hasar, oldurme)
- Event alaninda olunce envanter koruma

#### Rollback ve Kurtarma
- FAWE/WorldEdit ile async arazi geri yukleme
- Carpma aninda snapshot (terrain capture)
- Sunucu cokmesi durumunda crash kurtarma
- Yapilandirilabilir geri yukleme (restore-structure-on-finish)
- Bekleyen snapshot kurtarma kuyrugu

#### Kalici Veri Saklama
- Oyuncu istatistikleri (player-stats.yml): hasar, oldurme, ganimet, siralama odulu
- Meteor gecmisi (meteor-history.yml): tip, dunya, konum, sonuc
- Her kaydin zaman bilgisi ile birlikte saklanmasi

### Entegrasyonlar

- **CommandAPI** - Tum komut agaci
- **FAWE / WorldEdit** - Schematic islemleri, rollback, varsayilan schematic olusturma
- **WorldGuard** - Bolge korumasi ve gecici region olusturma
- **Towny** - Town arazilerinden kacinma
- **MythicMobs** - Ozel yaratik ve boss destegi
- **PlaceholderAPI** - 14+ placeholder
- **ItemsAdder** - Ozel esya verisi korumasi
- **FancyHolograms** - Hologram destegi (yan yana calisir)
- **NBTAPI** - Ek NBT entegrasyonu

### API

- OlMeteorAPI servisi (ServicesManager uzerinden)
- 4 API metodu: startAt, startRandom, stop, activeEvents
- 3 Bukkit event: MeteorPreStartEvent (iptal edilebilir), MeteorImpactEvent, MeteorFinishEvent
- Folia uyumlu scheduler

### Komutlar

| Komut | Yetki |
|---|---|
| /olmeteor start <tip> [dunya] [yaricap] | olmeteor.start |
| /olmeteor spawnat <tip> <konum> [dunya] [mod] | olmeteor.start |
| /olmeteor stop <eventId> | olmeteor.stop |
| /olmeteor cancel <eventId> | olmeteor.cancel |
| /olmeteor setup <tip> | olmeteor.setup |
| /olmeteor setupnew <tip> | olmeteor.setup |
| /olmeteor editschematic <tip> <isim> | olmeteor.setup |
| /olmeteor schematic <isim> | olmeteor.setup |
| /olmeteor setupfinish (sil / birak) | olmeteor.setup |
| /olmeteor useschematic <tip> <isim> | olmeteor.setup |
| /olmeteor selectmob <MythicMobId> | olmeteor.setup |
| /olmeteor settext <metin> | olmeteor.setup |
| /olmeteor loot <tip> | olmeteor.setup |
| /olmeteor wand | olmeteor.wand |
| /olmeteor list | olmeteor.list |
| /olmeteor info <eventId> [oyuncu] | olmeteor.info |
| /olmeteor stats [oyuncu] | olmeteor.info |
| /olmeteor history [1-50] | olmeteor.history |
| /olmeteor preview <tip> | olmeteor.setup |
| /olmeteor reload | olmeteor.reload |
| /olmeteor debug | olmeteor.admin |
| /olmeteor ticket <oyuncu> <tip> [1-64] | olmeteor.admin |
| /olmeteor auto | olmeteor.auto |
| /olmeteor auto ac | olmeteor.auto |
| /olmeteor auto kapat | olmeteor.auto |
| /olmeteor auto durum | olmeteor.auto |
| /olmeteor auto simdi | olmeteor.auto |
| /olmeteor auto ayarla ... | olmeteor.auto |
| /olmeteor preset <isim> [minY] [maxY] | olmeteor.auto |

### Yetkiler

| Yetki | Varsayilan |
|---|---|
| olmeteor.* | OP |
| olmeteor.admin | OP |
| olmeteor.setup | OP |
| olmeteor.start | OP |
| olmeteor.stop | OP |
| olmeteor.cancel | OP |
| olmeteor.reload | OP |
| olmeteor.list | OP |
| olmeteor.info | OP |
| olmeteor.history | OP |
| olmeteor.wand | OP |
| olmeteor.auto | OP |
| olmeteor.preset | OP |
| olmeteor.participate | Herkese acik |
| olmeteor.bypass.hazards | OP |

### Yapilandirma

- `config.yml` icerisinde 9 ana bolum
- `command-categories.yml` ile yardim menusu duzenleme
- `lang/messages_tr.yml` ile dil dosyasi (locale: tr)
- `player-stats.yml` ile kalici oyuncu istatistikleri
- `meteor-history.yml` ile meteor gecmisi

---

## Planlanan Ozellikler

- [ ] Veri tabani destegi (MySQL/SQLite)
- [ ] Oyun ici rehber (Guide GUI)
- [ ] Ozellestirilebilir meteor cesitleri (ozel sinif)
- [ ] Parti sistemi (takim halinde savas)
- [ ] Web paneli
- [ ] Discord entegrasyonu
