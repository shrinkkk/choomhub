# Setting up push notifications (free, no card)

Pushes go: **phone → your Cloudflare Worker → Firebase Cloud Messaging → partner's phone**.
The Worker holds the Firebase service-account key as a secret; the app never sees it.
Everything here is on free plans: Cloudflare Workers Free (100,000 requests a day) and FCM.

You do this once. It takes about 15 minutes. Commands are for PowerShell on Windows, run from the
project folder (`C:\Shri\projects\choomhub`). You need Node.js 20 or newer (`node --version`).

## 1. Publish the updated Firestore rules

The rules now cover two new places: `users/{uid}/devices/{id}` (your registered phones) and
`users/{uid}/settings/notifications` (your on/off switches).

Firebase console → **Firestore Database** → **Rules** → paste all of `firebase/firestore.rules` →
**Publish**.

## 2. Download the Firebase service-account key

Firebase console → gear icon → **Project settings** → **Service accounts** →
**Generate new private key** → **Generate key**. A `.json` file downloads.

- Keep it **outside** the project folder (e.g. `C:\Shri\keys\choomhub-push.json`). Never commit it,
  never put it in `local.properties`, never send it in a chat.
- Anyone with this file can act as your Firebase project's admin. If it leaks, delete that key in the
  same screen and make a new one.

## 3. Make a free Cloudflare account

Sign up at <https://dash.cloudflare.com/sign-up>. The free plan needs no card. When asked, pick a
**workers.dev subdomain** (any name; it becomes part of the Worker's URL).

## 4. Deploy the Worker

```powershell
cd push-worker
npm install
npx wrangler login                 # opens the browser to allow Wrangler into your Cloudflare account
npx wrangler deploy                # prints https://choomhub-push.<your-subdomain>.workers.dev
Get-Content C:\Shri\keys\choomhub-push.json -Raw | npx wrangler secret put SERVICE_ACCOUNT_JSON
```

The last line pipes the key straight into Cloudflare's encrypted secret store (if your terminal
asks you to type the value instead, stop and ask; pasting a multi-line file into a prompt can break it). Until it's set, the
Worker answers every request with an error, which is expected.

## 5. Tell the app where the Worker is

Add one line to `local.properties` (the same file as the Firebase values):

```properties
choomhub.push.workerUrl=https://choomhub-push.<your-subdomain>.workers.dev
```

It must be `https://`. This URL isn't secret: the Worker only acts for signed-in people in a space,
and only toward their own partner.

## 6. Rebuild and test on your phone

1. In Android Studio, press **Run** (the new setting is read at build time).
2. Open the app. On Android 13+ it asks once to allow notifications: allow it.
3. **Settings → Notifications → Send a test**. Within a few seconds you should get "Test
   notification" with the small raven. Try it with the app closed too.

If it says *"This phone isn't registered yet"*, wait a minute (the phone registers with Google in the
background on first launch) and try again.

## What notifies your partner today

| You do | They see | Switch |
|---|---|---|
| Set or move the meetup | "*Name* set the next meetup" · "Sat 14 Dec, 7:30 pm" | Meetup |
| Clear the meetup | "*Name* cleared the next meetup" | Meetup |
| Join their space | "*Name* joined your space" | Space |
| Change what you call them | "*Name* changed your name" · You're "…" to them now | Space |
| Send a hug / forehead kiss / head pats / tomato | "*Name* sent you a hug" · Consider yourself hugged. | Gestures |
| Send a snap | "*Name* sent a snap" · its caption | Snaps |
| React to their snap | "*Name* reacted 🖤 to your snap" | Snaps |
| Award them points | "*Name* awarded you 3 Good Girl points" · the reason | Points |
| Take points away | "*Name* took away 2 of your Good Girl points" · the reason | Points |
| Change your mood | "*Name* updated their mood" · Missing you · rain again | Moods |
| Enter the focus room | "*Name* has entered the focus room" | Focus room |
| Nudge them from the focus room | "*Name* wants you in the focus room" | Focus room |
| Leave a jam link in the record player | "*Name* left a jam in the record player" | Focus room |
| Suggest a name for the cat | "*Name* has a name for the cat" · "Mochi". Open the Pet House to agree | Pet House |
| Start a vet trip or a bath | "*Name* needs your help taking Mochi to the vet" / "giving Mochi a bath" | Pet House |

The Pet House also shows gentle reminders on your **own** phone (hungry, dry bowl, litter, hiding,
bored, poorly, medicine): never between 22:30 and 08:30, at most three a day and four hours apart,
under the same Pet House switch.

*Name* is whatever **they** call **you**. Each person's switches are in Settings and respected by the
Worker; Android's own per-channel settings work too.

## Testing the partner side with one phone

The test button checks your own phone. To see a notification arrive "from her", use an Android
emulator signed into a second Google account, join your space from it (Settings → invite code), then
change the meetup on the emulator and your phone should buzz.

## If something goes wrong

- **Test says the push service refused it (404/405)**: the Worker URL in `local.properties` is wrong
  or has a typo. Rebuild after fixing it.
- **Test says "Couldn't reach the push service"**: no internet, or the Worker isn't deployed.
- **Nothing arrives, but the test says "Sent"**: check Android settings → Apps → ChoomHub →
  Notifications are on, and the phone isn't in Do Not Disturb. Some phones (Xiaomi, Oppo, Vivo,
  Realme) also need **Autostart** / no battery restriction for ChoomHub to get pushes while closed.
- **Worker logs**: Cloudflare dashboard → Workers & Pages → choomhub-push → Logs. They never contain
  message text or keys.
- **Errors mentioning FCM permission (403)**: Google Cloud console for the same project →
  APIs & Services → enable **Firebase Cloud Messaging API**.

## Developing the Worker

```powershell
cd push-worker
npm test                     # unit tests (token checks, message building)
npm run test:integration     # end-to-end against the Firestore emulator (needs Java; uses ../firebase)
```
