# Architecture

This document separates **what exists now** from **what is planned**. Planned sections describe the
intended design so early code doesn't paint later milestones into a corner.

## 1. Overview

Single Android app module, MVVM with Clean Architecture layering, organised by feature. The backend
is **Firebase on the no-cost Spark plan** (Authentication, Cloud Firestore, Cloud Messaging), plus
one **Cloudflare Worker** (free plan, planned) to send push notifications. Everything must stay free
with no card on file; see ADR-020.

```
Compose screen ──> ViewModel (StateFlow<UiState>) ──> Repository interface (domain)
                                                            │
                         ┌──────────────────────────────────┴───────────────────────┐
                         │ Firebase configured                                      │ not configured
                         ▼                                                          ▼
          Firestore SDK (on-device cache = source of truth,           DataStore (device-only mode)
          offline writes queued, synced when online)
                         │
                         ▼
          Cloud Firestore (security rules)  ──(planned)──>  Cloudflare Worker ──> FCM ──> partner's phone
```

### Package layout (actual)

```
com.choomhub.app
├── ChoomHubApplication.kt         @HiltAndroidApp; initialises Firebase from local.properties
├── MainActivity.kt                edge-to-edge, theme, reduced-motion flag
├── ui/
│   ├── ChoomHubApp.kt             root: backdrop, gate (sign-in / pairing / naming / main), NavHost
│   └── AppViewModel.kt            computes the gate from SpaceState + names
├── core/
│   ├── common/                    TimeLabels, SecondTicker, Clock provider
│   ├── datastore/                 DataStore<Preferences> provider
│   ├── firebase/                  FirebaseConfig (BuildConfig), FirebaseInitializer
│   ├── notifications/             AppNotifications: channels, showing pushes while the app is open
│   ├── designsystem/              theme, components, illustrations, icons
│   └── navigation/                TopLevelDestination, ChoomHubBottomBar
├── domain/
│   ├── model/                     SpaceState + InviteCodes, CoupleNames + NameRules, Meetup + MeetupCountdown,
│   │                              NotificationCategory, PushEvent, ...
│   └── repository/                SpaceRepository, ProfileRepository, MeetupRepository, HomeRepository,
│                                  NotificationRepository, Notifier
├── data/
│   ├── firebase/                  FirebaseSpaceRepository, FirestoreProfileRepository, FirestoreMeetupRepository,
│   │                              FirestoreNotificationRepository
│   ├── push/                      PushRegistrar, ChoomHubMessagingService, PushApi, PushWorker, WorkManagerNotifier
│   ├── space/                     DeviceOnlySpaceRepository
│   ├── profile/, meetup/          DataStore implementations (device-only mode)
│   ├── home/                      LiveHomeRepository (real names, moods, gestures; sample snap/points/cat)
│   ├── mood/                      device-only mood and gesture repositories
│   └── di/                        DataModule, SpaceModule, ProfileModule, MeetupModule, PushModule, PushClientModule
└── feature/
    ├── account/                   sign-in and pairing screens + AccountViewModel
    ├── onboarding/                "What do you call them?"
    ├── home/                      home screen; meetup/ countdown card, MoonFace, MeetupViewModel
    ├── messages/, activities/, world/   placeholders
    └── settings/                  names, space status / invite code, sign out
firebase/                          firestore.rules, emulator config, rules tests (Node)
push-worker/                       Cloudflare Worker that sends pushes (JS, unit + emulator tests)
```

## 2. MVVM and repositories (implemented)

- Screens come in pairs: a stateful entry (`HomeScreen`) that gets ViewModels with `hiltViewModel()`
  and a stateless `…Content(state, callbacks)` that is previewable and testable.
- ViewModels expose `StateFlow`s and plain intent functions. No Android framework types, no Firebase
  calls. Composables never touch Firebase.
- Repository interfaces live in `domain/repository`. `data/di` picks the implementation at startup:
  **Firestore when `FirebaseConfig.isConfigured`, otherwise device-only DataStore**, so the app still
  builds and runs without any backend config (and all JVM tests run in device-only mode).
- **App gate** (`AppViewModel.gateFor`): `SignedOut → SignIn`, `NoSpace → Pairing`,
  `Waiting/Paired/DeviceOnly → Onboarding` (until the partner is named) `→ Main`. Success in any step
  needs no navigation call; the gate reacts to the repositories.

## 3. Design system (implemented)

Always-dark custom Material 3 theme in ink-and-bone; Cormorant Garamond + Space Mono (bundled, OFL);
film grain. Illustrations follow one ink style (ADR-027): shapes from SVG path data on a 100 × 100
grid (`MotifPaths.kt`), drawn with the `Ink.kt` toolkit: solid ink fills, seeded scratchy fur edges,
splatters, drips, sketchy lines. The meetup countdown uses the shared cat's face.

