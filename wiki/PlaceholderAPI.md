# PlaceholderAPI

PlaceholderAPI sunucuda yukluyse OlMeteor `olmeteor` expansion'ini otomatik kaydeder. Ayri bir eCloud indirmesi gerekmez.

---

## Placeholder Tablosu

### Aktif Meteor Bilgileri

| Placeholder | Sonuc | Aciklama |
|---|---|---|
| %olmeteor_active% | Sayi | Aktif meteor sayisi |
| %olmeteor_eventid% | Metin | Oyuncuya en yakin aktif meteorun ID'si |
| %olmeteor_phase% | Metin | En yakin meteorun asamasi (Impact, Active vb.) |
| %olmeteor_distance% | Sayi | En yakin meteora blok cinsinden uzaklik |
| %olmeteor_type% | Metin | En yakin meteorun yerlestirilmis tipi |
| %olmeteor_active_type% | Metin | %olmeteor_type% ile ayni aktif tip bilgisi |
| %olmeteor_boss_alive% | true/false | Boss hayatta mi? |

### Zamanlayici

| Placeholder | Sonuc | Aciklama |
|---|---|---|
| %olmeteor_next_time% | SS:DD:ss | Siradaki otomatik meteora kalan sure |

### Oyuncu Istatistikleri

| Placeholder | Sonuc | Aciklama |
|---|---|---|
| %olmeteor_player_damage% | Sayi | Oyuncunun kalici toplam meteor hasari |
| %olmeteor_player_kills% | Sayi | Oyuncunun kalici meteor oldurme sayisi |
| %olmeteor_player_loot% | Sayi | Oyuncunun aldigi meteor ganimeti sayisi |
| %olmeteor_player_rank% | #sira | En yakin aktif meteor icindeki hasar sirasi |

---

## Kullanim Ornekleri

### Scoreboard Ornegi:
```
Meteor: %olmeteor_active_type%
Uzaklik: %olmeteor_distance% blok
Hasarin: %olmeteor_player_damage%
Siradaki: %olmeteor_next_time%
```

### Hologram Ornegi (DecentHolograms):
```
%olmeteor_active_type%
Uzaklik: %olmeteor_distance% blok
Durum: %olmeteor_phase%
```

---

## Onemli Notlar

| Durum | Placeholder Degeri |
|---|---|
| Oyuncunun dunyasinda aktif meteor yok | none, 0, false |
| Istatistik kaydi yok | 0 |
| Boss yok veya oldu | false |
| Otomatik sistem kapali | --- |

---

## Sorun Giderme

Placeholderlar calismiyorsa:

1. `/olmeteor debug` ile PlaceholderAPI hook'unun acik oldugunu kontrol edin
2. PlaceholderAPI'nin dogru surumunu kullandiginizi dogrulayin
3. Tam sunucu restart i yapin (/reload kullanmayin)
4. Expansion'in kayitli oldugunu kontrol edin: /papi list
