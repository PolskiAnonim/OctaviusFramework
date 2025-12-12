# Octavius Framework

Aplikacja desktopowa wraz z prostą wtyczką do przeglądarki napisana w Kotlinie i używająca Compose Multiplatform.
Służy do zarządzania kolekcjami mediów azjatyckich (mang, powieści) oraz gier.

Wykorzystuje autorskie silniki do formularzy, raportów oraz dostępu do danych

## Kluczowe funkcje

###  Zarządzanie kolekcjami
- **Publikacje azjatyckie**: Kompleksowe śledzenie powieści, mangi, manhw z postępem czytania
- **Gry**: Organizacja kolekcji gier z seriami, platformami i metadanymi

### Zaawansowane frameworki
- **System formularzy**: Własny silnik z prostymi kontrolkami, kontrolkami wyboru, sekcjami i kontrolką sekcji powtarzalnej
- **System raportów**: Dynamiczne tabele z filtrowaniem, sortowaniem, zarządzaniem kolumnami
- **System bazodanowy** [Opis](database/README.md)

## Stack technologiczny

### Główne Technologie
- **Kotlin Multiplatform**
- **Compose Multiplatform** (desktop, web)
- **PostgreSQL 17+** z wieloma schematami
- **Spring JDBC** + **HikariCP**
- **Material 3** design system
- **kotlinx-serialization** dla JSON

## 🚀 Uruchamianie

### Wymagania systemowe
- **JDK 24+**
- **PostgreSQL 17+**
- **Baza danych** `octavius` ze schematem z `baza.sql`

### Budowanie i uruchamianie

```bash
./gradlew build
./gradlew run
```

### Dodatkowe komendy

**Walidacja tłumaczeń:**
```bash
./gradlew validateTranslations
```
Sprawdza użycie kluczy tłumaczeń w kodzie i raportuje nieużywane tłumaczenia.

**Budowanie rozszerzenia przeglądarki:**
```bash
./gradlew assembleBrowserExtension
```
Kompiluje rozszerzenie do `browser-extension/build/extension`.

## Architektura

### Struktura modułowa

```
Octavius/
├── desktop-app/           # Główna aplikacja i punkt wejścia
├── core/                  # Fundamenty: domain, localization, util
├── database/              # Warstwa dostępu do danych (Spring JDBC)
├── form-engine/           # Framework formularzy
├── report-engine/         # Framework raportów  
├── ui-core/               # Współdzielone komponenty UI i system nawigacji
├── feature-asian-media/   # Moduł publikacji azjatyckich
├── feature-games/         # Moduł gier
├── feature-settings/      # Moduł ustawień
├── feature-contract/      # Interfejsy dla modułów funkcjonalnych
├── api-server/            # API server
├── api-contract/          # Kontrakty API
└── browser-extension/     # Rozszerzenie przeglądarki
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

## Wzorzec domenowy

Każda encja biznesowa następuje konsekwentny wzorzec:

```
feature-[domain]/
├── form/
│   ├── [Entity]FormDataManager.kt    # Operacje bazodanowe
│   ├── [Entity]FormSchemaBuilder.kt  # Definicja struktury formularza
│   ├── [Entity]FormValidator.kt      # Reguły walidacji
│   └── ui/
│       └── [Entity]FormScreen.kt     # UI formularza
├── navigation/
│   └── [Entity]Tab.kt                # Definicja zakładki
└── report/
    ├── [Entity]ReportStructureBuilder.kt # Definicja struktury raportu
    └── ui/
        └── [Entity]ReportScreen.kt   # UI raportu
```

## 🌍 System lokalizacji

- **Tłumaczenia oparte o pliki JSON**: `translations_pl.json` w każdym module
- **Singleton Translations**: Globalny dostęp przez `T.get()` i `T.getPlural()`
- **Obsługa liczby mnogiej**: Wsparcie dla form "one", "few", "many"
- **Walidacja**: Automatyczne sprawdzanie użycia kluczy tłumaczeń przez task `validateTranslations`
