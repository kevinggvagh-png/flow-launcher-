# Glass Launcher — Android prototype

[Download GlassLauncher.apk](https://github.com/kevinggvagh-png/flow-launcher-/releases)

Built and checked by GitHub Actions (`assembleDebug lintDebug`). Open the downloaded APK on your Android tablet to install it, then choose Settings > Set as home screen inside Glass Launcher.

Created for Kevin's OnePlus Pad, based on the supplied Niagara layout.

## Version 0.3 changes
- Curved alphabet browsing from either edge.
- Eight saved accent colours and a saved Tinted Glass switch in Settings.
- Stronger translucent glass gradients, double highlights and circular app icon masks.
- Full-screen watch menus with visible straps, metal case, bezel and circular swipeable option dials for settings, accent selection and long-press app actions; Previous/Next/Choose buttons also available.
- Glass is a stylized translucent effect, not live background blur or optical refraction.

## Included
- Actual Android home activity and installed-app launching.
- Two app columns on tablets; one on phones.
- Translucent gradient panels with glass-like highlights, peach/ice/lavender accent.
- Search, alphabet filters, saved favorites (hold an app and choose Pin/Unpin).
- Default-home selection through Android's home role.
- Optional dim bedside clock with moving time text.
- Offline; no ads, account, accessibility service, or network permission.

## Status
Compiled debug prototype; not yet tested on a physical tablet. The APK is available from the download link above. Debug builds may use different signing keys across runs; a later build may require uninstalling the previous test version, which clears its preferences. The glass effect uses translucent gradients, not optical refraction or live wallpaper blur. The background is an original procedural gradient, not the wallpaper visible in the reference screenshot.

## Build on a computer
1. Open this folder in Android Studio with JDK 17 and Android SDK 35.
2. Let Gradle sync. The project uses Android Gradle Plugin 8.7.3 and Gradle 8.9. If asked for a Gradle distribution, select Gradle 8.9. Alternatively install Gradle 8.9 and run: gradle wrapper --gradle-version 8.9
3. Run: gradle assembleDebug lintDebug (or use Android Studio's Build APK action).
4. The APK is at app/build/outputs/apk/debug/app-debug.apk.

## Build with GitHub
Put the contents of this folder at the root of a repository you own. The included Actions workflow builds and lints the app, then provides a GlassLauncher-APK artifact. Download and unzip that artifact to get the APK. The workflow has successfully built, linted and published build-3. Successful pushes publish a new prerelease APK.

## Install
Transfer the APK to your tablet, open it, and allow installation from that source if Android asks. Open Glass Launcher > Settings > Set as home screen. Android asks you to choose the home app. This does not change your assistant. To revert, choose Niagara or the original launcher in Android Settings > Default apps > Home app.

## Bedside clock
This keeps a dim screen awake only while this app is in the foreground. It is not a true hardware always-on display, does not bypass your lock screen, and consumes battery. Tap anywhere or press Back to return. Leaving the app also exits the bedside mode.

## Device validation still required
- Compilation and Android lint passed for build-3; physical-device checks below remain outstanding.
- Launch apps, pin/unpin favorites, restart and verify persistence.
- Rotate between portrait and landscape; test search with keyboard.
- Verify default-home switching and recovery to the previous home app.
- Verify bedside clock restores brightness on exit, Home, and app switching.

## Scope
Personal-profile apps only. No widgets, notification badges, work-profile support, custom wallpaper picker, or system lock-screen integration in this first version.

Android API references:
- https://developer.android.com/reference/android/app/role/RoleManager
- https://developer.android.com/training/package-visibility/declaring
