# Architecture

This document separates **what exists now** from **what is planned**. Planned sections describe
the intended design so that early code doesn't paint later milestones into a corner; they are not
implemented yet.

## 1. Overview

Single Android app module, MVVM with Clean Architecture layering, organised by feature.

```
UI (Compose screens)  ->  ViewModel (StateFlow<UiState>)  ->  Repository interface (domain)
                                                                   |
                                                    Repository implementation (data)
                                                      |                         |
                                           Local: Room (source of truth)   Remote: Supabase
                                                      ^                         |
                                                      +---- Sync (WorkManager) -+
```

Today the data layer has a single `SampleHomeRepository` that emits in-memory sample data.
Room, Supabase and sync are planned (sections 4 to 6).

### Package layout (actual)

```
com.choomhub.app
├── ChoomHubApplication.kt        @HiltAndroidApp
├── MainActivity.kt                edge-to-edge, theme, reduced-motion flag
├── ui/ChoomHubApp.kt             root scaffold: backdrop, bottom bar, snackbar, NavHost
├── core/
│   ├── common/                    TimeLabels, Clock provider (Hilt)
│   ├── designsystem/
│   │   ├── theme/                 Color, Type, Shape, Spacing, Motion, ChoomHubTheme
│   │   ├── component/             cards, buttons, badges, headers, backdrop, ComingSoonScreen
│   │   ├── illustration/          Canvas art: moon, sparkles, tomato, cat, scenes, glyphs
│   │   └── icon/                  custom line icons (ImageVector)
│   ├── navigation/                TopLevelDestination, ChoomHubBottomBar
│   └── network/BackendConfig.kt   build-time backend config (unused until Supabase lands)
├── domain/
│   ├── model/                     Member, Mood, PartnerMood, Snap, PointBalances, ...
│   └── repository/                HomeRepository (interface)
├── data/
│   ├── sample/                    SampleHomeRepository (local sample data)
│   └── di/                        DataModule: binds interfaces to implementations
└── feature/
    ├── home/                      HomeRoute, HomeViewModel, HomeUiState, HomeScreen, components
    ├── messages/                  placeholder
    ├── activities/                placeholder
    ├── world/                     "Our World" placeholder
    └── settings/                  status screen
```

The original plan listed a top-level `app/` package; root UI lives in `ui/` instead, to avoid a
`com.choomhub.app.app` package. Packages such as `core/database`, `core/sync`,
`core/notifications`, `data/local`, `data/remote` and `feature/widget` are created when their
first real code arrives, not as empty placeholders.

## 2. MVVM and the repository pattern (implemented for Home)

- **Screens** come in pairs: a stateful entry (`HomeScreen`) that obtains the ViewModel with
  `hiltViewModel()` and collects state with `collectAsStateWithLifecycle()`, and a stateless
  `HomeContent(state, callbacks)` that is previewable and testable without Hilt.
- **ViewModels** expose `StateFlow<UiState>` built with `stateIn(WhileSubscribed(5s))` and plain
  functions for user intents (`onAffectionTapped`). No Android framework types and no network
  calls.
- **UI state** is a sealed `Loading | Ready` type. Mapping domain → UI is a pure function
  (`HomeSnapshot.toUiState(clock)`) and is unit-tested.
- **Repositories** are interfaces in `domain/repository`; implementations live in `data` and are
  bound in `DataModule`. Swapping sample data for Room-backed data is a one-line binding change
  with no UI changes.
- **Use cases** are added only where they hold real business rules (e.g. validating a point
  transaction). Pure pass-throughs are not created.
- **Time** comes from an injected `java.time.Clock`, so time-dependent logic is deterministic in
  tests.
- **Navigation**: each feature owns a `@Serializable` route object and a
  `NavGraphBuilder.xxxScreen()` extension; `ChoomHubApp` composes them.
  `TopLevelDestination` drives the bottom bar.

## 3. Design system (implemented)

- `ChoomHubTheme` wraps `MaterialTheme` with a custom dark `ColorScheme` and exposes extra tokens
  through `ChoomHubTheme.colors` and `ChoomHubTheme.spacing` (CompositionLocals).
- The app is **always dark** by design; no light theme, no dynamic colour.
- Typography: platform serif for display/titles (letters, paperbacks), default sans for body.
  Fonts are system fonts so nothing is bundled yet.
- Illustrations are drawn with Compose `Canvas` (no bitmaps, no emoji), scale to any size and
  pick up palette changes automatically.
- `LocalAmbientMotionEnabled` turns off continuous decorative animation (twinkling stars) when the
  system "remove animations" setting is on. Tests use this as well.

## 4. Offline-first approach (planned)

Offline-first is a core requirement, so it is designed now even though Room isn't added yet.

- **Room is the source of truth for every screen.** Repositories return `Flow`s from DAOs; the UI
  never waits on the network to show previously loaded data. The `HomeRepository` contract
  already states this.
- **Writes are local first.** A user action writes to Room and enqueues a sync operation in the
  same transaction; the UI updates immediately.
- **Remote changes** (Supabase Realtime, push-triggered fetches, periodic catch-up) are written
  into Room, and the UI updates through the same flows.

### Room vs Supabase responsibilities

