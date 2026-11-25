# Roadmap

Status key: ✅ done · 🚧 in progress · ⬜ planned

The order after Milestone 1 is a proposal; it will change as we go.

## Milestone 1: Foundation and visual prototype ✅

- ✅ Android project: Kotlin, Compose, Material 3, Hilt, Navigation Compose, version catalog
- ✅ Dark design system: colours, type, shapes, spacing, cards, buttons, illustrations, icons
- ✅ Home screen with local sample data (mood, latest snap, affection, points, cat)
- ✅ Bottom navigation: Home, Messages (now Pet House), Activities, Our World, Settings
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
- ✅ Leave a space from Settings (type "I hate *name*" to confirm); come back with a new code on any Google account and keep everything; the last one out deletes it all
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
- ✅ Focus room (Activities): a pixel-art room with both desks; entering turns on Do Not Disturb,
  starts your stopwatch and tells them; breaks on the beanbags with an alarm when they end (and
  how far over you are); a scoreboard (this week / all time), 1 point per focused hour, fire and
  rain sounds, nudges, and a jam link hidden in the record player
- ✅ Memory Lane as polaroid strings: closed stacks that unfold in place, date on the frame, time
  stamped on the photo, a polaroid look on kept photos; long-press to rename/delete a collection
  or to set a cover / remove a photo; empty collections from + New; keep picker on the snap screen
- ✅ Push notification for new snaps (Snaps switch)
- ✅ Songs below the snap: share one from Spotify (Share → ChoomHub, or paste a link), cover and play button on their phone, rated 1–10 with a stamp on the cover; Songs switch
- ✅ Reactions (one each, from 🖤 🥺 😭 😂 🔥 🍅), pushed to the sender
- ✅ Home-screen widget with their latest snap; refreshes when a snap arrives, when you leave the app, and hourly

## Milestone 6: Pet House ✅

Replaces the Messages tab (ADR-039); messages and letters are dropped for now.

- ✅ Adopt a black kitten together: one suggests a name, the other agrees (or bring it home anyway); that day is its birthday, and it's a kitten for 21 days
- ✅ Six needs that drift in real time (food, water, energy, litter, fun, love), worked out from timestamps so nothing runs on a server
- ✅ Hands-on care: food choices with a favourite and a dislike, hold-to-pour water, scoop the clumps, petting by zone (the belly bites), brushing, a chase game with the toys you own, weekly nail trims
- ✅ Two-person jobs: the vet and baths need one of you to start and the other to help (pushed), then three days of medicine
- ✅ Personality (two traits), a bond with each of you, a care streak, weight (overfeeding makes it chonky)
- ✅ Its own apartment that it wanders around, and where it shows what it needs; daily gifts (rocks and fossils are rare), a diary in its voice, 18 trophies
- ✅ Separate cat coins for each of you (care, a daily visit, focus hours); a shop with food, toys, furniture, 15 outfits and decor
- ✅ Links: gestures give it a little love, focus hours give coins, petting together gives a double purr, a status line on Home
- ✅ Gentle reminders on your own phone (never at night, at most three a day) and a "Pet House" notification switch

## Milestone 7: Productivity and points 🟡

- ✅ Points ledger (append-only; totals are summed, never stored), stacked boxes with yours on top
- ✅ Award or take away your partner's points (1–10, optional reason; totals can go negative), pushed with the role named
- ⬜ Achievements with descriptions
- ✅ Points shop (Activities, full screen): 12 starting rewards, buy with your own points, cash in, they mark it done, give back unused ones; Shop / Yours / Theirs tabs
- ✅ Custom rewards, price changes and removals, each needing the other one to agree
- ⬜ Points history

## Milestone 8: Shared cat ✅

Built as the Pet House (Milestone 6).

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
