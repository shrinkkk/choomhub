# Architectural decisions

A running log. When a decision changes, add a new entry that supersedes the old one rather than
silently editing history.

---

## ADR-001: Single app module, feature-oriented packages

**Date:** 2026-11-24 · **Status:** Accepted

Start with one `:app` module, organised as `core/`, `domain/`, `data/`, `feature/`, `ui/`.
Multi-module builds add configuration overhead that isn't justified for a two-person app at this
size. Dependencies already flow one way, so modules can be extracted later without rewrites.

## ADR-002: Root UI package is `ui/`, not `app/`

**Date:** 2026-11-24 · **Status:** Accepted

The initial spec sketched an `app/` package. Under the `com.choomhub.app` namespace that produces
`com.choomhub.app.app`, which reads badly, so root composables (`ChoomHubApp`) live in
`com.choomhub.app.ui`.

## ADR-003: AGP 9 built-in Kotlin, KSP for annotation processing

**Date:** 2026-11-24 · **Status:** Accepted

Android Gradle Plugin 9 compiles Kotlin itself and rejects the `org.jetbrains.kotlin.android`
plugin. The project therefore applies only the Compose compiler and serialization Kotlin plugins.
`kapt` is incompatible with built-in Kotlin, so Hilt uses **KSP**. Room will also use KSP.

## ADR-004: Latest stable versions, verified at setup time

**Date:** 2026-11-24 · **Status:** Accepted

Dependency versions were taken from Google Maven / Maven Central / Gradle Plugin Portal metadata
on the setup date (no previews or RCs). Notable consequences:

- Compose BOM 2026.11.01 requires **compileSdk 37.1+**, so the project compiles against **37.2**
  using AGP's `compileSdk { version = release(37) { minorApiLevel = 2 } }` DSL.
- **targetSdk stays at 36** for now: raising it opts into new runtime behaviour and should be a
  deliberate, tested change.
- Gradle wrapper 9.9.2 (AGP 9.5 requires ≥ 9.6).

## ADR-005: Only add dependencies when code uses them

**Date:** 2026-11-24 · **Status:** Accepted

Room, DataStore, WorkManager, Coil, Glance, supabase-kt/Ktor and Firebase are committed
technologies (see README) but are **not** declared until the milestone that uses them. This keeps
the catalog truthful and avoids unused-dependency drift.

## ADR-006: Type-safe Navigation Compose routes

**Date:** 2026-11-24 · **Status:** Accepted

Routes are `@Serializable` objects or classes (Navigation Compose 2.8+ API) instead of string
routes, which gives compile-time checked arguments. This adds the Kotlin serialization plugin,
which supabase-kt will need anyway.

## ADR-007: Sample data behind the real repository interface

**Date:** 2026-11-24 · **Status:** Accepted

Milestone 1 UI reads from `HomeRepository`, bound to `SampleHomeRepository`. Sample data is
flagged (`isSampleData`) and the UI labels it. Replacing it with a Room-backed implementation needs
no UI changes. No fake production credentials exist anywhere.

## ADR-008: Always-dark custom theme, Canvas illustrations, system fonts

**Date:** 2026-11-24 · **Status:** Partly superseded by ADR-016 (fonts, visual direction)

The identity is intentionally dark, so there is no light theme and no dynamic colour. Artwork is
drawn in Compose `Canvas` instead of emoji or bitmaps, so it scales, follows the palette and adds
no binary assets. Typography uses the platform serif and sans faces for now; bundled custom fonts
can replace them by changing two `FontFamily` values in `Type.kt`.

## ADR-009: Respect "remove animations" for ambient motion

**Date:** 2026-11-24 · **Status:** Accepted

Continuous decorative animations (twinkling stars, later drifting rain) are gated by
`LocalAmbientMotionEnabled`, set from the system animator scale. This is an accessibility courtesy,
and it also keeps Compose UI tests from waiting forever on infinite animations.

## ADR-010: Backend configuration via git-ignored `local.properties`

**Date:** 2026-11-24 · **Status:** Superseded by ADR-024 (Firebase config)

Supabase URL and publishable key are read from `local.properties` into `BuildConfig` at build
time. They are public-client values protected by RLS, but keeping them out of git keeps the
repository free of project identifiers. Service-role and Firebase service-account credentials are
never placed in the app; they live in Edge Function secrets.

