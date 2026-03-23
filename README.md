# Unmi - Local Domain Asset Manager

<p align="center">
  <strong>Privacy-first domain portfolio management for Android</strong>
</p>

<p align="center">
  Kotlin &bull; Jetpack Compose &bull; Material Design 3 &bull; Room &bull; Hilt
</p>

---

## Features

- **Domain Management** — Add, edit, delete, search, filter and batch-manage domains
- **Auto Identify** — One-tap RDAP/WHOIS lookup fills registrar, dates, nameservers and pricing (via nazhumi.com API)
- **Domain Valuation** — Multi-factor algorithm estimates domain value (length, TLD, keywords, age)
- **Renewal Statistics** — Budget forecasts at 7/30/90/365 day intervals, registrar & TLD distribution
- **WHOIS Lookup** — ICANN RDAP with fallback to traditional WHOIS for 1200+ TLDs
- **Encrypted Storage** — AES-256-GCM encryption with PBKDF2-derived keys; password = account
- **Multi-Account** — Each password creates an isolated encrypted data space
- **i18n** — Chinese & English, switchable in settings or follow system
- **Material Design 3** — Dynamic color, dark mode, polished UI

## Architecture

```
UI Layer          Jetpack Compose + ViewModel + StateFlow (UDF)
Domain Layer      (optional) Valuation Engine
Data Layer        Repository → Room + DataStore + Retrofit/OkHttp
DI                Hilt
Security          PBKDF2-SHA256 + AES-256-GCM (no Keystore dependency)
```

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| Preferences | DataStore |
| Network | Retrofit + OkHttp |
| DI | Hilt (Dagger) |
| Async | Coroutines + Flow |
| Security | PBKDF2 + AES-GCM |
| Min SDK | 26 (Android 8.0) |

## Build

```bash
# Debug
./gradlew assembleDebug

# Release (minified, ~6MB)
./gradlew assembleRelease
```

Release builds enable R8 code shrinking, resource shrinking, and ProGuard obfuscation.

## Security Model

- Password hashed with **PBKDF2-SHA256** (100K iterations)
- Encryption key derived from password (separate salt), never stored
- Domain sensitive fields encrypted with **AES-256-GCM**
- Session key cleared when app goes to background
- Database alone cannot be decrypted without the password

## Project Structure

```
app/src/main/java/io/unmi/app/
├── MainActivity.kt
├── UnmiApplication.kt
├── security/SecurityManager.kt
├── di/                          # Hilt modules
├── data/
│   ├── local/db/                # Room entities, DAOs, database
│   ├── local/datastore/         # DataStore preferences
│   ├── network/                 # RDAP, WHOIS, Nazhumi API, Valuation
│   └── repository/              # Domain & Statistics repositories
└── ui/
    ├── theme/                   # M3 color, typography, shape
    ├── navigation/              # Routes + bottom nav
    ├── components/              # StatCard, DomainListItem
    ├── unlock/                  # Login / register (tab-based)
    ├── home/                    # Dashboard
    ├── domain/{list,detail,edit} # Domain CRUD
    ├── statistics/              # Charts & budgets
    ├── whois/                   # WHOIS/RDAP lookup
    ├── tools/                   # Import/export, backup
    ├── settings/                # Preferences & language
    └── about/                   # App info
```

## License

MIT
