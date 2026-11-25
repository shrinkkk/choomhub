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
- ✅ Face is the shared black cat, seven moods from crying to ecstatic as the meetup gets closer

## Milestone 1.3: Roles and ink style ✅

- ✅ Good Boy / Good Girl chosen during setup (first pick decides both), synced and rule-protected
- ✅ "Forehead kiss" gesture
- ✅ All art redrawn in the ink style from the owner's reference

## Milestone 1.4: Time of day and the cat icon ✅

- ✅ Dawn / day / dusk / night skies, all dark; surfaces tinted per phase
- ✅ Home corner scene: branch with ravens (day) or sleeping bats (night) against the sun or moon
- ✅ Header art no longer rotates; motifs placed on the other screens
- ✅ App icon is the bored cat (plus a themed-icon silhouette)

## Milestone 2: Backend foundation and pairing 🚧

- ✅ Firebase (Spark, free) wired from `local.properties`; device-only mode without it
- ✅ Google sign-in (Credential Manager)
- ✅ Start a space / join with a single-use, 48-hour invite code; at most two people
- ✅ Names and meetup synced through Firestore (offline cache, queued writes)
- ✅ Security rules + emulator tests (28 cases now)
- ⬜ Verified on real devices (sign-in, start space, join from a second account/emulator)
- ⬜ Avatars

## Milestone 3: Moods and gestures 🟡

- ✅ Mood picker (an optional 80-character note), synced; partner's mood and yours on Home
- ✅ Editable shared mood list (Settings → Moods): add custom moods, remove any, restore defaults
- ✅ Hugs, forehead kisses, head pats, tomato deliveries: sent, kept in a shared history, last each
  way shown on Home, 3-second cooldown per gesture
- ✅ Push notifications for both, each with its own switch (Gestures, Moods)
- ⬜ Thinking-of-you, comfort, custom gestures
- ⬜ Current activity/status, daily check-ins, stay-with-me mode

## Milestone 4: Notifications 🟡 (built; needs the one-time setup and a device check)

- ✅ FCM registration by installation id, synced per phone; removed on sign-out
- ✅ Channels per category (Meetup, Space, Other); Android 13+ permission asked once
- ✅ Per-category switches in Settings, respected by the Worker; "Send a test" button
- ✅ Cloudflare Worker (free) that verifies the sender and sends FCM HTTP v1 pushes (unit + emulator tests)
- ✅ Queued sending with retries (WorkManager); meetup set/cleared, joined, renamed notify the partner
- ⬜ Deploy the Worker and verify on both phones ([setup guide](NOTIFICATIONS_SETUP.md))
- ⬜ New categories as features land (moods, gestures, snaps, messages)
- ⬜ Fallback: periodic background check, only if pushes prove unreliable

## Milestone 5: Snaps and widget ✅

(Random snap prompts were built and then removed at the owner's request: they felt robotic.)

- ✅ Camera only (no gallery, by choice), caption, on-phone compression (≤ 1280 px, < 850 KB), stored in Firestore
- ✅ Partner's latest snap on Home ("new" until opened); sending a new one removes your previous unless kept
- ✅ Keep → collections in Our World → Memory Lane (create, toggle per snap, grid per collection)
- ✅ Memory Lane as polaroid strings: closed stacks that unfold in place, date on the frame, time
  stamped on the photo, a polaroid look on kept photos; long-press to rename/delete a collection
  or to set a cover / remove a photo; empty collections from + New; keep picker on the snap screen
- ✅ Push notification for new snaps (Snaps switch)
- ✅ Reactions (one each, from 🖤 🥺 😭 😂 🔥 🍅), pushed to the sender
- ✅ Home-screen widget with their latest snap; refreshes when a snap arrives, when you leave the app, and hourly

## Milestone 6: Messages ⬜

- ⬜ Text messages, reactions, shared mailbox
- ⬜ Letters, open-when letters, time capsules, random compliments
- ~~Voice notes~~ (dropped, ADR-023)

## Milestone 7: Productivity and points 🟡

- ✅ Points ledger (append-only; totals are summed, never stored), stacked boxes with yours on top
- ✅ Award or take away your partner's points (1–10, optional reason; totals can go negative), pushed with the role named
- ⬜ Achievements with descriptions
- ⬜ Points history; custom rewards and a point shop

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
