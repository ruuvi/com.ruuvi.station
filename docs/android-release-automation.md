# Android Release Automation

This page is for maintainers who need to send an Android build to testers, move a tested build
through Google Play tracks, or update the Play Store listing.

The short version:

- Build the signed `withoutFileLogsRelease` AAB once and distribute it to QA through Firebase.
- Upload that exact Firebase-tested AAB to Google Play internal without rebuilding it.
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

Use `Deploy to Firebase [Ruuvi Station Android]` to create a release candidate for QA.

It runs tests, builds and signs the `withoutFileLogsRelease` AAB, and uploads that AAB to Firebase
App Distribution.

The workflow also stores the signed AAB for 30 days as the
`ruuvi-station-android-firebase-aab` GitHub Actions artifact. The artifact includes
`build-metadata.env`, which records its version, source commit, Firebase workflow run ID, and SHA-256
checksum. This artifact is the only release candidate that the Play Internal workflow accepts.

For convenience, the workflow also builds the signed `withoutFileLogsRelease` APK and stores it for
30 days as `ruuvi-station-android-firebase-apk`. This APK is useful for direct installation and
troubleshooting, but Firebase distributes the AAB and only the AAB can be promoted to Play Internal.

Manual run:

1. Open GitHub Actions.
2. Choose `Deploy to Firebase [Ruuvi Station Android]`.
3. Click `Run workflow`.
4. Pick the branch.
5. Add release notes if needed.
6. Run it.
7. After it succeeds, copy the numeric workflow run ID from the run URL. For example, the run ID in
   `https://github.com/ruuvi/com.ruuvi.station/actions/runs/123456789` is `123456789`.

QA should approve or reject this Firebase release before it enters Google Play.

### Google Play Internal Testing

Use `Upload to Google Play Internal` after QA approves a Firebase release candidate.

This workflow does not rebuild the application. It downloads
`ruuvi-station-android-firebase-aab` from the selected Firebase workflow run, validates the embedded
run ID and build metadata, recalculates the SHA-256 checksum, and uploads that exact AAB to the
internal testing track. It stores the same AAB on the Play Internal workflow run for traceability.

Manual run:

1. Open GitHub Actions.
2. Choose `Upload to Google Play Internal`.
3. Click `Run workflow`.
4. Pick the branch.
5. Enter the approved Firebase workflow run ID.
6. Add release notes if needed.
7. Leave `validation_only` disabled to upload to Play.
8. Run it.
9. Check Play Console and copy the uploaded build number. You will need that number for promotions.

To test only the artifact handoff, enable `validation_only`. The workflow will download and verify
the Firebase AAB without uploading it to Google Play.

Because Firebase artifacts are retained for 30 days, promote an approved candidate to Play Internal
before its artifact expires.

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
Firebase AAB -> Play internal -> alpha -> beta -> production
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
