# Varagh (ورق)

A PDF reader for people who love books. Persian-first (RTL, Vazirmatn), English as a second
language. Read comfortably with eye-friendly page themes, keep a history of everything you read,
and build a reading habit. Works fully offline; an optional server adds sync and social features.

## Features

| Area | What you get |
|---|---|
| **Library** | Import PDFs via the system file picker (persisted access), duplicate detection by SHA-256, cover from page 1, grid/list, search by title/author, filter by status, sort by last opened / title / date added. |
| **Reader** | Fast `PdfRenderer` pages with an LRU bitmap cache and prefetching; vertical scroll or page-by-page (right-to-left option for Persian books); pinch / double-tap zoom; page themes Day, Sepia, Soft grey, Dark, Night (AMOLED) and Custom; warm-light filter; in-app brightness; keep screen on; immersive mode; page slider and go-to-page; bookmarks with notes; reopens exactly where you left off. |
| **History & stats** | Books by status, finish dates (Solar Hijri calendar in Persian), books finished this year, pages, minutes, reading streak, books-per-month chart; per-book time, sessions and bookmarks. |
| **Profile** | Name, username, bio, avatar, auto-updating "currently reading" card and an offline **share card** image. |
| **Settings** | Default reading theme/layout, app theme (system/light/dark), dynamic colour (Android 12+), language (فارسی / English), JSON backup & restore. |
| **Social** *(server only)* | Feed of what followed readers start/read/finish, discover readers, public profiles, follow/unfollow, "readers of this book", sign in / register. |

PDF files **never** leave the device. With the server enabled only metadata, progress, sessions and
stats are synced.

## Architecture

MVVM + Clean Architecture, Hilt, Coroutines/Flow, Jetpack Compose (Material 3), Room, DataStore,
Retrofit/OkHttp/kotlinx.serialization, WorkManager, Coil.

```
:app                     single activity, navigation shell, theme/language, Hilt app + WorkManager
:core:model              plain Kotlin models (Book, ReadingSession, UserPreferences, FeatureFlags, …)
:core:domain             repository interfaces + use cases (all business rules live here)
:core:data               Local… (Room) and Remote… (Retrofit + Room cache) repositories,
                         DataStore settings, files/covers/avatars, backup, sync, the backend switch
:core:database           Room entities, DAOs, migrations (schemas exported in core/database/schemas)
:core:network            Retrofit API (docs/api-contract.yaml), DTOs, JWT interceptor/authenticator
:core:designsystem       theme, Vazirmatn type, components, reading-theme colour matrices, motion
:feature:library|reader|history|profile|social|settings
```

Features depend only on `:core:domain`, `:core:model` and `:core:designsystem`; they never see
Room or Retrofit. Build logic lives in `build-logic/` (convention plugins) and versions in
`gradle/libs.versions.toml`.

### The backend switch

One boolean decides everything:

```properties
# gradle.properties
varagh.useRemoteBackend=false
varagh.apiBaseUrl=https://api.example.com/v1/
```

`core/data/build.gradle.kts` turns these into `BuildConfig.USE_REMOTE_BACKEND` /
`BuildConfig.API_BASE_URL`. `RepositoryModule` (in `:core:data`) picks the implementation of every
repository from that flag using `Provider`s, so the unused side is never even constructed, and the
injected `FeatureFlags` hides every server-only screen and button.

| | `false` (default) | `true` |
|---|---|---|
| Repositories | `Local…` (Room) | `Remote…` (Room + background sync over Retrofit) |
| Social tab, login, public profile toggle, "readers of this book" | hidden, routes not registered | shown |
| `INTERNET` permission | **not requested** | requested |
| Works offline | yes | yes (Room is the source of truth; sync retries later) |

Flip it without editing files:

```bash
./gradlew assembleDebug -Pvaragh.useRemoteBackend=true -Pvaragh.apiBaseUrl=https://your.server/v1/
```

