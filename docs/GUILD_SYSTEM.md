# FractionCore — System Gildii

Dokumentacja funkcjonalna systemu gildii w FractionCore. Opisuje zakładanie gildii, rangi, członków, relacje, rozwiązanie gildii oraz konfigurację.

---

## Spis treści

1. [Tworzenie gildii](#tworzenie-gildii)
2. [Rangi i hierarchia](#rangi-i-hierarchia)
3. [Zarządzanie członkami](#zarządzanie-członkami)
4. [Relacje międzygildyjne](#relacje-międzygildyjne)
5. [Rozwiązanie gildii i przekazanie przywództwa](#rozwiązanie-gildii-i-przekazanie-przywództwa)
6. [Czat i tagi gildii](#czat-i-tagi-gildii)
7. [Konfiguracja](#konfiguracja)
8. [Uprawnienia](#uprawnienia)
9. [Komendy](#komendy)

---

## Tworzenie gildii

### Komenda

```
/guild create <nazwa> <tag>
/guild create confirm
```

### Walidacja

| Pole | Wymaganie |
|------|-----------|
| `nazwa` | 3–24 znaki (domyślnie `foundation.min-name-length` / `max-name-length`) |
| `tag` | 2–6 znaków alfanumerycznych (`foundation.min-tag-length` / `max-tag-length`) |
| unikalność | Nazwa i tag gildii muszą być unikalne w bazie |
| koszt | Lista przedmiotów zdefiniowana w `foundation-cost.items` |
| cooldown | Gracz nie może zakładać nowej gildii przez `foundation-requirements.cooldown-minutes` minut po opuszczeniu poprzedniej |
| limit | `foundation-requirements.max-guilds-per-player` = 1 gildia na gracza |
| światy | Zakazane światy z listy `foundation-requirements.blocked-worlds` |
| odległość | Gildia musi być w odpowiedniej odległości od spawnu i od innych gildii |

### Podgląd

Po wpisaniu komendy z nazwą i tagiem gracz widzi podgląd:
- nazwę i tag gildii,
- wymagane przedmioty,
- lokalizację centrum gildii,
- potwierdzenie `/guild create confirm`.

### Efekt końcowy

Po potwierdzeniu:
- tworzony jest rekord gildii w bazie,
- zakładacz otrzymuje rangę **Lider**,
- generowany jest cuboid ochronny,
- tworzony jest dom gildii (`/guild home`),
- lider teleportowany jest do centrum gildii.

---

## Rangi i hierarchia

System oparty jest na 5 rangach o rosnącej wadze.

| Ranga | Waga | Uprawnienia |
|-------|------|-------------|
| **Lider** | 100 | Pełna kontrola: rozwiązanie gildii, przekazanie przywództwa, zarządzanie rangami, sojusze, wrogowie, banowanie |
| **Co-Lider** | 80 | Zarządzanie członkami, sojusze, wrogowie, banowanie, akceptowanie próśb o dołączenie |
| **Moderator** | 60 | Zapraszanie graczy, wyrzucanie członków niższych rang, akceptowanie/odrzucanie próśb |
| **Członek** | 40 | Standardowy członek gildii |
| **Rekrut** | 20 | Nowy członek z najniższymi uprawnieniami |

### Zasady rang

- Lidera nie można wyrzucić ani zdegradować.
- Co-Lider i Moderator mogą zarządzać tylko członkami o niższej lub równej randze względem siebie (zależnie od komendy).
- Przy przekazywaniu przywództwa stary lider staje się Co-Liderem.

---

## Zarządzanie członkami

### Zapraszanie

```
/guild invite <nick>
```

- Wymaga rangi **Moderator** lub wyższej.
- Limit oczekujących zaproszeń: `member-management.max-invites-per-guild`.
- Zaproszenie wygasa po `member-management.invite-timeout-minutes` minutach.

### Dołączanie

```
/guild join <tag>
```

- Gracz może dołączyć tylko, jeśli otrzymał zaproszenie.
- Istnieje cooldown po opuszczeniu poprzedniej gildii.
- Nowy członek otrzymuje rangę **Rekrut**.

### Wyrzucanie

```
/guild kick <nick>
```

- Wymaga rangi **Moderator** lub wyższej.
- Nie można wyrzucić lidera.
- Można wyrzucić tylko gracza o niższej lub równej randze (zależnie od logiki komendy).

### Opuszczanie gildii

```
/guild leave
```

- Każdy członek może opuścić gildię.
- Lider musi najpierw przekazać przywództwo.

### Banowanie

```
/guild ban <nick> [powod]
/guild unban <nick>
/guild banlist
```

- Wymaga rangi **Co-Lider** lub wyższej.
- Zbanowany gracz nie może dołączyć do gildii.

### Awansowanie i degradowanie

```
/guild promote <nick> [ranga]
/guild demote <nick> [ranga]
```

- Wymaga rangi **Co-Lider** lub wyższej.
- Bez podania rangi awans/degradacja o jeden poziom.
- Można podać docelową nazwę rangi (`lider`, `co-lider`, `moderator`, `czlonek`, `rekrut`).

---

## Relacje międzygildyjne

### Typy relacji

| Relacja | Opis |
|---------|------|
| **Sojusz** | Gildie sojusznicze, zielony/cyan kolor tagu |
| **Wrogość** | Gildie wrogie, czerwony kolor tagu |
| **Rozejm** | Tymczasowy pokój, żółty kolor tagu |
| **Neutral** | Brak relacji, szary kolor tagu |

### Komendy

```
/guild ally <tag>           # wyślij prośbę o sojusz
/guild allyaccept <tag>     # zaakceptuj prośbę o sojusz
/guild allydecline <tag>    # odrzuć prośbę o sojusz
/guild enemy <tag>          # ustal wrogosć
/guild neutral <tag>        # ustaw relację neutralną
/guild relations            # lista sojuszy i wrogów
```

### Uprawnienia

- Zarządzanie relacjami wymaga rangi **Co-Lider** lub wyższej.
- Relacje są symetryczne — zapisane w bazie jako jeden rekord dla pary gildii.

### Kolory tagów

Kolory relacji konfigurowane są w `modules/guild.yml` w sekcji `relation-colors`:

```yaml
relation-colors:
  guildless: "<gray>"      # gracz bez gildii
  member: "<green>"        # członek tej samej gildii
  ally: "<dark_aqua>"      # sojusznik
  enemy: "<red>"           # wróg
  truce: "<yellow>"        # rozejm
  neutral: "<gray>"        # neutralna gildia
```

---

## Rozwiązanie gildii i przekazanie przywództwa

### Rozwiązanie gildii

```
/guild disband
/guild disband confirm
```

- Tylko **Lider** może rozwiązać gildię.
- Po wpisaniu `/guild disband` włącza się odliczanie w Action Barze.
- Potwierdzenie musi nastąpić w ciągu `disband.timeout-seconds` sekund (domyślnie 60).
- Jeśli czas minie, komenda traci ważność.
- Po rozwiązaniu:
  - gildia jest archiwowana w historii,
  - członkowie tracą członkostwo,
  - wysyłany jest ogólny komunikat serwera,
  - tagi nad głową są usuwane.

### Zwrot kosztów

Opcjonalny zwrot części kosztu zakładania gildii:

```yaml
disband:
  refund:
    enabled: false
    percentage: 0.5
```

### Przekazanie przywództwa

```
/guild leader <nick>
```

- Tylko **Lider** może przekazać przywództwo.
- Stary lider staje się **Co-Liderem**.
- Nowy lider musi być członkiem gildii.

---

## Czat i tagi gildii

### Czat gildyjny

Gdy gracz ma gildię, jego wiadomości na czacie są prefiksowane tagiem gildii i literą rangi.

Przykład:
```
[MBA][C] Maniek: wiadomość
```

Konfiguracja w `modules/guild.yml`:

```yaml
chat:
  enabled: true
  format: "<dark_gray>[{tag}]{rank_letter} <white>{player}<gray>: "
  rank-letters:
    leader: "L"
    coleader: "Z"
    moderator: "M"
    member: "C"
    recruit: "R"
  rank-letter-colors:
    leader: "<gold>"
    coleader: "<aqua>"
    moderator: "<green>"
    member: "<gray>"
    recruit: "<dark_gray>"
```

### Tagi nad głową i w TAB-ie

Każdy gracz z gildią ma tag `[TAG][RANGA]` nad głową i w TAB-ie. Kolor tagu zależy od relacji widzącego gracza z gildią docelową.

---

## Konfiguracja

Główny plik konfiguracyjny systemu gildii: `plugins/FractionCore/modules/guild.yml`.

### Kluczowe sekcje

#### Podstawy zakładania gildii

```yaml
foundation:
  min-name-length: 3
  max-name-length: 32
  min-tag-length: 2
  max-tag-length: 6
  announce-globally: false
```

#### Koszt zakładania

```yaml
foundation-cost:
  enabled: true
  items:
    - material: DIAMOND_BLOCK
      amount: 16
```

#### Rangi

```yaml
ranks:
  leader:
    name: "Lider"
    weight: 100
  coleader:
    name: "Co-Lider"
    weight: 80
  moderator:
    name: "Moderator"
    weight: 60
  member:
    name: "Czlonek"
    weight: 40
  recruit:
    name: "Rekrut"
    weight: 20
```

#### Czat gildyjny

```yaml
chat:
  enabled: true
  format: "<dark_gray>[{tag}]{rank_letter} <white>{player}<gray>: "
  rank-letters: { ... }
  rank-letter-colors: { ... }
```

#### Rozwiązanie gildii

```yaml
disband:
  timeout-seconds: 60
  refund:
    enabled: false
    percentage: 0.5
```

#### Ustawienia gildii

```yaml
settings:
  home:
    teleport-delay-seconds: 5
    cooldown-seconds: 300
  description:
    max-length: 256
  flags:
    is-public: true
    allow-join-requests: false
    show-home: false
```

---

## Uprawnienia

Główne uprawnienie do korzystania z komendy `/guild`:

```
fractioncore.command.guild
```

Uprawnienia poszczególnych podkomend (domyślnie `true` dla wszystkich):

```
guild.user.invite
 guild.user.join
 guild.user.leave
 guild.user.kick
 guild.user.promote
 guild.user.demote
 guild.user.leader
 guild.user.ban
 guild.user.unban
 guild.user.sethome
 guild.user.home
 guild.user.description
 guild.user.flag
 guild.user.requests
 guild.user.joinaccept
 guild.user.joindecline
 guild.user.disband
 guild.user.ally
 guild.user.allyaccept
 guild.user.allydecline
 guild.user.enemy
 guild.user.neutral
 guild.user.relations
```

Uprawnienia administratora:

```
fractioncore.admin
fractioncore.admin.module
fractioncore.admin.lang.reload
fractioncore.admin.reload
fractioncore.admin.debug
```

---

## Komendy

| Komenda | Opis | Wymagana ranga |
|---------|------|----------------|
| `/guild create <nazwa> <tag>` | Rozpocznij zakładanie gildii | brak gildii |
| `/guild create confirm` | Potwierdź założenie gildii | brak gildii |
| `/guild invite <nick>` | Zaproś gracza do gildii | Moderator+ |
| `/guild invite cancel` | Anuluj wszystkie zaproszenia | Moderator+ |
| `/guild invite decline <tag>` | Odrzuć zaproszenie od gildii | — |
| `/guild join <tag>` | Dołącz do gildii (po zaproszeniu) | — |
| `/guild leave` | Opuść gildię | członek gildii |
| `/guild kick <nick>` | Wyrzuć członka gildii | Moderator+ |
| `/guild promote <nick> [ranga]` | Awansuj członka | Co-Lider+ |
| `/guild demote <nick> [ranga]` | Degraduj członka | Co-Lider+ |
| `/guild leader <nick>` | Przekaż przywództwo | Lider |
| `/guild ban <nick> [powod]` | Zbanuj gracza w gildii | Co-Lider+ |
| `/guild unban <nick>` | Odbanuj gracza | Co-Lider+ |
| `/guild banlist` | Lista zbanowanych | Co-Lider+ |
| `/guild info [tag]` | Informacje o gildii | — |
| `/guild sethome` | Ustaw dom gildii | Co-Lider+ |
| `/guild home` | Teleportuj do domu gildii | członek gildii |
| `/guild description <tekst>` | Ustaw opis gildii | Co-Lider+ |
| `/guild flag <flaga> <true/false>` | Zmień flagę gildii | Co-Lider+ |
| `/guild requests` | Lista próśb o dołączenie | Co-Lider+ |
| `/guild joinaccept <nick>` | Przyjmij prośbę o dołączenie | Co-Lider+ |
| `/guild joindecline <nick>` | Odrzuć prośbę o dołączenie | Co-Lider+ |
| `/guild disband` | Rozpocznij rozwiązywanie gildii | Lider |
| `/guild disband confirm` | Potwierdź rozwiązanie gildii | Lider |
| `/guild ally <tag>` | Wyślij prośbę o sojusz | Co-Lider+ |
| `/guild allyaccept <tag>` | Zaakceptuj sojusz | Co-Lider+ |
| `/guild allydecline <tag>` | Odrzuć prośbę o sojusz | Co-Lider+ |
| `/guild enemy <tag>` | Ustal wrogosć | Co-Lider+ |
| `/guild neutral <tag>` | Ustal relację neutralną | Co-Lider+ |
| `/guild relations` | Pokaż relacje gildii | członek gildii |
| `/guild help` | Pomoc | — |
| `/guild admin ...` | Komendy administratora | `fractioncore.admin` |

---

## Dodatkowe informacje

- System gildii jest modułem — można go wyłączyć w `config.yml` ustawiając `modules.guild.enabled: false`.
- Konfigurację i pliki językowe można przeładować bez restartu serwera komendami `/guild admin reload` oraz `/guild admin lang reload`.
- Kolory we wszystkich wiadomościach obsługują format MiniMessage (`<color>`, `<bold>`, itp.) oraz legacy kody (`&a`, `§a`).
