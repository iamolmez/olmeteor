# OlEncounters

OlEncounters, Paper ve Folia sunucuları için geliştirilmiş kapsamlı ve tamamen özelleştirilebilir bir karşılaşma ve dünya etkinliği eklentisidir.

Eklenti yalnızca gökten meteor düşürmekle sınırlı değildir. Sunucu yöneticileri OlEncounters ile gökten inen, yerden yükselen, portaldan çıkan, uzaktan uçarak gelen veya yavaşça ortaya çıkan özel yapılar ve etkinlikler hazırlayabilir. Her karşılaşma kendi şematiğine, geliş animasyonuna, yaratıklarına, ganimetlerine, mesajlarına, efektlerine ve dünya kurallarına sahip olabilir.

OlEncounters; sinematik etkinlikler, boss savaşları, kaynak alanları, zindan girişleri, istila etkinlikleri, hazine bölgeleri ve özel sunucu karşılaşmaları oluşturmak için kullanılabilir.

## Temel özellikler

### Özelleştirilebilir karşılaşmalar

Her karşılaşma birbirinden bağımsız olarak yapılandırılabilir. Küçük bir kaynak meteoru, büyük bir boss etkinliği veya portaldan gelen özel bir yapı aynı sistem içerisinde ayrı ayarlara sahip olabilir.

Karşılaşma başına ayarlanabilen bazı özellikler:

- Kullanılacak şematik
- Karşılaşmanın kategorisi
- Etki alanı ve alan şekli
- Geliş animasyonu
- Animasyon süresi ve mesafesi
- Parçacık ve ses efektleri
- MythicMobs yaratıkları
- Dalga sistemi
- Ganimet blokları
- Kişisel veya ortak ganimet
- Hasar sıralaması
- Sıralama ödülleri
- Anahtar ve ödül sistemi
- Sohbet duyuruları
- Yakındaki oyunculara özel mesajlar
- Temizlenme ve arazi yenileme süresi
- Otomatik etkinlik ağırlığı ve konum kuralları

### Gerçek şematik animasyonları

OlEncounters yalnızca bir meteor çekirdeğini hareket ettirmek yerine şematiğin kendisini animasyonlu olarak gösterebilir. Yapının blokları geliş sırasında görüntü varlıklarıyla temsil edilir ve animasyon tamamlandığında gerçek yapı güvenli şekilde yerleştirilir.

Desteklenen geliş biçimleri:

- Anında oluşturma
- Normal gökten düşme
- Yavaş ve sinematik gökten iniş
- Yerden yükselme
- Portaldan çıkma
- Yoktan var olma
- Yatay olarak uçarak gelme

Şematik animasyonu için blok limiti, toplu işleme miktarı, katman gecikmesi, parçacık yoğunluğu, sesler ve performans ayarları değiştirilebilir. Büyük şematiklerde sunucu performansını korumak için güvenli sınırlar ve otomatik geri dönüş sistemi bulunur.

### Oyun içi kurulum ve düzenleme

Karşılaşmalar yalnızca yapılandırma dosyalarından hazırlanmak zorunda değildir. Oyun içi kurulum sistemiyle yeni bir karşılaşma oluşturabilir veya kayıtlı bir şematiği düzenleyebilirsiniz.

Kurulum sırasında:

- Ana kök blok seçilebilir
- Başlangıç ve bitiş noktaları belirlenebilir
- Yaratık doğma noktaları eklenebilir
- Aynı noktaya tekrar tıklanarak kayıt silinebilir
- Birden fazla ganimet noktası oluşturulabilir
- Ganimet bloğu olarak sandık veya istenilen başka bir blok kullanılabilir
- Hologram ve yazı konumları ayarlanabilir
- Şematik kaydedilebilir
- Değişiklikler kaydedilmeden çıkılabilir
- Kurulum alanının işlem sonunda silinip silinmeyeceği seçilebilir

Kurulum envanteri güvenli şekilde yedeklenir ve çıkış yapıldığında kurulum eşyaları oyuncunun envanterinden temizlenir.

### MythicMobs ve dalga sistemi

Her karşılaşmada hangi MythicMobs yaratıklarının doğacağı, doğma şansları ve doğacakları noktalar ayrı ayrı belirlenebilir.

Yaratık sistemi şunları destekler:

- Birden fazla yaratık türü
- Yaratık başına doğma ağırlığı
- Sabit veya rastgele doğma noktaları
- Dalga tabanlı savaş sistemi
- Boss karşılaşmaları
- Çok sayfalı MythicMobs seçim menüsü
- Yaratıkların uzaktaki oyuncular nedeniyle kendiliğinden kaybolmasını engelleme
- Gökten inme, yerden çıkma, portal ve yoktan var olma gibi yaratık geliş efektleri
- Bütün yaratıklar öldüğünde otomatik aşama geçişi

