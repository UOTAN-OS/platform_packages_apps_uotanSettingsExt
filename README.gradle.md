# Gradle development build

The Soong build remains the source of truth for system images. The Gradle build reuses the
production sources and includes `uwu-sdk/uwuCompose` as the `:uwu-compose` source module for
Android Studio indexing and Compose previews.

Before the first Gradle sync, build the matching platform artifacts and copy their compile-time
stubs:

```bash
m framework-minus-apex SettingsLib SettingsLibCollapsingToolbarBaseActivity
./pull-system-libs.sh
```

Then open this directory in Android Studio or build the development APK:

```bash
./gradlew :app:assembleDebug
```

The script also packages the SettingsTheme implementation needed by the standalone debug app at
runtime. The debug application ID is `org.uwuaosp.settingsext.dev`. It is not a privileged app and cannot
exercise features that require the platform certificate or `WRITE_SECURE_SETTINGS`. The Gradle
build consumes SettingsLib theme resources directly from the surrounding AOSP checkout, so this
directory must remain at `packages/apps/uwuSettingsExt`.
