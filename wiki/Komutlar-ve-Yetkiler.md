# Komutlar ve Yetkiler

Ana komut `/olmeteor` seklindedir. Komut onerileri CommandAPI tarafindan oyun icinde gosterilir.

---

## Oyuncu Komutlari

| Komut | Yetki | Aciklama |
|---|---|---|
| /olmeteor help | Yok | Yapilandirilabilir yardim menusunu gosterir |
| /olmeteor list | olmeteor.list | Aktif meteorlari listeler |
| /olmeteor info <eventId> [oyuncu] | olmeteor.info | Tur, asama, dunya, konum ve uzakligi gosterir |
| /olmeteor history [1-50] | olmeteor.history | Eski meteorlarin zaman, konum ve sonuclarini gosterir |
| /olmeteor stats [oyuncu] | olmeteor.info | Kalici hasar, oldurme ve ganimet istatistiklerini gosterir |

---

## Yonetim Komutlari

### Meteor Baslatma ve Durdurma

| Komut | Yetki | Aciklama |
|---|---|---|
| /olmeteor start <tip> [dunya] [yaricap] | olmeteor.start | Guvenli rastgele konumda meteor baslatir |
| /olmeteor spawnat <tip> <x y z> [dunya] [mod] | olmeteor.start | Tam olarak verilen konumda meteor baslatir |
| /olmeteor stop <eventId> | olmeteor.stop | Etkinligi guvenli bitirme/geri yukleme akisina sokar |
| /olmeteor cancel <eventId> | olmeteor.cancel | Etkinligi yonetici tarafindan iptal eder |

Meteor tipleri: small, medium, large, epic, legendary

Dusus modlari (spawnat icin): instant, normal, slow

### Ornek Kullanimlar:
```
/olmeteor start small
/olmeteor start epic world_nether 500
/olmeteor spawnat legendary ~ ~ ~ world normal
/olmeteor stop meteor_2024_01_01_12_00
/olmeteor cancel meteor_2024_01_01_12_00
```

### Yonetim ve Ayarlar

| Komut | Yetki | Aciklama |
|---|---|---|
| /olmeteor reload | olmeteor.reload | Yapilandirmayi yeniden okur (Plugin reload yapmaz) |
| /olmeteor debug | olmeteor.admin | Hook, kurtarma ve sistem durumunu gosterir |
| /olmeteor preview <tip> | olmeteor.setup | Meteor alanini blok yerlestirmeden onizler |
| /olmeteor ticket <oyuncu> <tip> [1-64] | olmeteor.admin | PDC korumali meteor cagirma bileti verir |

### Ornek Kullanimlar:
```
/olmeteor reload
/olmeteor debug
/olmeteor preview legendary
/olmeteor ticket Ahmet legendary 5
```

---

## Setup Komutlari

| Komut | Yetki | Aciklama |
|---|---|---|
| /olmeteor setup <tip> | olmeteor.setup | Yeni/kayitli schematic secim ekranini acar |
| /olmeteor setupnew <tip> | olmeteor.setup | Dogrudan sifirdan setup baslatir |
| /olmeteor useschematic <tip> <isim> | olmeteor.setup | Kayitli schematic'i tipe atar |
| /olmeteor editschematic <tip> <isim> | olmeteor.setup | Schematic'i yanina yapistirip duzenleme modunu acar |
| /olmeteor schematic <isim> | olmeteor.setup | Secili alani schematic olarak kaydeder |
| /olmeteor setupfinish (sil / birak) | olmeteor.setup | Setup sonrasinda gecici yapiyi siler veya birakir |
| /olmeteor selectmob <MythicMobId> | olmeteor.setup | Setup icin MythicMobs yaratik secer |
| /olmeteor settext <metin> | olmeteor.setup | Meteor hologram metnini ayarlar |
| /olmeteor loot <tip> | olmeteor.setup | Ganimet GUI'sini acar |
| /olmeteor wand | olmeteor.wand | Setup secim cubugunu verir |

### Ornek Kullanimlar:
```
/olmeteor setup large
/olmeteor editschematic epic epic_crater_v2
/olmeteor schematic my_meteor_v1
/olmeteor selectmob SkeletalKing
/olmeteor settext "Goktasi Alani"
/olmeteor loot medium
```

---

## Otomatik Meteor Komutlari

| Komut | Aciklama |
|---|---|
| /olmeteor auto | Oyuncuda GUI'yi, konsolda durum ekranini acar |
| /olmeteor auto ac | Otomatik meteorlari acar ve yeni zaman planlar |
| /olmeteor auto kapat | Otomatik meteorlari kapatir |
| /olmeteor auto durum | Plani ve siradaki zamani gosterir |
| /olmeteor auto simdi | Auto kurallariyla hemen konum aratir |
| /olmeteor auto ayarla ... | Butun temel auto ayarlarini tek komutta kaydeder |
| /olmeteor preset <isim> [minY] [maxY] | Aktif konum presetini degistirir |

Auto komutlarinin yetkisi `olmeteor.auto` seklindedir.

### Ornek Kullanimlar:
```
/olmeteor auto
/olmeteor auto ac
/olmeteor auto kapat
/olmeteor auto durum
/olmeteor auto simdi
/olmeteor preset desert_surface
/olmeteor preset air 120 220
```

---

## Yetki Tablosu

| Yetki | Varsayilan | Aciklama |
|---|---|---|
| olmeteor.* | OP | Butun OlMeteor yetkileri (wildcard) |
| olmeteor.admin | OP | Yonetim komutlarinin tamami |
| olmeteor.setup | OP | Setup modu ve schematic islemleri |
| olmeteor.start | OP | Meteor baslatma (start, spawnat) |
| olmeteor.stop | OP | Meteor durdurma |
| olmeteor.cancel | OP | Meteor iptal etme |
| olmeteor.reload | OP | Config yeniden yukleme |
| olmeteor.list | OP | Aktif meteor listeleme |
| olmeteor.info | OP | Meteor detay ve istatistik goruntuleme |
| olmeteor.history | OP | Meteor gecmisi |
| olmeteor.wand | OP | Setup cubugu alma |
| olmeteor.auto | OP | Otomatik meteor yonetimi |
| olmeteor.preset | OP | Konum preseti degistirme |
| olmeteor.participate | Herkese acik | Etkinlige katilabilme |
| olmeteor.bypass.hazards | OP | Radyasyon, EMP ve tehlikeleri atlama |

### Ornek Yetki Ayari (LuckPerms):
```
/lp group admin permission set olmeteor.admin true
/lp group member permission set olmeteor.participate true
/lp group member permission set olmeteor.list true
```