### Ganimet ve ödül sistemi

OlEncounters kişisel ve ortak ganimet sistemlerini birlikte destekler.

Ganimet noktası sandık olmak zorunda değildir. Yönetici tarafından belirlenen herhangi bir blok kırıldığında veya etkileşime girildiğinde ganimet açılabilir. Aynı karşılaşmada birden fazla ganimet bloğu kullanılabilir.

Desteklenen ganimet özellikleri:

- Oyun içi sürükle-bırak ganimet düzenleyicisi
- Eşya başına çıkma şansı ve ağırlık
- Minimum ve maksimum eşya miktarı
- Kişisel ganimet
- Ortak ganimet
- Yeniden açılabilen ganimet noktaları
- Sandık tamamen boşaltılana kadar erişim
- Kilitli ödül kasaları
- Hasar sıralamasına göre ödül
- Konsol komutuyla ödül verme
- Anahtar gerektiren veya anahtarsız ödül sistemi
- Ödül sistemini tamamen kapatma
- Anahtar eşyasını değiştirme
- ItemsAdder eşya desteği
- NBT, PDC ve modern Data Components verilerini koruma

Kişisel ganimet kullanıldığında her oyuncunun hakkı ayrı takip edilir. Bir oyuncunun aldığı eşyalar başka bir oyuncunun ganimetini etkilemez.

### Hasar sıralaması ve oyuncu geri bildirimi

Karşılaşma sırasında oyuncuların verdiği hasar takip edilebilir. Sistem, isteğe bağlı action bar mesajlarıyla oyuncuya anlık hasar bilgisini gösterebilir.

Etkinlik sonunda:

- En çok hasar veren oyuncular sıralanabilir
- Sıralama tamamen kapatılabilir
- Action bar bildirimi ayrı olarak kapatılabilir
- Derece başına farklı ödüller verilebilir
- Ödüller eşya veya konsol komutu olabilir
- Sıralama mesajları yalnızca ilgili oyunculara veya belirlenen kitleye gönderilebilir

Bu ayarlar her karşılaşma için ayrı ayrı değiştirilebilir.

### Gelişmiş otomatik etkinlik sistemi

OlEncounters belirlenen süre aralıklarında otomatik karşılaşmalar başlatabilir. Tek bir kategoriye birden fazla karşılaşma eklenebilir ve hangi karşılaşmanın seçileceği ağırlık sistemiyle belirlenebilir.

Otomatik sistemde ayarlanabilen özellikler:

- Minimum ve maksimum bekleme süresi
- Kullanılacak dünya
- Minimum ve maksimum uzaklık
- Merkez konumu
- Daire, kare, üçgen, elmas ve altıgen arama alanları
- Minimum ve maksimum Y seviyesi
- Kara, hava veya herhangi bir yüzey seçimi
- Ağaçların üzeri dahil doğal yüzeylere izin verme
- Su ve lav yüzeylerine izin verme veya engelleme
- Yer altı konumlarına izin verme
- Havada doğacak etkinlikler için yükseklik aralığı
- Engebeli araziye izin verme
- İzin verilen veya engellenen bloklar
- WorldGuard bölgelerinden kaçınma
- Towny kasabalarından kaçınma
- Dünya sınırı kontrolü
- Chunk güvenliği
- TPS koruması
- Kimse etkinliğe gelmezse otomatik silme
- Karşılaşma ağırlıkları
- Birden fazla otomatik karşılaşma kategorisi

Konum arama işlemi sunucuyu dondurmamak için parçalar halinde yapılır. Uygun konum bulunduğunda etkinlik otomatik olarak başlatılır ve başarısız başlangıçlar güvenli şekilde kaydedilir.

### Duyuru ve mesaj sistemi

Her karşılaşmanın kendi başlangıç, çarpma, savaş, ödül ve bitiş mesajları olabilir.

Mesajlar ayrı ayrı şu hedeflere gönderilebilir:

- Sunucudaki bütün oyuncular
- Yalnızca etkinliğin yakınındaki oyuncular
- Belirlenen uzaklığın dışındaki oyuncular
- Yalnızca etkinliğe katılan oyuncular
- Yalnızca yöneticiler
- Konsol

Meteor veya karşılaşma konumu okunabilir dünya, X, Y ve Z bilgileriyle gösterilebilir. Yetkili oyuncular yönetici mesajındaki konuma tıklayarak doğrudan etkinlik alanına ışınlanabilir.

