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

**Date:** 2026-11-24 · **Status:** Accepted

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