**Time of day (ADR-029).** `SkyPhase.at(LocalTime)` picks Dawn (05:00), Day (08:00), Dusk (17:00) or
Night (19:30). `MainActivity` re-checks it every 30 s and passes it to `ChoomHubTheme(phase)`, which
tints surfaces and exposes `ChoomHubTheme.sky`. `SkyBackground` draws that phase's gradient and stars;
Home adds `HomeScenery` (branch, sun or moon, ravens or bats) fixed in its top-right corner. The small
gothic motifs sit fixed on other screens (moth, key, cobweb, candle, raven on sign-in). `LocalAmbientMotionEnabled` respects "remove animations".

## 4. Offline-first (implemented via Firestore; ADR-021)

Firestore's Android SDK keeps an **on-device persistent cache** (on by default) and is the local
source of truth:

- Screens observe repository `Flow`s built on snapshot listeners, which emit the cached copy
  immediately, then server updates. Nothing waits for the network to show known data.
- Writes (`set`/`update`) apply to the local cache at once (the UI updates), are **queued while
  offline, persisted across restarts**, and sent when connectivity returns. Repositories don't await
  them for everyday actions (names, meetup).
- Actions that need the server to decide (joining with an invite code) do require a connection and
  say so.
- **Conflicts**: single shared values (meetup, names) are last-write-wins with `updatedAt`
  server timestamps. Future append-only data (messages, point transactions) will be separate
  documents, never overwritten; point balances are derived by summing the ledger.

Room is no longer planned by default; it may be added for a feature that needs it (e.g. widget data
that must be read without the Firestore SDK).

## 5. Firestore data model (implemented)

| Path | Fields | Who can read / write |
|---|---|---|
| `users/{uid}` | `coupleId`, `updatedAt` | only that user; `coupleId` must be a space they're in |
| `couples/{coupleId}` | `members` (1–2 uids), `createdBy`, `createdAt`, `inviteCode` | members read; created with only yourself; one join; creator can swap the invite while alone |
| `invites/{code}` | `coupleId`, `createdBy`, `expiresAt` (≤ 3 days, app uses 48 h), `usedBy` | any signed-in user can *get* by code (no listing); consumed once, not by its creator, only together with joining |
| `couples/{id}/names/{namerUid}` | `nameForPartner` (1–24 chars), `updatedAt` | members read; only `namerUid` writes |
| `couples/{id}/shared/meetup` | `at` (timestamp or null), `updatedBy`, `updatedAt` | members read/write, signed as themselves |
| `couples/{id}/shared/roles` | `goodBoy`, `goodGirl` (uids), `updatedAt` | members read; each can only place themself |
| `couples/{id}/moods/{uid}` | `mood` (key), `label` (≤ 24), `note` (≤ 80 or null), `updatedAt` | members read; only `uid` writes |
| `couples/{id}/shared/moodOptions` | `options` (1–40 `{key, label}`), `updatedBy`, `updatedAt` | members read/write; missing = the 11 defaults |
| `couples/{id}/gestures/{id}` | `kind` (hug, forehead_kiss, head_pats, tomato), `from`, `sentAt` | members read; added as yourself; never edited or deleted |
| `users/{uid}/devices/{fid}` | `platform`, `updatedAt` (doc id = FCM installation id) | only that user; the push Worker reads with server credentials |
| `users/{uid}/settings/notifications` | `meetup`, `space` (booleans), `updatedAt` | only that user; the Worker reads it before sending |

**Pairing** (no Cloud Functions needed): the first person writes the couple, the invite and their
user record in one batch. The second person reads the invite by code, then in one batch marks it used,
adds themself to `members` (`arrayUnion`) and points their user record at the space. The rules check
each step with `getAfter()`, so a partial or forged join is rejected. A third person can never join.

**Names**: each person writes only the name they give the other, so "your partner's name" is the one
you wrote and "your name" is the one they wrote. This works before the partner has joined.

### Planned additions

- **Snaps** (ADR-022): `couples/{id}/snaps/{snapId}` holding a compressed image (≈1280 px JPEG/WebP,
  a few hundred KB, as a Firestore `Blob`), caption, sender, `sentAt`, `bookmarkedBy`. When a person
  sends a new snap, their previous one is deleted **unless bookmarked**. Bookmarked snaps can be added
  to named **collections** (`couples/{id}/collections/{cid}`) shown in **Our World → Memory Lane**.
- Messages (text only, no voice notes, ADR-023), achievements and an append-only points ledger,
  the shared cat.

## 6. Notifications (implemented, ADR-020, ADR-028)

**Registration.** The app opts into FCM's installation-id targeting. When FCM registers the phone,
`ChoomHubMessagingService.onRegistered(fid)` hands the id to `PushRegistrar`, which keeps it in
DataStore and writes `users/{uid}/devices/{fid}` for whoever is signed in. Signing out deletes that
document first, so the old account stops getting pushes on this phone.

**Sending.** A ViewModel calls `Notifier.notifyPartner(event)` after a change (meetup set/cleared,
joined, renamed). `WorkManagerNotifier` queues a `PushWorker` with a network constraint and
exponential backoff; events with the same collapse key replace each other while queued. The worker
POSTs `{event, detail}` to the Worker with the Firebase **ID token**, refreshing it once on 401.

