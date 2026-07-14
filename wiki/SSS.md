# Sik Sorulan Sorular (SSS)

---

## Genel Sorular

### OlMeteor hangi sunucu yazilimlarini destekliyor?
Paper ve Folia 1.21.1+ surumleri desteklenir. Spigot ve CraftBukkit desteklenmez.

### Hangi Java surumu gerekiyor?
Java 21 zorunludur.

### CommandAPI olmadan calisir mi?
Hayir. CommandAPI zorunlu bir bagimliliktir. OlMeteor, CommandAPI bulunamazsa guvenli sekilde kapanir.

### FAWE/WorldEdit sart mi?
Schematic olusturma ve arazi geri yukleme icin FAWE veya WorldEdit onerilir. FAWE/WorldEdit olmadan setup ve rollback ozellikleri calismaz.

---

## Kurulum Sorulari

### Eklentiyi nasil guncellerim?
1. Sunucuyu kapatin
2. Eski OlMeteor JAR'ini yenisiyle degistirin
3. config.yml, lang/, schematics/ ve veri dosyalarinizin yedegini alin
4. Sunucuyu yeniden baslatin
Kesinlikle `/reload` kullanmayin.

### `/reload` kullanabilir miyim?
Hayir. CommandAPI, WorldGuard, FAWE ve Folia zamanlayicilari canli reload sirasinda guvenli degildir. Her zaman tam restart yapin.

### Eklenti acilmiyor, ne yapmaliyim?
- Java 21 kullandiginizi dogrulayin
- Paper/Folia 1.21.1+ kullandiginizi kontrol edin
- CommandAPI'nin yuklu ve once yuklendiginden emin olun
- Eski JAR'lari temizleyin
- Sunucuyu tamamen yeniden baslatin

---

## Setup Sorulari

### Setup modundan nasil cikarim?
Setup'tan cikmak icin iki yol vardir:
- **Barrier** aracina tiklayarak kaydetmeden cikabilirsiniz
- **Bedrock** aracina tiklayarak kaydedip cikabilirsiniz
Ayrica `/olmeteor setup <tip>` komutunu tekrar yazarak cikis menusunu acabilirsiniz.

### Setup sirasinda envanterim kayboldu, nerede?
Envanteriniz `data/inventories/<UUID>.yml` dosyasina yedeklenir. Setup'tan basarili bir sekilde cikinca otomatik olarak geri yuklenir. Sunucu cokerse dosya kalir ve manuel olarak geri yuklenebilir.

### Setup sirasinda hangi komutlar engellenir?
/tp, /spawn, /home, /fly, /gamemode, /clear, /kill, /stop gibi tehlikeli komutlar engellenir. /olmeteor, /msg, /tell gibi komutlara izin verilir.

### Birden fazla mob noktasi ekleyebilir miyim?
Evet. Ayni schematic'e birden fazla mob, ganimet ve hologram noktasi eklenebilir. Her nokta ana kok bloguna gore offset olarak kaydedilir.

---

## Meteor Sorulari

### Meteor tipleri nelerdir?
Small (Kolay, 15 blok), Medium (Normal, 25 blok), Large (Zor, 40 blok), Epic (Epik, 60 blok), Legendary (Efsanevi, 80 blok).

### Dusus modlari arasindaki fark nedir?
- **instant**: Dogrudan carpma, animasyon yok
- **normal**: Normal animasyonlu dusus (8-12 saniye)
- **slow**: Uzun sinematik dusus (18-25 saniye)

### Meteor neden belirtilen konuma dusmedi?
Manuel `/olmeteor spawnat` komutu kesin konum kullanir. Otomatik meteorlar icin konum arayici devreye girer ve guvenli bir konum arar.

### Ayni anda kac meteor calisabilir?
Otomatik meteorlar icin `max-active-events` ayari ile sinirlanir (varsayilan: 1). Manuel olarak sinirsiz meteor baslatilabilir.

---

## Loot ve Odul Sorulari

### Ganimet GUI'sine nasil esya eklerim?
Envanterden GUI'ye surukleyerek ekleyebilirsiniz. Ayrica setup sirasinda Chest araciyla bir container'a Shift + sag tiklayarak icindekileri tabloya aktarabilirsiniz.

### Kisisel ve ortak ganimet arasindaki fark nedir?
- **personal: true**: Her oyuncu kendi ayri envanterini gorur
- **personal: false**: Tum oyuncular ayni sandigi paylasir