## ADR-011: JVM UI tests with Robolectric

**Date:** 2026-11-24 · **Status:** Accepted

Navigation and launch checks run as Robolectric tests in `src/test`, so CI and development
machines without emulators can verify that the app starts and every tab is reachable. A
companion test renders screenshots for review. On-device instrumented tests will be added for
widget and notification behaviour.

## ADR-012: Disable Android cloud backup of app data

**Date:** 2026-11-24 · **Status:** Accepted

Private photos, messages and sessions should not be copied into Google cloud backups or
device-to-device transfers. `allowBackup="false"` plus data-extraction rules exclude everything.
The shared space is restored from Supabase after sign-in instead.

## ADR-013: One name everywhere: ChoomHub

**Date:** 2026-11-24 · **Status:** Accepted

The app, package (`com.choomhub.app`), application ID, code identifiers, resources, config keys
(`choomhub.supabase.*`) and docs all use ChoomHub. The application ID changed from the first
prototype build, so that build must be uninstalled; it held no data. Earlier git history still
contains the working title and is left as is (history is never rewritten).

## ADR-014: Each member names the other

**Date:** 2026-11-24 · **Status:** Accepted

On first launch you name your partner; that name is how they appear in the app, on both phones.
Your own name is whatever your partner chose. Either can be changed at any time from Settings.
Modelled as `CoupleNames(partnerName, myName)` behind `ProfileRepository`. Server side (later),
a member's `display_name` will be writable only by the *other* member of the couple via RLS.

## ADR-015: DataStore as interim storage for names

**Date:** 2026-11-24 · **Status:** Amended by ADR-021 (Firestore when configured; DataStore only in device-only mode)

Names are needed before Room and sync exist. They are stored in a single Preferences DataStore
now (the first use of the already-chosen DataStore technology) and will move to Room's members
table when sync arrives, without changing the repository interface. DataStore files are excluded
from backups by the existing data-extraction rules.

## ADR-016: Visual direction: alternative-gloomy, not cute (supersedes parts of ADR-008)

**Date:** 2026-11-24 · **Status:** Accepted

Feedback on the first prototype: too cartoonish, slightly "AI-generated". Changes: desaturated
palette, tighter corner radii, film grain, no faces/sparkles/bubbles, line-art gesture icons,
silhouette-style illustrations, crows and ravens as a recurring motif, plainer copy. ADR-008's
"system fonts" point is superseded: **Cormorant Garamond** and **Space Mono** are bundled (SIL
OFL, subset to Latin, about 470 KB total) because type carries most of the mood. Always-dark and
Canvas illustrations still stand.

## ADR-017: Rotating header art by date

**Date:** 2026-11-24 · **Status:** Accepted

The header shows one of six gothic motifs, changing every three days. It is computed purely from
the calendar date (`GothicMotif.forDate`), so it needs no storage or sync and both phones agree.

## ADR-018: Inline editing instead of dialogs for simple fields

**Date:** 2026-11-24 · **Status:** Accepted

Renaming happens inline inside the Settings card rather than in a pop-up dialog. It is calmer,
keeps context visible, and Compose UI tests could not reliably drive text entry inside a dialog
window under Robolectric.

## ADR-019: Hilt test bindings for UI tests

**Date:** 2026-11-24 · **Status:** Accepted

Robolectric UI tests run under `HiltTestApplication` (set in `robolectric.properties`) and replace
`ProfileModule` with an in-memory fake via `@UninstallModules` + `@BindValue`. This keeps tests
independent of on-disk DataStore state. Repository bindings that tests need to swap live in their
own small Hilt modules for this reason.

## ADR-020: Firebase (Spark) + one Cloudflare Worker instead of Supabase

**Date:** 2026-11-24 · **Status:** Accepted (approved by the owner)

Supabase's API domain (`*.supabase.co`) was blocked by Indian ISPs (Jio, Airtel, ACT) from 24 Feb
2026 and, for us, the dashboard was unreachable on Jio and campus Wi-Fi in Nov 2026. Both users are on
Indian networks, so a blockable backend is unacceptable. The owner also requires **zero cost with no
card on file**. Firebase's Spark plan provides Authentication, Firestore (1 GiB, 50K reads / 20K
writes per day) and FCM at no cost, but not Cloud Storage or Cloud Functions (Blaze only). So: photos
are stored compressed inside Firestore (ADR-022), and push sending (which needs a secret service-account
key) moves to a Cloudflare Worker on its free plan (100K requests/day, no card). Postgres RLS is
replaced by Firestore security rules.

