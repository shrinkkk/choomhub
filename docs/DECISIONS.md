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

**Date:** 2026-11-24 · **Status:** Accepted

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

**Date:** 2026-11-24 · **Status:** Accepted

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
