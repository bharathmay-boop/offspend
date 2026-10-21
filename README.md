# SMS Expense Tracker (working name)

An Android app that reads bank and UPI transaction SMS on your phone and asks you to confirm the category with one tap. No typing, no bank login, and your spending data never leaves your phone.

## How it works

1. A bank SMS arrives.
2. The app reads the amount, currency and merchant on the phone.
3. A notification shows the amount and a guessed category.
4. One tap saves it, or you change the category or add a note.

Cash and anything else without an SMS can be added by hand with the Add button on the home screen.

## Status

Early build. Android first, tested in India. iOS later through a Shortcuts automation, since iOS apps cannot read SMS. Work is tracked on the GitHub Projects board.

## Build and run

You need JDK 21 and the Android SDK (platform 37, plus an Android 16 system image for the emulator).

```
./gradlew test            # all unit tests, including the 54-message parser test set
./gradlew assembleDebug   # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug
```

For the Play build, `./gradlew bundleRelease` writes `app/build/outputs/bundle/release/app-release.aab`, shrunk with R8 and signed with the upload key. The key is never in the repo: see "Release build" in [docs/PLAY_RELEASE.md](docs/PLAY_RELEASE.md).

Point Gradle at the SDK with a `local.properties` file containing `sdk.dir=<path to your SDK>`.

To try it on an emulator, install the debug APK, then send a fake bank SMS from the emulator console:

```
adb emu sms send AD-HDFCBK "Sent Rs.320.00 From HDFC Bank A/C *1234 To CALIFORNIA BURRITO On 08/10/26 Ref 412398765012"
```

## Privacy

Spending data stays on the device. Any product stats are opt-in and contain no amounts, merchants or message text.

## Design
Every screen follows the design system in [DESIGN.md](DESIGN.md).
