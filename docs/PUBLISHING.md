# Maintaining releases

Repository: [M1ora/RoyalCalendar](https://github.com/M1ora/RoyalCalendar).

## Pull requests

The Android build workflow checks debug and release builds and Android Lint.
Pull requests use a small description and validation template. Mandatory
approvals and branch-protection rules are not changed by the project scripts.
They can be enabled later in GitHub settings if the maintainer wants reviews
to be required before merging.

## Build and sign a release

1. Update `versionCode` and `versionName` in `app/build.gradle`.
2. Run the builds, Android Lint and connected-device checks from [DEVELOPMENT.md](DEVELOPMENT.md#checks).
3. Build the unsigned release APK with `./gradlew assembleRelease`.
4. Sign it with the owner's private release key. On the original Windows
   workstation, `./scripts/sign-release.ps1 -Version 1.0.0` reads the private
   files in `.signing/` and produces the APK plus a SHA-256 file under `output/`.
5. Create a GitHub release at the intended commit/tag and attach the signed
   `RoyalCalendar-vVERSION.apk` and its `.sha256` file.

The local signing directory is ignored by Git. It contains
`royal-calendar-release.jks` and `store-password.txt`. Keep a private backup:
the same signing key is needed for future APKs to update existing installs.
Do not upload these files to GitHub, issue reports or release attachments.

A clone of the source does not contain the release key. Contributors can build
debug APKs for development or sign release builds with their own key.
GitHub Actions uploads debug and **unsigned** release artifacts; the unsigned
APK cannot be installed until signed. APKs attached to public project releases
are signed separately with the stable private release key.

## Development installs

The owner's current local prototype is signed with the same private key as
public releases, so a signed release can update it without removing app data.
Ordinary `assembleDebug` builds use Android's debug key and cannot update the
public release in place. Keep the private key and passwords out of Git and
release attachments. Never uninstall the owner's app as part of publishing.

## Source and attribution

APK files belong in Releases rather than Git history. SDKs, JDKs, caches,
downloaded research files and private phone screenshots are excluded by
`.gitignore`. The preview in the README is rendered artwork, not a private
phone screenshot.

Code uses [MIT](../LICENSE). The separate artwork notices are in
[ASSETS.md](../ASSETS.md).