Bütün mesajlar dil dosyalarından değiştirilebilir. Türkçe ve İngilizce dil desteği bulunur. Seçilen dile göre komut önerileri ve yardım mesajları gösterilir.

### Güvenli temizlik ve arazi geri yükleme

OlEncounters etkinlik başlamadan önce etkilenecek alanı kaydeder. Etkinlik tamamlandığında yapı, yaratıklar, görüntü varlıkları, hologramlar, ganimet blokları ve oyuncuların yaptığı alan değişiklikleri temizlenebilir.

Geri yükleme sistemi:

- Etkinlik yapısını kaldırır
- Kırılan blokları geri getirir
- Sonradan eklenen blokları temizler
- Açılmış veya değiştirilmiş konteynerleri yeniler
- Etkinlik yaratıklarını kaldırır
- Görüntü varlıklarını ve hologramları temizler
- Yarım kalan etkinlikleri sunucu açılışında kurtarabilir
- FAWE ile büyük alanları daha verimli geri yükleyebilir
- Temizleme öncesinde ayarlanabilir geri sayım gösterebilir

Bütün ganimetler alındığında alanın kaç saniye sonra silineceği oyunculara bildirilebilir. Kimse etkinliğe katılmazsa farklı bir zaman aşımı süresi kullanılabilir.

### Performans ve Folia desteği

OlEncounters Paper ve Folia zamanlayıcı yapılarına uygun şekilde geliştirilmiştir. Bölgeye ve varlığa bağlı işlemler uygun scheduler üzerinden yürütülür.

Performans özellikleri:

- Folia uyumlu bölge ve varlık zamanlayıcıları
- Toplu şematik animasyonu
- Sınırlandırılmış görüntü varlığı sayısı
- Asenkron ve parçalara ayrılmış konum arama
- TPS koruması
- Görev ve görüntü varlığı temizliği
- Güvenli chunk yükleme sınırları
- Etkinlik başlatma hatalarında kontrollü iptal
- Çevrimdışı oyuncular için ertelenmiş ödül ve anahtar iadesi

### Entegrasyonlar

OlEncounters aşağıdaki eklentilerle çalışabilir:

- CommandAPI
- FastAsyncWorldEdit
- WorldEdit
- WorldGuard
- Towny
- MythicMobs
- ItemsAdder
- PlaceholderAPI
- FancyHolograms
- NBT API

CommandAPI zorunlu bağımlılıktır. Diğer entegrasyonlar güvenli bağlantı sistemiyle isteğe bağlı olarak yüklenir. İsteğe bağlı bir eklenti bulunmadığında OlEncounters tamamen çökmez; yalnızca ilgili entegrasyona bağlı özellikler devre dışı kalır.

WorldGuard, Towny, MythicMobs ve diğer harici API sınıfları ana eklenti sınıfında doğrudan tutulmaz. Bu yaklaşım, eksik bağımlılıklarda oluşabilecek `NoClassDefFoundError` riskini azaltır.

## Uyumluluk

- OlEncounters sürümü: 1.5.0
- Sunucu yazılımı: Paper ve Folia
- Minecraft: 1.21.1 ve üzeri
- Java: 21
- Zorunlu bağımlılık: CommandAPI
- Yapımcı: OlPlugins

## Kurulum

1. Sunucunuza uygun CommandAPI sürümünü yükleyin.
2. `OlEncounters-1.5.0.jar` dosyasını `plugins` klasörüne yerleştirin.
3. Şematik özellikleri kullanacaksanız WorldEdit veya FastAsyncWorldEdit yükleyin.
4. Sunucuyu tamamen yeniden başlatın.
5. Oluşturulan `plugins/OlEncounters` klasöründeki ayarları düzenleyin.
6. Ana komut için `/olencounters` kullanın.

JAR veya bağımlılık güncellemesinden sonra `/reload` kullanılması önerilmez. Sunucunun tamamen yeniden başlatılması daha güvenlidir.

## Eski OlMeteor kurulumlarından geçiş

OlEncounters, eski OlMeteor veri klasörünü ilk açılışta algılayabilir ve mevcut verileri yeni klasöre güvenli şekilde taşıyabilir.

Eski komutlar, yetkiler, PlaceholderAPI alanları ve kalıcı veri etiketleri için geriye dönük uyumluluk korunur. Buna rağmen güncel kurulumlarda `/olencounters`, `olencounters.*` ve `%olencounters_*%` biçimlerinin kullanılması önerilir.

## Komut

Ana yönetim komutu:

