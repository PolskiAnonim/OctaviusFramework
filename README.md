# Octavius Framework

Aplikacja desktopowa w Kotlin Compose Multiplatform do zarządzania i śledzenia azjatyckich mediów (powieści, manga, manhwa) oraz gier. Aplikacja wykorzystuje własne frameworki formularzy i raportów.

## ✨ Kluczowe funkcje

### 📚 Zarządzanie kolekcjami
- **Publikacje azjatyckie**: Kompleksowe śledzenie powieści, mangi, manhw z postępem czytania
- **Gry**: Organizacja kolekcji gier z seriami, platformami i metadanymi

### 🚀 Zaawansowane frameworki
- **System formularzy**: Własny silnik z prostymi kontrolkami, kontrolkami wyboru, sekcjami i kontrolką sekcji powtarzalnej
- **System raportów**: Dynamiczne tabele z filtrowaniem, sortowaniem, zarządzaniem kolumnami
- **Nawigacja**: Centralny router z zarządzaniem stosami dla każdej zakładki
- **Walidacja**: Wielopoziomowa walidacja pól i reguł biznesowych

### 🎨 Nowoczesny interfejs
- **Material 3**: Pełne wsparcie Material Design 3
- **Drag & Drop**: Zaawansowane przesuwanie elementów
- **Lokalizacja**: Pełna lokalizacja z obsługą form liczby mnogiej

## 🛠️ Stack technologiczny

### Główne Technologie
- **Kotlin Multiplatform**
- **Compose Multiplatform**
- **PostgreSQL**
- **Spring JDBC**
- **HikariCP**

## 🚀 Uruchamianie

### Wymagania systemowe
- **JDK 24+**
- **PostgreSQL 17+**
- **Baza danych** `novels_games` ze schematem z `baza.sql`

### Budowanie i uruchamianie

**WSL/Windows:**
```bash
source ~/.bashrc
./gradlew1 build
./gradlew1 run
```

**Standardowe środowiska:**
```bash
./gradlew build
./gradlew run
```

### Walidacja tłumaczeń
```bash
./gradlew validateTranslations
```

## 🏗️ Architektura

### Struktura modułowa

```
Octavius/
├──  desktop-app/           # Główna aplikacja i punkt wejścia
├──  core/                  # Fundamenty: database, domain, config
├──  form-engine/           # Framework formularzy
├──  report-engine/         # Framework raportów  
├──  navigation/            # System nawigacji
├──  ui-kit/                # Współdzielone komponenty UI
├──  feature-asian-media/   # Moduł publikacji azjatyckich
├──  feature-games/         # Moduł gier
└──  feature-settings/      # Moduł ustawień
```

### 🔧 System formularzy (form-engine)

Zaawansowany framework zorientowany na dane:

**Typy kontrolek:**
- **Primitive**: String, Integer, Double, Boolean
- **Selection/Dropdown**: Enum, Database
- **Collection**: StringList (dynamiczne tablice)
- **Container**: Section (grupowanie)
- **Repeatable**: Dynamiczne zarządzanie wierszami

**Zaawansowane funkcje:**
- **Dependencies**: Kontrolki mogą się pokazywać/ukrywać na podstawie innych wartości
- **Actions**: Automatyczne akcje przy zmianie wartości
- **Validation**: Wielopoziomowa walidacja z regułami biznesowymi

### 📊 System raportów (report-engine)

Dynamiczne tabele z pełną konfiguracją:

**Funkcje:**
- **Zarządzanie kolumnami**: Drag & drop, pokazywanie/ukrywanie kolumn, zmiana kolejności
- **Filtry**: Specyficzne dla typu (string, number, enum, boolean)
- **Sortowanie**: Wielokolumnowe z zachowaniem stanu
- **Paginacja**: Efektywne ładowanie danych
- **Konfiguracja**: Zapisywanie/ładowanie układów tabel

### 🧭 System nawigacji

Centralny router

```kotlin
AppRouter (Singleton) -> AppNavigationState -> Tab Stacks -> Screens
```

**Kluczowe cechy:**
- **Oddzielny stos na każdą zakładkę**: Każda zakładka ma niezależną historię nawigacji
- **Globalny dostęp**: Funkcje nawigacji dostępne z każdego miejsca

### 🗄️ Warstwa bazy danych

**TypeRegistry** - Automatyczne skanowanie schematów PostgreSQL:
- Mapowanie typów PostgreSQL na klasy Kotlin
- Obsługa ENUM, COMPOSITE i ARRAY
- Wsparcie dla wielu schematów (public, asian_media, games)

**Komponenty:**
- **DatabaseManager**: Singleton z HikariCP pool
- **DatabaseFetcher**: Zaawansowane operacje SELECT
- **DatabaseUpdater**: Transakcyjne operacje UPDATE/INSERT/DELETE
- **RowMappers**: Automatyczna konwersja ResultSet na obiekty Kotlin

## 📁 Wzorzec domenowy

Każda encja biznesowa następuje konsekwentny wzorzec:

```kotlin
modules/[domain]/
├── form/
│   ├── [Entity]FormDataManager.kt    # Operacje bazodanowe
│   ├── [Entity]FormSchemaBuilder.kt  # Definicja struktury formularza
│   └── [Entity]FormValidator.kt      # Reguły walidacji
├── ui/
│   ├── [Entity]FormScreen.kt         # UI formularza
│   ├── [Entity]ReportScreen.kt       # UI raportu
│   └── [Entity]Tab.kt                # Zakładka główna
└── [Entity]ReportStructureBuilder.kt # Definicja raportu
```

## 🌍 System lokalizacji

- **Tłumaczenia oparte o pliki JSON**: `translations_pl.json` w każdym module
- **Obsługa liczby mnogiej**
- **Walidacja**: Automatyczne sprawdzanie użycia kluczy tłumaczeń/task `validateTranslations`
