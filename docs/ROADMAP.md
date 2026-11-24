# Roadmap

Status key: ✅ done · 🚧 in progress · ⬜ planned

The order after Milestone 1 is a proposal; it will change as we go.

## Milestone 1: Foundation and visual prototype ✅

- ✅ Android project: Kotlin, Compose, Material 3, Hilt, Navigation Compose, version catalog
- ✅ Dark design system: colours, type, shapes, spacing, cards, buttons, illustrations, icons
- ✅ Home screen with local sample data (mood, latest snap, affection, points, cat)
- ✅ Bottom navigation: Home, Messages, Activities, Our World, Settings
- ✅ Placeholder screens for the other tabs; Settings shows config status
- ✅ Unit tests and Robolectric navigation tests
- ✅ README, architecture, roadmap and decision docs

## Milestone 1.1: Identity and visual direction ✅

- ✅ Renamed to ChoomHub everywhere (package `com.choomhub.app`)
- ✅ First-run naming: you name your partner; editable in Settings (device-local until sync)
- ✅ Darker, less cartoonish look: muted palette, bundled Cormorant Garamond + Space Mono,
  film grain, line-art gesture icons
- ✅ Rotating gothic header art (raven, moth, candle, key, moon and crows, cobweb), crows in scenes
- ✅ Raven-and-moon launcher icon; toned-down copy

## Milestone 2: Backend foundation and pairing ⬜

- ⬜ Supabase project setup, SQL migrations, RLS policies (`couples`, `members`)
- ⬜ supabase-kt + Ktor client, configuration from `local.properties`
- ⬜ Authentication (sign-in method to be decided)
- ⬜ Couple pairing via single-use invite and server-side validation
- ⬜ Sync names both ways (your name for them → their phone), avatars

## Milestone 3: Offline-first core ⬜

- ⬜ Room database and first entities
- ⬜ DataStore for local preferences
- ⬜ Sync queue (`sync_operations`), `SyncWorker`, handler registry, backoff, idempotency
- ⬜ Realtime subscription writing into Room

## Milestone 4: Moods and affection ⬜

- ⬜ Mood picker, custom description, current activity/status
- ⬜ Partner mood on Home from real data
- ⬜ Hugs, kisses, head pats, tomato deliveries, cuddles, thinking-of-you, comfort, custom gestures
- ⬜ Daily emotional check-ins, stay-with-me mode

## Milestone 5: Notifications ⬜

- ⬜ FCM integration, token registration
- ⬜ Edge Function sending FCM HTTP v1 pushes to the partner
- ⬜ Notification channels and per-category preferences

## Milestone 6: Snaps and widget ⬜

- ⬜ Camera / gallery capture, captions, private Storage upload (Coil for loading)
- ⬜ Latest snap and optional snap history, reactions, random snap prompts
- ⬜ Glance home-screen widget with event-driven refresh

## Milestone 7: Messages ⬜

- ⬜ Text messages, reactions, shared mailbox
- ⬜ Voice notes
- ⬜ Love notes, open-when letters, time capsules, random compliments

## Milestone 8: Productivity and points ⬜

- ⬜ Achievements with descriptions
- ⬜ Append-only point ledger (award / deduct), history
- ⬜ Custom rewards and point shop

## Milestone 9: Shared cat ⬜

- ⬜ Move the cat from Home to Our World
- ⬜ Adoption, name, personality
- ⬜ Feeding, petting, moods (server-validated)
- ⬜ Accessories, unlockable decorations, animations

## Milestone 10: Tomato universe ⬜

- ⬜ Tomato gifts, collection, characters and stickers
- ⬜ Shared tomato garden, surprises

## Milestone 11: Our World ⬜

- ⬜ Customisable shared room, photo wall, decorations
- ⬜ Memories, relationship milestones, shared soundtrack
- ⬜ Daily questions, mini-games

## Testing note

Development is currently tested on a single phone, so two-person flows (pairing, push to the
partner, realtime) will need either a second device or emulator, or a test account driven from
the Supabase dashboard / SQL, when those milestones arrive.