| Concern | Room (device) | Supabase (server) |
|---|---|---|
| What screens read | Yes | No (never read directly by UI) |
| Pending local writes | Yes (sync queue) | n/a |
| Authoritative state & ordering | Cache of it | Yes (server timestamps, validation) |
| Access control | n/a | Row Level Security |
| Photos / voice notes | Small cache / thumbnails | Private Storage buckets |
| Fan-out to the partner | n/a | Realtime + Edge Function → FCM |

## 5. Synchronisation design (planned)

A single, reusable `core/sync` component:

- **`sync_operations` table** in Room: `id`, `type`, `payload` (JSON), `idempotencyKey` (UUID
  created on the device), `status` (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`),
  `attemptCount`, `lastError`, `createdAt`, `updatedAt`.
- **`SyncWorker`** (WorkManager, `CONNECTED` constraint, unique work, exponential backoff) drains
  pending operations through per-type `SyncHandler`s registered with Hilt multibindings. Adding a
  feature means adding a handler, not editing the worker.
- **Idempotency**: every remote insert carries the idempotency key, enforced unique server-side,
  so retries can never double-apply (vital for point transactions).
- **Conflict strategies by data type**:

| Data | Strategy |
|---|---|
| Current mood / status | Latest valid update wins, by server timestamp |
| Messages, letters, reactions | Append-only |
| Point transactions | Append-only ledger; balances are derived sums, never overwritten |
| Snap history | Preserved; newest valid snap becomes "current" |
| Shared cat state | Server-validated operations (e.g. `feed`, `pet`) via RPC / Edge Function |

- Server-generated timestamps (`created_at default now()`) give authoritative ordering; device
  clocks are only used for display before sync.

## 6. Notification architecture (planned)

1. A row is inserted in Supabase (e.g. a new snap or a hug).
2. A database webhook or trigger invokes an **Edge Function**.
3. The function verifies the event, finds the recipient (the *other* member of the couple) and
   checks their per-category notification preferences.
4. It sends a push with the **FCM HTTP v1 API**, using a service account stored only as an Edge
   Function secret.
5. The device receives it in a `FirebaseMessagingService`, writes or refreshes data into Room
   (and the widget), and shows a notification on the matching channel.

Android notification channels mirror the categories (mood, snaps, messages, affection, points,
cat, …) so the user can also control them from system settings. Device FCM tokens are stored per
member in Supabase under RLS.

## 7. Security principles

- **Row Level Security on every table**: a row is visible only to members of its `couple_id`;
  membership is checked through `auth.uid()`.
- **Exactly two members per couple**, enforced server-side. An invite code only *starts* pairing;
  it is single-use, expiring, and pairing is completed by an authenticated server function. The
  code alone never grants access.
- **Private Storage buckets** with policies scoped to the couple; images are served through signed
  URLs.
- **The app ships only public-client values** (Supabase URL and publishable/anon key). Service-role
  keys and the Firebase service account live only in Edge Function secrets.
- **No sensitive logging**: no message text, captions, image URLs or tokens in logs.
- **Backups**: `allowBackup="false"` and data-extraction rules exclude all app data from cloud
  backup and device transfer.
- End-to-end encryption is not a current requirement.

## 8. Widget architecture (planned)

A Jetpack Glance widget shows the latest received snap, caption and time.

- The widget reads from Room or a small cached image file, never from the network directly.
- Updates are **event-driven**: when a new snap arrives (push → sync → Room), the app calls
  `GlanceAppWidget.update()`. No polling; Android's minimum periodic update interval (~30 min) is
  only a fallback.
- The image is downscaled and cached on-device to stay within widget memory limits.

## 9. Configuration

Backend values are injected at build time from the **git-ignored** `local.properties` into
`BuildConfig` and read through `core/network/BackendConfig`:

```properties
# local.properties (never committed)
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
choomhub.supabase.url=https://YOUR-PROJECT.supabase.co
choomhub.supabase.publishableKey=YOUR_PUBLISHABLE_OR_ANON_KEY
```

When the keys are absent (the current state) the build still works, `BackendConfig.isConfigured`
is `false`, and Settings shows "Not configured · showing sample data".

Firebase will need `app/google-services.json` (also git-ignored). Supabase CLI files for Edge
Functions and SQL migrations will live under a top-level `supabase/` directory, with secrets set
through `supabase secrets set`, never committed.

## 10. Testing

- **Unit tests** (`src/test`): pure logic (`TimeLabels`) and ViewModels with fake repositories, a
  fixed `Clock` and `kotlinx-coroutines-test`.
- **Robolectric UI tests** (`src/test`): launch the real `MainActivity` with Hilt on the JVM,
  check that Home renders and walk every bottom-nav tab.
- **Screenshot capture** (`ScreenshotCaptureTest`): renders each tab to PNG under
  `app/build/outputs/screenshots/` for visual review without a device (no pixel comparison).
- Instrumented (on-device) tests will be added once there is device-specific behaviour (widget,
  notifications).

## 11. Future extensibility

- New features add a package under `feature/`, a route plus a `NavGraphBuilder` extension, a
  repository interface plus implementation, and (when they write data) a `SyncHandler`.
- Splitting into Gradle modules (`:core:designsystem`, `:feature:*`) is possible later because
  dependencies already point one way (feature → domain/core, data → domain). It is deliberately
  deferred until build times or boundaries justify it.