## ADR-021: Firestore's offline cache replaces Room + custom sync queue

**Date:** 2026-11-24 · **Status:** Accepted (approved by the owner)

The original spec's Room source of truth plus a WorkManager sync queue was designed for Supabase,
which has no offline support. Firestore's SDK already provides a persistent on-device cache, offline
write queueing that survives restarts, latency compensation and server timestamps. Duplicating that
would roughly double the backend work. Screens still only see repositories, so Room can be added
later for a specific need.

## ADR-022: Snaps are ephemeral unless bookmarked; images live in Firestore

**Date:** 2026-11-24 · **Status:** Accepted (owner's product decision; implementation pending)

Sending a new snap deletes your previous one unless one of you bookmarked it. Bookmarked snaps can be
added to named collections shown in Our World → Memory Lane. Images are compressed on the phone
(~1280 px, a few hundred KB) and stored as Firestore blobs (1 MiB document limit), since Cloud Storage
isn't free. Ephemerality also keeps usage far below the 1 GiB free storage.

## ADR-023: No voice notes for now

**Date:** 2026-11-24 · **Status:** Accepted (owner's decision)

Voice notes are removed from the roadmap. Messages are text (and letters) only.

## ADR-024: Manual Firebase initialisation from local.properties

**Date:** 2026-11-24 · **Status:** Accepted

Instead of committing `google-services.json` and applying the google-services Gradle plugin, the
project id, app id, API key and Google web client id are read from git-ignored `local.properties` into
`BuildConfig`, and `FirebaseApp` is initialised in `ChoomHubApplication`. Without them the app runs in
device-only mode, which also keeps CI and JVM tests independent of a real project.

## ADR-025: Pairing via rule-checked client batches (no server code)

**Date:** 2026-11-24 · **Status:** Accepted

Without Cloud Functions, creating and joining a space are client-side batched writes. Firestore rules
validate each batch with `getAfter()`: a space starts with only its creator, an invite is single-use,
expires (≤ 3 days; app uses 48 h), can't be consumed by its creator, and a join must add exactly the
caller to a one-member space while consuming the invite. Rules are covered by emulator tests.

## ADR-026: Meetup countdown rules

**Date:** 2026-11-24 · **Status:** Accepted

One shared "next meetup", last write wins. The cat's mood steps up as the day gets closer: more than
30 days away → sad; within 30 days → bored; within 14 → hopeful; within 7 → happy; within 3 →
excited; within 1 day → ecstatic. After the meetup time it shows "It's today." (ecstatic) for 12
hours, then goes back to "nothing planned" (crying cat). Thresholds live in `MeetupCountdown` and are
unit-tested. (Revised 2026-11-25 from three moods to seven at the owner's request.)

## ADR-027: Ink illustration style (supersedes ADR-016's art direction)

**Date:** 2026-11-25 · **Status:** Accepted (owner's reference image)

The owner shared a reference: a black cat in ink, edges made of scratchy pale fur strokes, huge cream
eyes with small pupils, long wiry whiskers, ink splatter, near-monochrome. All art now follows it:
solid ink silhouettes (`InkPalette.Ink`) with seeded hairline "fur" along their outlines
(`scratchEdge`), bone-white accents (`InkPalette.Bone`), splatters and drips, sketchy doubled lines
instead of neat outlines, no glows or gradients. The palette shifted to ink-and-bone: primary actions
are pale bone, violet and rose survive only as faint accents, and red is kept for tomatoes. The
countdown face is the shared black cat (taller, not round), not a moon.

Revision (2026-11-25, owner feedback on previews): the cat is skinny, with a long thin neck and narrow
shoulders under a wider head, so the head and neck read as one silhouette. Fur is drawn by `furEdge`,
which finds each outline point's outward normal and grows short lit tufts away from the body, instead
of the uniform `scratchEdge` hairlines. The cobweb's spider sits on the web itself (no loose thread).
Gesture icons for Hug and Forehead kiss show two simple people, matching the line-icon set. Art
changes are shown to the owner as previews before they ship.

## ADR-028: Push notifications via FCM installation ids, a queued sender and the Worker

**Date:** 2026-11-25 · **Status:** Accepted

- **Target phones by Firebase Installation ID (FID)**, not legacy registration tokens. Firebase now
  marks the token API deprecated, and FCM HTTP v1 has a `fid` target. The app enables
  `firebase_messaging_installation_id_enabled` and stores the id it gets in `onRegistered`.
- **One document per phone** (`users/{uid}/devices/{fid}`), readable only by its owner. The Worker
  reads it with server credentials; a phone switching accounts deletes its document on sign-out.
- **The Worker writes the words.** The app sends only an event name (plus the meetup time). The Worker
  picks the recipient (always the partner, from Firestore), checks their switches, and uses the name
  the recipient chose for the sender. A modified app can't push arbitrary text or reach strangers.
- **Queued with WorkManager**, so changes made offline still notify later; same-kind events replace
  each other while waiting. Not expedited (it would need a foreground notification on older Android).
- **Notification messages** (not data-only), so Android shows them even if the app was killed, on
  per-category channels. While the app is open, the app shows them itself.
- **Plain copy**, consistent with the rest of the app ("Shri set the next meetup", not "💕").
- The periodic background check from ADR-020 is deferred: the queue plus Firestore sync on open cover
  the two-person case; revisit if pushes prove unreliable on her phone's brand.

## ADR-029: Time-of-day sky, Home's branch scene, and the cat as the app icon

**Date:** 2026-11-25 · **Status:** Accepted (owner reviewed previews)

- **Sky phases** with fixed local hours (dawn 05:00, day 08:00, dusk 17:00, night 19:30). Every phase
  stays dark and muted: bruised violet, overcast violet, plum-to-wine, near-black. No blues. Surfaces
  are tinted slightly per phase so cards don't look pasted on. Fixed hours rather than computed
  sunrise/sunset keep it predictable; revisit if it feels off across seasons.
- **No more rotating header art.** The motifs looked out of place up there; they now sit fixed on the
  other screens. Home's top-right corner has a scene instead, drawn as background: a dead branch with
  the sun high behind two ravens facing each other (dawn, day), or the sun/moon low behind two bats
  hanging asleep (dusk, night) so they read as silhouettes. The bats follow the owner's reference
  (one foot gripping, angular folded wings, head tucked at the bottom), in black ink.
- **App icon** = the countdown cat, Bored, on the day sky's violet. Its foreground is rendered from
  `CatFace` into PNGs by `LauncherIconRender` (the fur can't be a vector); the themed icon is a
  vector silhouette. An icon that follows the cat's mood was considered and dropped for now:
  Android has no supported way to change an app's icon, and the activity-alias workaround can remove
  the icon from the home screen on many launchers each time it switches.

## ADR-030: Moods and gestures

**Date:** 2026-11-25 · **Status:** Accepted

- **Moods** are one document per person (`couples/{id}/moods/{uid}`), overwritten on each change:
  only the current mood matters, so there's no history to read or pay for. Moods are a fixed list
  (rules check the key) plus an optional note of up to 80 characters.
- **Gestures** are an append-only collection (`couples/{id}/gestures`), so both phones see the same
  history and nothing can be unsent or edited. Home listens to the newest 20. Each costs one write
  and, for the other phone, one read; far inside the free quota.
- Both are written to Firestore's offline cache straight away and synced when possible; the push
  goes through the same queued sender as before, with the gesture kind or mood as `kind`. The
  Worker validates `kind` against its own list and writes the words itself.
- Tapping the same gesture again within 3 seconds is ignored, and queued pushes of the same gesture
  collapse into one, so an excited thumb doesn't become ten notifications.
- Home keeps "sample" labels on the parts that are still sample (snaps, points, the cat) instead of
  one badge for the whole screen.

## ADR-031: The mood list is the couple's own

**Date:** 2026-11-25 · **Status:** Accepted (owner's request)

The 11 built-in moods are only the starting list. Settings → Moods lets either person add moods
(up to 24 characters, 40 in total) and remove any, or restore the defaults; the list lives in
`couples/{id}/shared/moodOptions` and both phones use it. Custom moods get generated keys (`c_…`)
and a colour from the same muted accents. A saved mood carries its label, so it still reads right
after the list changes. Rules and the push Worker no longer check moods against a fixed list, only
their length; the Worker sends the label as written.