`/olencounters`

Kurulum, etkinlik başlatma, otomatik sistem, geçmiş, önizleme, yapılandırma ve yönetim işlemleri bu ana komut altında birleştirilmiştir.

## Destek

Bir hata bildirirken aşağıdaki bilgileri ekleyin:

- OlEncounters sürümü
- Paper veya Folia sürümü
- Java sürümü
- Yüklü bağımlılıklar
- Hatanın tam konsol çıktısı
- İlgili karşılaşmanın yapılandırması
- Hatanın tekrar oluşturulma adımları

Bu bilgiler sorunun daha hızlı incelenmesini sağlar.

# OlEncounters

OlEncounters is a comprehensive and fully customizable encounter and world-event plugin developed for Paper and Folia servers.

The plugin is not limited to dropping meteors from the sky. Server owners can use OlEncounters to create structures and events that descend from above, rise from the ground, emerge through portals, fly in horizontally, or materialize directly in the world. Every encounter can have its own schematic, arrival animation, mobs, loot, messages, visual effects, and world rules.

OlEncounters can be used to build cinematic events, boss fights, resource sites, dungeon entrances, invasions, treasure zones, and other custom server encounters.

## Main features

### Fully customizable encounters

Every encounter can be configured independently. A small resource meteor, a large boss event, and a structure emerging from a portal can all exist within the same system with completely different settings.

Per-encounter options include:

- Schematic selection
- Encounter category
- Impact radius and area shape
- Arrival animation
- Animation duration and distance
- Particles and sounds
- MythicMobs selection
- Wave configuration
- Loot blocks
- Personal or shared loot
- Damage leaderboard
- Ranking rewards
- Key and reward behavior
- Chat announcements
- Nearby-player messages
- Cleanup and terrain restoration timing
- Automatic-event weight and location rules

### Full-schematic animations

OlEncounters can animate the actual schematic instead of moving only a decorative meteor core. During arrival, schematic blocks are represented by display entities. When the animation finishes, the real structure is placed safely at the destination.

Available arrival modes include:

- Instant placement
- Normal sky fall
- Slow cinematic descent
- Underground rise
- Portal arrival
- Materialization
- Horizontal fly-in

Schematic animations support configurable block limits, batching, layer delays, particle density, sounds, and performance restrictions. Large schematics can automatically fall back to a safer presentation when they exceed the configured display limit.

### In-game setup and editing

Encounters do not have to be created entirely through configuration files. The in-game setup system can create a new encounter from scratch or edit an existing schematic.

During setup, administrators can:

- Select the main root block
- Define start and end positions
- Add mob spawn points
- Remove a point by selecting it again
- Create multiple loot locations
- Use a chest or another configured block as a loot block
- Position holograms and text displays
- Save the schematic
- Exit without saving
- Choose whether the temporary setup structure should be removed afterward

Player inventories are backed up during setup. Setup tools are removed safely when the session ends.

### MythicMobs and wave combat

Each encounter can define which MythicMobs should spawn, their selection weights, and their spawn locations.

The mob system supports:

- Multiple mob types
- Per-mob spawn weights
- Fixed or generated spawn points
- Wave-based combat
- Boss encounters
- Paginated MythicMobs selection menus
- Persistent encounter mobs that do not naturally despawn when players move away
- Sky, underground, portal, and materialization arrival effects
- Automatic phase progression after all required mobs are defeated

### Loot and reward system

OlEncounters supports both personal and shared loot.

A loot location does not have to use a chest. Administrators can configure almost any block to reveal loot when broken or interacted with. A single encounter can contain multiple loot blocks and reward locations.

Loot features include:

- Drag-and-drop in-game loot editor
- Per-item chance and weight
- Minimum and maximum item amounts
- Personal loot
- Shared loot
- Reopenable loot points
- Containers that remain accessible until emptied
- Locked reward vaults
- Damage-ranking rewards
- Console-command rewards
- Key-based or keyless access
- Completely disabled reward mode
- Custom key items
- ItemsAdder item support
- Preservation of NBT, PDC, and modern Data Components

With personal loot enabled, every player's reward state is tracked independently. One player collecting an item does not consume another player's rewards.

### Damage leaderboard and player feedback

OlEncounters can track damage dealt by participating players. Optional action-bar messages provide live combat feedback.

At the end of an encounter:

- Top damage dealers can be ranked
- The leaderboard can be disabled per encounter
- Action-bar feedback can be disabled separately
- Different rewards can be assigned to each rank
- Rewards can contain items or console commands
- Ranking messages can be limited to participants or another configured audience

### Advanced automatic-event system

