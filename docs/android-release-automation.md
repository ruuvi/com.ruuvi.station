# Android Release Automation

This page is for maintainers who need to send an Android build to testers, move a tested build
through Google Play tracks, or update the Play Store listing.

The short version:

- Use Firebase when QA needs an installable APK quickly.
- Use Google Play internal when a candidate build should enter the Play Store release path.
- Promote the same tested build from internal to alpha, then beta, then production.
- Edit `fastlane/metadata/` when Play Store text or screenshots need to change.

Release credentials, signing material, Play Store access, Firebase access, and notifications are
configured privately in GitHub settings by repository maintainers. They are intentionally not listed
in this public repo. Normal local development does not need those values.

## Version and Build Numbers

There are two numbers to keep in mind:

```text
3.5.20 (612689859)
```

`3.5.20` is the visible app version. It comes from `versionName` in `app/build.gradle`.

`612689859` is the release build number. CI generates it automatically from time, so release builds
do not fight over the same Google Play `versionCode`.

Update `versionName` only when the public app version should change:

- patch release or hotfix: `3.5.20` -> `3.5.21`
- feature release: `3.5.20` -> `3.6.0`
- major product release: `3.5.20` -> `4.0.0`

For internal testing, multiple builds can share the same visible version. CI will still give each
upload a new build number.

## Which Workflow Should I Use?

### Firebase Tester Build

Use `Deploy to Firebase [Ruuvi Station Android]` when QA needs a signed APK from a branch.

It runs tests, builds the `withFileLogsRelease` APK, uploads it to Firebase App Distribution, stores
the APK as a downloadable Actions artifact, and sends a Telegram message after a successful upload
when notifications are configured.

Manual run:

1. Open GitHub Actions.
2. Choose `Deploy to Firebase [Ruuvi Station Android]`.
3. Click `Run workflow`.
4. Pick the branch.
5. Add release notes if needed.
6. Run it.

Use this for quick QA testing. Do not treat a Firebase upload as a Play Store release candidate unless
the same code will also go through the Play track.

### Google Play Internal Testing

Use `Upload to Google Play Internal` when a build is ready to start the Play Store testing path.

It runs tests, builds the signed release AAB, uploads it to the internal testing track, and also stores
a signed APK artifact for manual install testing.

Manual run:

1. Open GitHub Actions.
2. Choose `Upload to Google Play Internal`.
3. Click `Run workflow`.
4. Pick the branch.
5. Add release notes if needed.
6. Run it.
7. Check Play Console and copy the uploaded build number. You will need that number for promotions.

### Google Play Alpha

Use `Promote to Google Play Alpha` when the internal build is ready for the smaller tester group.

This does not build the app again. It moves the already-tested build number from internal testing to
alpha, so testers receive the exact same artifact.

Manual run:

1. Open Play Console and copy the build number from internal testing.
2. Open GitHub Actions.
3. Choose `Promote to Google Play Alpha`.
4. Enter the build number.
5. Keep the release status completed unless you intentionally want a draft.
6. Run it.

### Google Play Public Beta

Use `Promote to Google Play Public Beta` when the build is ready for the larger beta group.

Normally the source track is alpha. If the release intentionally skipped alpha, choose internal as the
source track.

Manual run:

1. Open Play Console and copy the tested build number.
2. Open GitHub Actions.
3. Choose `Promote to Google Play Public Beta`.
4. Enter the build number.
5. Choose the source track that currently contains the tested build.
6. Keep the release status completed unless you intentionally want a draft.
7. Run it.

### Production Rollout

Use `Submit Google Play Production` only after the same build number has already been tested.

The normal path is:

```text
internal -> alpha -> beta -> production
```

Alpha can be skipped when needed. Beta can also be skipped for small or urgent releases, but the
source track in the workflow must match where the tested build currently lives.

Manual run:

1. Open Play Console and copy the tested build number.
2. Open GitHub Actions.
3. Choose `Submit Google Play Production`.
4. Enter the tested build number.
5. Choose the source track that currently contains that build.
6. Keep staged rollout unless a full rollout is intentional.
7. Enter the confirmation value requested by the workflow.
8. Run it.

The default rollout is staged so the release can be monitored before it reaches everyone.

## Play Store Metadata

Play Store text, graphics, and screenshots live in:

```text
fastlane/metadata/
```

Each locale has its own folder:

```text
en-US
fi-FI
sv-SE
de-DE
fr-FR
pl-PL
ru-RU
```

Common files:

```text
title.txt
short_description.txt
full_description.txt
changelogs/default.txt
images/icon.png
images/featureGraphic.jpg
images/phoneScreenshots/
```

For text-only changes:

1. Edit the locale files under `fastlane/metadata/`.
2. Review the diff.
3. Run `Validate Google Play Metadata`.
4. If validation passes, run `Sync Google Play Metadata`.
5. Choose the text-only sync mode.

For screenshots or graphics, be more careful. Image sync can replace live Play Store assets for the
selected locales. Review the file diff and the images before running a screenshot, image, or full sync.

## Local Checks

Most release work should happen through GitHub Actions. Maintainers can still run metadata checks
locally when their machine has the private Play Store access configured:

```sh
bundle install
bundle exec fastlane android validate_play_store_metadata
bundle exec fastlane android download_play_store_metadata
```

If downloaded metadata changes files in the repo, review those diffs before committing.
