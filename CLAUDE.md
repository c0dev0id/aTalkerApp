# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
./gradlew assembleDebug      # debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease    # release APK
./gradlew lint               # Android Lint
./gradlew test               # unit tests
./gradlew connectedAndroidTest  # instrumented tests (device/emulator required)
```

## Architecture

Phone overlay app for tablet/motorcycle use. No persistent main screen — everything is a system overlay drawn via `WindowManager.TYPE_APPLICATION_OVERLAY`.

### Services (always running)

| Class | Role |
|---|---|
| `OverlayService` | ForegroundService. Owns the overlay window, notification, bluetooth headset session. Entry point for showing the contacts list via notification action. |
| `PhoneService` | Extends `InCallService`. Receives all call events from Android Telecom (incoming, active, ended). Updates `CallManager`. |

### State

`CallManager` is a singleton `StateFlow<CallUiState>`. It is the single source of truth bridging `PhoneService` → `OverlayService` → UI. States: `Idle`, `ShowingContacts`, `Incoming`, `Active`.

### UI (Compose in overlay)

`OverlayWindow` adds a `ComposeView` to `WindowManager`. The window is **focusable** (no `FLAG_NOT_FOCUSABLE`) so it receives D-pad key events. `OverlayLifecycleOwner` provides the `LifecycleOwner`/`ViewModelStoreOwner`/`SavedStateRegistryOwner` that `ComposeView` requires outside of an Activity context.

`OverlayRoot` observes `CallManager.state` and switches between:
- `ContactsScreen` — D-pad navigable contact list; `navigateUp`/`navigateDown` in `ContactsScreen.kt` control selection wrapping behavior
- `IncomingCallScreen` — LEFT=reject, RIGHT=accept, CONFIRM=accept, BACK=reject
- `ActiveCallScreen` — CONFIRM or BACK ends the call; shows elapsed time

### Bluetooth headset

`BluetoothHeadsetManager` registers a `MediaSessionCompat` to capture `KEYCODE_HEADSETHOOK` / `KEYCODE_MEDIA_PLAY_PAUSE` from BT headsets. A single button press answers an incoming call or hangs up an active one.

### Setup

`PermissionsActivity` is the LAUNCHER activity. It guides the user through granting `SYSTEM_ALERT_WINDOW`, runtime permissions, and the default dialer role (`RoleManager.ROLE_DIALER`). Once the overlay permission is granted it starts `OverlayService` and the activity can be closed.

`DialerActivity` is a stub that exists only to satisfy Android's requirement that a default dialer candidate expose a `DIAL` intent handler.

## Key permissions

`SYSTEM_ALERT_WINDOW` (overlay), `ANSWER_PHONE_CALLS`, `CALL_PHONE`, `READ_CONTACTS`, `FOREGROUND_SERVICE_PHONE_CALL`, `BLUETOOTH_CONNECT`

## CI/CD

GitHub Actions: lint + debug build on push to main. Release via manual `release.yml` dispatch (auto-increments version, creates tag + GitHub release draft). Requires secrets: `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
