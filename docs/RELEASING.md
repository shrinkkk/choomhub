# Releasing ChoomHub (Firebase App Distribution, free)

She installs ChoomHub once through Google's **Firebase App Tester** app. After that, each new version
you upload reaches her as a notification and installs over the old one; her data stays.

Two rules that hold for every release, forever:

1. **Same signing key.** Android only installs an update if it's signed with the same key as the
   version on the phone. Lose the key and she has to uninstall and reinstall.
2. **Higher version number.** Handled for you: `versionCode` is the number of git commits, so a
   build from a newer commit is always higher. Commit before you build a release.

---

## One-time setup

### 1. Make the release key (2 minutes)

Android Studio → **☰ (top-left, the hidden menu bar) → Build → Generate Signed App Bundle or APK…** → **APK** → **Next** → under *Key
store path* click **Create new…**:

- **Key store path:** somewhere outside the project and backed up, e.g.
  `C:\Shri\projects\Assets\keys\choomhub-release.jks`
- **Password:** a strong one, and **save it in a password manager**.
- **Key alias:** `choomhub`, its own password (can be the same), validity 25+ years, your name.

Click **OK**, then **Cancel** the wizard (the build below uses the key automatically).

**Or skip the wizard** and make the key from Android Studio's Terminal tab (it asks for the
password and your name):

```powershell
keytool -genkeypair -v -keystore C:\Shri\projects\Assets\keys\choomhub-release.jks -alias choomhub -keyalg RSA -keysize 2048 -validity 10000
```

Back up the `.jks` file and the passwords (e.g. a private cloud folder *and* a USB stick). Never put
the key in the project folder or git (`*.jks` is git-ignored anyway).

### 2. Tell the build where the key is

Add to `local.properties` (use forward slashes in the path):

```properties
choomhub.release.storeFile=C:/Shri/projects/Assets/keys/choomhub-release.jks
choomhub.release.storePassword=…
choomhub.release.keyAlias=choomhub
choomhub.release.keyPassword=…
```

### 3. Let Google sign-in trust the release key

Google sign-in only works for apps signed with a key Firebase knows. In the project folder's terminal:

```powershell
keytool -list -v -keystore C:\Shri\projects\Assets\keys\choomhub-release.jks -alias choomhub
```

(If `keytool` isn't found, use Android Studio's: `"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"`.)
Copy the **SHA1** line → Firebase console → gear → **Project settings** → your Android app →
**Add fingerprint** → paste → Save. (Keep the debug SHA-1 too, so Android Studio builds still work.)

### 4. Turn on App Distribution

Firebase console → **Run → App Distribution** (or search "App Distribution") → pick the Android
app → **Get started**. Under **Testers & Groups**, make a group (`shri-jas`) and add her Gmail and
yours.

---

## Each release

1. **Commit** your changes (the version number comes from the commit count).
2. Android Studio → **Build → Build App Bundle(s) / APK(s) → Build APK(s)** with the **release**
   variant (Build Variants panel on the left → `app` → `release`), or in the terminal:
   ```powershell
   .\gradlew.bat assembleRelease
   ```
   The file is `app\build\outputs\apk\release\app-release.apk`.
3. Firebase console → **App Distribution → Releases** → drag the APK in → choose the `shri-jas` group →
   write a line of release notes ("snaps can be kept now") → **Distribute**.

Test it on **your** phone first (you're in the group too): open App Tester, install, and check
sign-in, Home, a snap and a notification. Then she gets the same build.

### Optional: from the terminal instead of the console

```powershell
cd firebase
npx firebase login
npx firebase appdistribution:distribute ..\app\build\outputs\apk\release\app-release.apk --app <your Firebase app ID> --groups shri-jas --release-notes "what changed"
```

The app ID is the `choomhub.firebase.appId` value in `local.properties` (`1:…:android:…`).

---

## Her side (once)

1. On her phone, open the invite email from Firebase and **accept** with her Google account.
2. Install **Firebase App Tester** when the page offers it, and sign in with the same account.
3. If Android asks, allow App Tester to **install unknown apps** (Settings opens for this).
4. In App Tester, tap **ChoomHub → Download**, then open it like any app.

For updates she gets a notification; she opens App Tester and taps **Update** (or **Download** on the
new version). Uploaded builds disappear from App Tester after 150 days, but an installed app keeps
working.

## Your own phone

Builds from Android Studio's ▶ Run button are signed with your *debug* key, so they can't install
over an App Distribution build (and vice versa). If Android Studio says the signatures don't match,
uninstall ChoomHub first. Once you're both on App Distribution, it's simplest to use the release
build for everyday use and Run only for quick testing.
