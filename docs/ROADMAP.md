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

## Milestone 1.2: Meetup countdown ✅

- ✅ Live countdown (days/hrs/min/sec) at the top of Home with a moon face: teary → sad → happy → excited
- ✅ Schedule / reschedule / clear (date + time pickers)

## Milestone 2: Backend foundation and pairing 🚧

- ✅ Firebase (Spark, free) wired from `local.properties`; device-only mode without it
- ✅ Google sign-in (Credential Manager)
- ✅ Start a space / join with a single-use, 48-hour invite code; at most two people
- ✅ Names and meetup synced through Firestore (offline cache, queued writes)
- ✅ Security rules + 17 emulator tests
- ⬜ Verified on real devices (sign-in, start space, join from a second account/emulator)
- ⬜ Avatars

## Milestone 3: Moods and gestures ⬜

- ⬜ Mood picker, custom description, current activity/status; partner mood on Home from real data
- ⬜ Hugs, kisses, head pats, tomato deliveries, thinking-of-you, comfort, custom gestures
- ⬜ Daily check-ins, stay-with-me mode

## Milestone 4: Notifications ⬜

- ⬜ FCM token registration, notification channels per category, preferences
- ⬜ Cloudflare Worker (free) that verifies the sender and sends FCM HTTP v1 pushes
- ⬜ Fallback: periodic background check when pushes can't be delivered

## Milestone 5: Snaps and widget ⬜

- ⬜ Camera / gallery, caption, on-phone compression, stored in Firestore
- ⬜ Partner's latest snap on Home; sending a new one deletes your previous unless bookmarked
- ⬜ Bookmarks → collections in Our World → Memory Lane
- ⬜ Reactions, random snap prompts
- ⬜ Glance home-screen widget with event-driven refresh

## Milestone 6: Messages ⬜

- ⬜ Text messages, reactions, shared mailbox
- ⬜ Letters, open-when letters, time capsules, random compliments
- ~~Voice notes~~ (dropped, ADR-023)

## Milestone 7: Productivity and points ⬜

- ⬜ Achievements with descriptions
- ⬜ Append-only point ledger (award / deduct), history
- ⬜ Custom rewards and point shop

## Milestone 8: Shared cat ⬜

- ⬜ Move the cat from Home to Our World
- ⬜ Adoption, name, personality; feeding, petting, moods
- ⬜ Accessories, unlockable decorations, animations

## Milestone 9: Tomato universe ⬜

- ⬜ Tomato gifts, collection, characters and stickers
- ⬜ Shared tomato garden, surprises

## Milestone 10: Our World ⬜

- ⬜ Memory Lane (snap collections, memories, milestones)
- ⬜ Customisable shared room, photo wall, decorations, shared soundtrack
- ⬜ Daily questions, mini-games

## Testing note

Development is tested on a single phone so the app can stay a surprise. Two-person flows can be
tested by joining from an Android emulator signed in with a second Google account.
