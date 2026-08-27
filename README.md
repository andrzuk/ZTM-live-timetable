# Rozkład ZTM Poznań – Live Timetable

An Android application displaying **real-time public transit departures** for the city of Poznań, Poland. The app integrates with the Poznań Open Data / ZTM API and GTFS data to show live departure boards for trams and buses, nearby stops based on GPS location, and service alerts.

---

## Features

- **Live departure board** – real-time arrivals and departures for any stop, refreshed automatically every 15 seconds
- **Nearby stops** – uses device GPS to list the closest stops, sorted by distance
- **Stop search** – search stops by name, symbol, or line number with instant filtering
- **Filter by vehicle type** – quick-filter chips for All / Trams / Buses / PST (fast tram)
- **Favourite stops** – save frequently used stops for instant access from the home screen
- **Service alerts** – display warnings and disruptions for each stop
- **Vehicle details** – shows vehicle model, low-floor status, air-conditioning indicator, and delay information
- **Offline-friendly** – falls back to GTFS static data when the live API is unavailable
- **GPS fallback** – defaults to Poznań city centre (Rondo Kaponiera) when location permission is not granted

---

## Screenshots

*(Add screenshots here)*

---

## Technical Specification

| Property | Value |
|---|---|
| Platform | Android |
| Minimum SDK | API 24 (Android 7.0 Nougat) |
| Target SDK | API 36 |
| Language | Kotlin |
| UI framework | Jetpack Compose + Material 3 |
| Architecture | MVVM (`AndroidViewModel` + `StateFlow`) |
| Local storage | Room (SQLite) – favourite stops |
| Networking | Retrofit + OkHttp + Moshi |
| Location | Google Play Services – Fused Location Provider |
| AI integration | Firebase AI (Gemini) via `firebase-ai` |
| App Check | Firebase App Check (reCAPTCHA) |
| Build system | Gradle (Kotlin DSL) |
| Testing | JUnit 4 · Robolectric · Roborazzi · Espresso |

---

## Architecture Overview

```
app/
└── src/main/java/com/example/
    ├── MainActivity.kt               # Single-Activity entry point
    ├── data/
    │   ├── api/
    │   │   └── PoznanZtmApiService.kt  # OkHttp client for Poznań Open Data API
    │   ├── datasource/
    │   │   └── PoznanGtfsData.kt       # Bundled GTFS static data (stops, lines)
    │   ├── local/
    │   │   ├── AppDatabase.kt          # Room database
    │   │   ├── FavoriteStopDao.kt      # DAO for favourite stops
    │   │   └── FavoriteStopEntity.kt   # Room entity
    │   ├── model/
    │   │   └── TransitModels.kt        # Domain models (TransitStop, LiveDeparture, etc.)
    │   └── repository/
    │       └── PoznanTransitRepository.kt  # Single source of truth; merges API + GTFS + Room
    └── ui/
        ├── components/
        │   └── CommonComponents.kt     # Shared composables
        ├── screens/
        │   ├── HomeScreen.kt           # Favourite stops + entry point
        │   ├── SearchScreen.kt         # Stop search and filtering
        │   ├── NearbyStopsScreen.kt    # GPS-sorted nearby stops
        │   └── StopDetailScreen.kt     # Live departure board for a selected stop
        ├── theme/
        │   ├── Color.kt
        │   ├── Type.kt
        │   └── Theme.kt
        └── viewmodel/
            └── TransitViewModel.kt     # Shared ViewModel for the whole app
```

---

## Data Sources

| Source | Purpose |
|---|---|
| [Poznań Open Data API](https://www.poznan.pl/mim/plan/) (`map_service.html`) | Live real-time departures per stop |
| GTFS static feed (bundled) | Stop list, lines, coordinates, timetable reference |

The `PoznanTransitRepository` merges live API results with the bundled GTFS data and exposes a single `getLiveDepartures(stop)` method consumed by the ViewModel.

---

## Requirements

- Android Studio **Meerkat** or newer (supporting Gradle Kotlin DSL)
- JDK 11
- Android device or emulator running **API 24+**
- A `google-services.json` file placed in `app/` (required for Firebase features)
- *(Optional)* A Gemini API key for AI-assisted features

---

## Setup & Build

1. **Clone the repository**

   ```bash
   git clone https://github.com/andrzuk/ZTM-live-timetable.git
   cd ZTM-live-timetable
   ```

2. **Add Firebase configuration**

   Download `google-services.json` from your Firebase project and place it at `app/google-services.json`.

3. **Configure secrets** *(optional – required only for Gemini AI features)*

   Copy `.env.example` to `.env` and fill in your key:

   ```bash
   cp .env.example .env
   # then edit .env and uncomment GEMINI_API_KEY=<your-key>
   ```

4. **Build and run**

   Open the project in Android Studio and click **Run**, or use Gradle from the command line:

   ```bash
   ./gradlew assembleDebug
   ```

   Install on a connected device:

   ```bash
   ./gradlew installDebug
   ```

5. **Run unit tests**

   ```bash
   ./gradlew test
   ```

6. **Run instrumented (on-device) tests**

   ```bash
   ./gradlew connectedAndroidTest
   ```

---

## Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Fetch live departure data |
| `ACCESS_NETWORK_STATE` | Check connectivity before network calls |
| `ACCESS_FINE_LOCATION` | Precise GPS for nearby stop sorting |
| `ACCESS_COARSE_LOCATION` | Fallback location for nearby stop sorting |

---

## Contributing

Contributions are welcome. Please follow the existing code style (Kotlin, Jetpack Compose, MVVM) and make sure your changes pass the existing tests before opening a pull request.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes with clear messages
4. Open a pull request against `main`

---

## License

This project does not currently include an explicit license file. Please contact the repository owner before using the code in your own projects.