**The Worker** (`push-worker/`, Cloudflare free plan):
1. verifies the ID token (RS256 against Google's securetoken keys, audience/issuer/expiry);
2. finds the caller's couple and partner in Firestore (the `test` event goes to the caller instead);
3. stops if the partner turned that category off;
4. builds the text itself, using the name the *recipient* gave the sender; only the meetup time comes
   from the app, capped at 80 characters;
5. sends an FCM HTTP v1 message per registered phone (`fid` target, channel, tag, raven icon) and
   deletes registrations FCM reports as `UNREGISTERED`.
It authenticates to Google with the service-account key held only as the Worker secret
`SERVICE_ACCOUNT_JSON`, and never logs request contents.

**Showing.** In the background Android displays the notification on its channel (`meetup`, `space`,
`general`). While the app is open, `onMessageReceived` shows the same notification. Android 13+
permission is asked once on reaching the main app; Settings has the switches, a shortcut to the
system screen and a **Send a test** button.

If the Worker is unreachable, nothing breaks: data still syncs and queued sends retry for a while.
Setup steps: [NOTIFICATIONS_SETUP.md](NOTIFICATIONS_SETUP.md).

## 7. Security principles

- Firestore security rules on every path; default deny. Rules are tested on the emulator
  (`firebase/test/rules.test.mjs`, 24 cases).
- Exactly two members per space, enforced by rules. Invite codes are random, single-use, expire, and
  only work together with an authenticated join. They aren't the only gate: auth, rules and the
  two-member limit apply too.
- The app ships only public client identifiers. The FCM service-account key lives only in the
  Worker's secrets; the Worker accepts nothing but a verified ID token and a known event name, and
  only ever notifies the caller's partner (or the caller, for the test).
- No sensitive logging (no messages, captions, image data or tokens).
- `allowBackup="false"` plus data-extraction rules exclude app data from Android backups.
- End-to-end encryption is not a current requirement.

## 8. Widget (planned)

A Glance widget shows the latest snap. It reads cached data on the device (Firestore cache or a small
file the app writes when a snap arrives) and is refreshed when a snap arrives (push → app → widget
update), not by polling.

## 9. Configuration

Firebase values are read at build time from the **git-ignored** `local.properties` into `BuildConfig`
(`core/firebase/FirebaseConfig`). Firebase is initialised manually, so `google-services.json` and the
google-services Gradle plugin are not used, and nothing project-specific is committed.

```properties
# local.properties (never committed)
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
choomhub.firebase.projectId=your-project-id
choomhub.firebase.appId=1:…:android:…          # mobilesdk_app_id in google-services.json
choomhub.firebase.apiKey=AIza…                  # current_key in google-services.json
choomhub.firebase.webClientId=….apps.googleusercontent.com  # Auth → Google → Web SDK configuration
choomhub.push.workerUrl=https://choomhub-push.<subdomain>.workers.dev   # optional; enables pushes
```

The FCM sender id is derived from the app id, so it needs no setting. Without `workerUrl` the app
works normally and Settings says pushes aren't set up.

If any value is missing, the app runs in **device-only mode** (no sign-in; names and meetup stay on
the phone). Google sign-in also needs the debug (and later release) **SHA-1** registered on the
Android app in the Firebase console.

**Rules deployment**: copy `firebase/firestore.rules` into Firebase console → Firestore → Rules →
Publish, or `cd firebase && npm install && npx firebase deploy --only firestore:rules`.

## 10. Testing

- **Unit tests**: pure logic (time labels, names, motifs, countdown, invite codes, space-state mapping,
  app gate) and ViewModels with fakes, fixed `Clock`, fake ticker, `kotlinx-coroutines-test`.
- **Robolectric UI tests**: real `MainActivity` under `HiltTestApplication`; `@UninstallModules` +
  `@BindValue` swap repositories for fakes (profile, meetup, space). Covered: naming flow, rename,
  navigation, countdown states, gesture notice.
- **Screenshot capture**: every screen, countdown moods, sign-in/pairing, motif gallery →
  `app/build/outputs/screenshots/`.
- **Security rules**: `cd firebase && npm install && npm run test:rules` (needs Java for the emulator).
- **Push Worker**: `cd push-worker && npm test` (token verification, service-account signing, message
  building) and `npm run test:integration` (the whole handler against the Firestore emulator with FCM
  faked: partner-only delivery, muting, non-members refused, dead registrations removed).
- **Push client**: request/response handling, retry decisions, token refresh, Settings notifications
  (Robolectric) and the ViewModels that send events.
- **Not yet covered automatically**: the Firebase repositories against a live or emulated backend,
  Google sign-in, and real FCM delivery. Those are verified on a device.

## 11. Future extensibility

New features add a package under `feature/`, a route, a repository interface with Firestore and (if
useful) device-only implementations, rules for their paths, and rules tests. Splitting into Gradle
modules remains possible later.