### Odulleri nasil ozellestirebilirim?
Uc farkli odul katmani vardir:
1. **Ganimet GUI'si**: `/olmeteor loot <tip>` ile duzenlenir
2. **Siralama Odulleri**: `ranking-rewards` ile config'de tanimlanir
3. **rewards-commands**: Sandik acilinca calisacak komutlar

### ItemsAdder esyalarim kaybolur mu?
Hayir. ItemsAdder verileri, PDC, NBT ve 1.21 Data Components korunur.

---

## Otomatik Meteor Sorulari

### Otomatik meteorlar neden baslamiyor?
- `/olmeteor auto ac` ile sistemi aktiflestirdiginizden emin olun
- Min/max sure degerlerini kontrol edin
- `air` presetinde minY ve maxY degerlerini girdiginizden emin olun
- TPS guard sinirini kontrol edin
- `/olmeteor auto simdi` ile tanilama yapin

### Otomatik meteorlar hangi dunyalarda calisir?
Config'de `automatic-events.worlds` listesinde belirtilen dunyalarda calisir. Liste bos ise tum yuklu dunyalar kullanilir.

### Meteorlar neden hep ayni yere dusuyor?
Konum cooldown sistemi, yakin gecmiste kullanilan alanlari tekrar secmez. Eger hala ayni yere dusuyorsa cooldown suresini veya yaricapini kontrol edin.

### WorldGuard/Towny bolgesine meteor dusmesini nasil engellerim?
`location-finder.worldguard-check-claims: true` ve `towny-require-wilderness: true` ayarlarini acin. `/olmeteor debug` ile hook'larin aktif oldugunu dogrulayin.

---

## Hata ve Sorun Giderme

### NoClassDefFoundError hatasi aliyorum
Bu hata genellikle eski JAR, bozuk WorldGuard kurulumu veya `/reload` sonrasi olusur. Cozum:
1. Sunucuyu kapatin
2. Eski JAR'lari temizleyin
3. WorldGuard ve WorldEdit surumlerini kontrol edin
4. Sunucuyu tam baslatin

### Schematic gorunmuyor veya geri yuklenmiyor
- FAWE/WorldEdit hook'unu `/olmeteor debug` ile kontrol edin
- Schematic dosya adini kontrol edin
- Setup'ta iki kose ve ana kok blogunu sectiginizden emin olun
- `restore-structure-on-finish: true` ayarini kontrol edin

### Moblar yanlis yerde doguyor
- Ana kok blogunu Recovery Compass ile yeniden secin
- End Rod ile mob noktalarini yeniden kaydedin
- MythicMobs ID'sinin dogru oldugunu kontrol edin

### Ganimet blogu acilmiyor
- Setup'ta Chest araciyla ganimet noktasi ekleyin
- `loot.block` degerinin gecerli oldugunu dogrulayin
- Moblarin/bossun oldugunu kontrol edin
- `reward-top-count` ve `boss-damage-threshold-percent` degerlerini kontrol edin

---

## Entegrasyon Sorulari

### PlaceholderAPI nasil kullanilir?
PlaceholderAPI yukluyse OlMeteor otomatik olarak `olmeteor` expansion'ini kaydeder. Ayri bir indirme gerekmez. /papi list ile kayitli oldugunu kontrol edebilirsiniz.

### MythicMobs nasil entegre edilir?
MythicMobs yukluyse `/olmeteor selectmob <id>` komutu ile yaratik secilebilir. Setup icinde End Rod editoruyle agirliklar ayarlanabilir.

### Kendi eklentimden OlMeteor API'sini nasil kullanirim?
Bukkit ServicesManager uzerinden OlMeteorAPI servisini alin. Bagimliligi `compileOnly` olarak ekleyin. Detaylar icin Gelistirici API sayfasina bakin.

---

## Performans Sorulari

### OlMeteor sunucumu yavaslatir mi?
OlMeteor asenkron FAWE islemleri ve Folia uyumlu scheduler kullanir. TPS korumasi dusuk performansta otomatik meteorlari erteler. Chunk force-load yaricapi 3 chunk ile sinirlidir.

### Cok sayida esya iceren loot tablosu performansi etkiler mi?
Hayir. Loot tablosu verimli sekilde yonetilir. Her esya bagimsiz olarak hesaplanir.

### Ayni anda cok fazla meteor calistirmak sorun olusturur mu?
Evet. `max-active-events` ayarini yukseltmek sunucu performansini etkileyebilir. Varsayilan deger olan 1'i korumaniz onerilir.

---

> Detayli bilgi icin ilgili wiki sayfasina bakin veya `/olmeteor debug` ile sistem durumunu kontrol edin.
