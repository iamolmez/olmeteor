# Kurulum

## Gereksinimler

| Bilesen | Durum | Aciklama |
|---|---|---:|
| Paper veya Folia 1.21.1+ | Zorunlu | Spigot/CraftBukkit destek hedefi degildir. |
| Java 21 | Zorunlu | Sunucunun calistigi Java surumudur. |
| CommandAPI | Zorunlu | Komutlarin kaydi icin gereklidir. |
| FAWE veya WorldEdit | Onerilen | Schematic kaydetme/yapistirma ve arazi snapshot islemleri. |
| WorldGuard | Istege bagli | Korunan bolgelere otomatik meteor dusmesini engeller. |
| Towny | Istege bagli | Town arazilerine otomatik meteor dusmesini engeller. |
| MythicMobs | Istege bagli | Ozel yaratik ve boss dogurma. |
| PlaceholderAPI | Istege bagli | Scoreboard ve hologram placeholderlari. |
| ItemsAdder | Istege bagli | Ganimet GUI'sine eklenen ozel esyalarin verileri korunur. |
| FancyHolograms | Istege bagli | Yukluyse entegrasyon olarak algilanir. |
| NBTAPI | Istege bagli | Ek NBT entegrasyonu; temel NBT/PDC korumasi onsuz da calisir. |

---

## Kurulum Adimlari

### 1. Hazirlik
```
1. Sunucuyu TAMAMEN kapatin.
2. Gerekli JAR dosyalarini hazirlayin.
3. plugins/ klasorunu kontrol edin.
```

### 2. Dosyalari Yerlestirme
```
plugins/
  OlMeteor-1.3.0.jar       -- Ana eklenti
  CommandAPI-*.jar           -- Zorunlu bagimlilik
  FastAsyncWorldEdit-*.jar   -- Onerilen (schematic islemleri)
  WorldGuard-*.jar           -- Istege bagli
  Towny-*.jar               -- Istege bagli
  MythicMobs-*.jar          -- Istege bagli
  ...diger istege bagli eklentiler
```

### 3. Ilk Baslatma
```
1. Sunucuyu baslatin.
2. plugins/OlMeteor/ klasorunun olusmasini bekleyin.
3. Konsolda /olmeteor debug calistirarak entegrasyon durumlarini kontrol edin.
4. Dil icin plugins/OlMeteor/config.yml icinde locale: tr kullanin.
```

### 4. Dogrulama
```
/olmeteor debug      -- Hook ve sistem durumu kontrolu
/olmeteor help       -- Yardim menusu
/olmeteor setup small -- Ilk meteor setup
```

---

## Guncelleme

```
1. Sunucuyu kapatin.
2. Eski OlMeteor JAR'ini yenisiyle degistirin.
3. config.yml, lang/, schematics/ ve veri dosyalarinizin yedegini saklayin.
4. Sunucuyu yeniden baslatin.
```

Uyari: `/reload`, PlugMan ve benzeri canli yeniden yukleme araclari kullanmayin. Ozellikle CommandAPI, WorldGuard, FAWE ve Folia zamanlayicilari canli reload sirasinda guvenli degildir.

---

## Sik Yapilan Hatalar

| Hata | Cozum |
|---|---|
| Plugin acilmiyor | Java 21 kontrolu, CommandAPI varligi, Paper/Folia 1.21.1+ |
| NoClassDefFoundError | Eski JAR, bozuk WorldGuard veya reload sonrasi |
| Schematic calismiyor | FAWE/WorldEdit hook kontrolu, setup'da kose secimi |
| Auto meteor baslamiyor | /olmeteor auto ac ile aktiflestirme, TPS kontrolu |

---

## Ilk Kontrol

```
/olmeteor debug
/olmeteor help
/olmeteor setup small
```

CommandAPI bulunamazsa OlMeteor kendisini guvenli sekilde kapatir. Istege bagli bir entegrasyon eksikse yalnizca o entegrasyona ait ozellik devre disi kalir.