OlEncounters can start encounters automatically within configurable time intervals. Multiple encounters can belong to the same category, and weighted selection determines which one is chosen.

Automatic-event settings include:

- Minimum and maximum delay
- Target world
- Minimum and maximum radius
- Custom center location
- Circle, square, triangle, diamond, and hexagon search areas
- Minimum and maximum Y levels
- Land, air, or any-surface placement
- Natural surfaces, including tree tops
- Optional water and lava surfaces
- Optional underground placement
- Height ranges for air encounters
- Rough-terrain support
- Allowed and blocked materials
- WorldGuard region avoidance
- Towny town avoidance
- World-border validation
- Chunk safety
- TPS protection
- Automatic cleanup when nobody participates
- Per-encounter selection weights
- Multiple automatic-event categories

Location searches are processed in controlled batches to avoid freezing the server. When a suitable location is found, the encounter starts automatically. Failed startups are reported and cleaned up safely.

### Announcements and messages

Every encounter can define its own start, arrival, combat, reward, and completion messages.

Each message can be configured for a specific audience:

- All online players
- Nearby players only
- Players outside the nearby radius
- Encounter participants
- Administrators only
- Console

Encounter locations can be displayed with readable world, X, Y, and Z information. Authorized administrators can click location messages to teleport directly to the event area.

Messages are configurable through locale files. Turkish and English are supported, and command suggestions can follow the selected server language.

### Safe cleanup and terrain restoration

OlEncounters records the affected area before an encounter changes the world. When the encounter finishes, it can remove the structure, mobs, displays, holograms, loot blocks, and player-made changes before restoring the original terrain.

The restoration system can:

- Remove the encounter structure
- Restore broken blocks
- Remove blocks placed afterward
- Restore modified containers
- Remove encounter mobs
- Clean display entities and holograms
- Recover incomplete events after a crash or unexpected shutdown
- Use FAWE for more efficient large-area restoration
- Display a configurable cleanup countdown

After all loot has been collected, players can be informed that the area will disappear after a configured delay. A separate inactivity timeout can remove encounters that nobody visits.

### Performance and Folia compatibility

OlEncounters is designed around Paper and Folia scheduling requirements. Region-sensitive and entity-sensitive actions are dispatched through the appropriate scheduler.

Performance features include:

- Folia-compatible region and entity scheduling
- Batched schematic animations
- Configurable display-entity limits
- Asynchronous and batched location searches
- TPS protection
- Task and display cleanup
- Safe chunk-loading limits
- Controlled cancellation after startup failures
- Deferred rewards and key refunds for offline players

### Integrations

OlEncounters can integrate with:

- CommandAPI
- FastAsyncWorldEdit
- WorldEdit
- WorldGuard
- Towny
- MythicMobs
- ItemsAdder
- PlaceholderAPI
- FancyHolograms
- NBT API

CommandAPI is required. Other integrations are loaded through safe optional hooks. If an optional dependency is not installed, the entire plugin does not fail; only the features that require that integration are disabled.

External API types such as WorldGuard, Towny, and MythicMobs are isolated behind dedicated integration wrappers. This architecture reduces the risk of `NoClassDefFoundError` failures when an optional dependency is unavailable.

## Compatibility

- OlEncounters version: 1.5.0
- Server software: Paper and Folia
- Minecraft: 1.21.1 and newer
- Java: 21
- Required dependency: CommandAPI
- Author: OlPlugins

## Installation

1. Install a compatible CommandAPI version.
2. Place `OlEncounters-1.5.0.jar` in the server's `plugins` folder.
3. Install WorldEdit or FastAsyncWorldEdit if you want to use schematic features.
4. Fully restart the server.
5. Configure the generated files inside `plugins/OlEncounters`.
6. Use `/olencounters` to access the main command.

Using `/reload` after updating the plugin or one of its dependencies is not recommended. A complete server restart is safer.

## Migrating from OlMeteor

OlEncounters can detect a legacy OlMeteor data directory and safely migrate existing data during its first startup.

Backward compatibility is retained for legacy commands, permissions, PlaceholderAPI identifiers, and persistent data tags. New installations should use `/olencounters`, `olencounters.*`, and `%olencounters_*%`.

## Main command

The main administration command is:

`/olencounters`

Setup, event startup, automatic scheduling, history, previews, configuration, and other administrative tools are organized under this command.

## Support

When reporting a problem, include:

- OlEncounters version
- Paper or Folia version
- Java version
- Installed dependencies
- Complete console error
- Relevant encounter configuration
- Steps required to reproduce the issue

Providing this information makes troubleshooting significantly faster.