No code changes are needed, only a rebuild and a server implementing the contract.

### Sync (remote builds)

Room stays the UI's source of truth. Remote repositories write locally with
`sync_state = PENDING` (deletions become `DELETED` tombstones) and schedule `SyncWorker`
(WorkManager, network-constrained, exponential backoff, plus a 6-hourly periodic run while signed
in). `SyncEngine` pushes pending rows (`POST/PATCH/DELETE /library`, `POST /sessions`,
`PATCH /me`), then pulls `GET /library` and applies newer server changes (last-write-wins by
`updatedAt`; unsynced local edits always win). Bookmarks have no API endpoints and stay on the device.

Auth: JWT access/refresh tokens, encrypted with Tink (AES-256-GCM, key wrapped by the Android
Keystore) in a dedicated DataStore; an OkHttp interceptor adds the bearer token and an
authenticator refreshes once on 401 (failing refresh signs out). All network failures surface as
clear, localized error states with retry.

### API contract

[`docs/api-contract.yaml`](docs/api-contract.yaml) (OpenAPI 3) describes every endpoint the app
uses, so the backend (planned: Spring Boot + JWT) can be built independently.

## Building

Requirements: Android Studio (latest stable) or JDK 17+ (Android Studio's bundled JBR works),
Android SDK 37.

```bash
./gradlew assembleDebug          # debug APK (applicationId com.mid.varagh.debug)
./gradlew test                   # unit + Robolectric tests (JVM, no device needed)
./gradlew connectedDebugAndroidTest   # instrumented tests incl. Room migrations (device/emulator)
```

On Windows without `JAVA_HOME`, point it at Android Studio's JBR, e.g.
`set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`.

### Release builds

1. Create an upload key once:
   `keytool -genkeypair -v -keystore varagh-upload.jks -alias varagh -keyalg RSA -keysize 4096 -validity 10000`
2. Copy `keystore.properties.example` to `keystore.properties` (git-ignored) and fill it in.
3. `./gradlew bundleRelease` (Play) or `./gradlew assembleRelease` (APK).

Release builds are minified and resource-shrunk with R8. Without `keystore.properties` the build
still works but is signed with the debug key (a warning is printed); never upload that.

Bump `versionCode` / `versionName` in `app/build.gradle.kts` for each release.

### Database migrations

The schema is exported to `core/database/schemas`. For every change: bump
`VaraghDatabase.VERSION`, add a `Migration` to `VaraghMigrations.ALL`, build once and commit the new
schema JSON. `SchemaPolicyTest` (JVM) fails if a schema or migration step is missing and
`MigrationTest` (instrumented) runs every upgrade. Release builds never use destructive migration.

## Performance & memory notes

- Pages render on a single background thread (PdfRenderer is not thread-safe) at screen width,
  snapped to 64 px buckets, and are re-rendered at 2x only while zoomed.
- Rendered bitmaps live in an LRU cache bounded to 1/6 of the heap (24–96 MB) and are trimmed on
  `onTrimMemory`; pages that scroll out of view cancel their pending render, and neighbours are
  prefetched. Page sizes for long books (500+ pages) are measured in the background.
- The document is closed when the reader's ViewModel is cleared; covers are 360 px JPEGs.

## Known limitations (v1)

- Reading themes recolour the whole page, including pictures ("preserve images" is out of scope).
- No table of contents / outline (PdfRenderer does not expose it).
- Password-protected PDFs are not supported by the platform renderer and are rejected on import.
- With the server enabled, library entries are synced for books that exist on the device; a book
  added on another phone appears here only after its PDF is imported here as well.

## Privacy

Varagh has no analytics or ads. The offline build cannot access the network at all. PDFs are
read in place through the Storage Access Framework and are never copied off the device.

## License

Code: Apache License 2.0 (see `LICENSE`). Font: Vazirmatn, SIL Open Font License 1.1
(see `core/designsystem/VAZIRMATN_OFL.txt`).
