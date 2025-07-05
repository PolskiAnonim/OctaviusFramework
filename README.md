# Octavius Framework

Aplikacja desktopowa w Kotlin Compose Multiplatform do śledzenia azjatyckich mediów (powieści, manga, manhwa) i gier.

## Funkcje

- **Zarządzanie publikacjami azjatyckimi**: Śledzenie powieści, mangi, manhw
- **Zarządzanie grami**: Organizacja kolekcji gier z seriami i metadanymi
- **Zaawansowane formularze**: Własny system formularzy z walidacją i automatyczną obsługą bazy danych
- **Raporty**: Tabele z filtrowaniem, sortowaniem i konfigurowalnymi kolumnami
- **Interfejs po polsku**: Pełna lokalizacja z obsługą form liczby mnogiej

## Technologie

- **Kotlin Multiplatform** + **Compose Multiplatform**
- **PostgreSQL** z trzema schematami (public, asian_media, games)
- **Spring JDBC** + **HikariCP**
- **Material 3**

## Uruchamianie

### Wymagania
- JDK 24+
- PostgreSQL 17+
- Baza danych `novels_games` ze schematem z `baza.sql`

### Konfiguracja
Utwórz plik `composeApp/.env`:
```env
DB_URL=jdbc:postgresql://localhost:5430/novels_games
DB_USERNAME=postgres
DB_PASSWORD=1234
DB_MAX_POOL_SIZE=10
BASE_DOMAIN_PACKAGE=org.octavius.domain
DB_SCHEMAS=public,asian_media,games
LANGUAGE=pl
```

### Budowanie i uruchamianie

Na WSL:
```bash
source ~/.bashrc
./gradlew1 build
./gradlew1 run
```

Standardowo:
```bash
./gradlew build
./gradlew run
```

## Architektura

### Baza danych
- Multi-schema PostgreSQL (public, asian_media, games)
- Automatyczne mapowanie typów PostgreSQL na Kotlin
- Zaawansowana obsługa złożonych typów (arrays, enums, composite types)

### System formularzy
Własny framework formularzy z:
- Deklaratywnymi schematami
- Walidacją
- Automatyczną translacją na operacje bazodanowe
- Różnymi typami kontrolek (primitive, selection, collection, layout)

### Wzorzec domenowy
Każda encja ma:
- Model domeny (Game.kt, Publication.kt)
- FormDataManager (ładowanie danych, operacje zapisu)
- FormHandler (logika biznesowa)
- FormSchemaBuilder (definicja UI)
- FormValidator (walidacja)
- System raportów (tabele z filtrowaniem)

## Struktura projektu

```
composeApp/src/desktopMain/kotlin/org/octavius/
├── app/          # Punkt wejścia
├── config/       # Konfiguracja
├── database/     # Warstwa bazy danych
├── domain/       # Modele domeny
├── form/         # System formularzy
├── modules/      # Moduły funkcjonalne
├── report/       # System raportów
├── ui/           # Komponenty UI i motywy
└── util/         # Narzędzia
```