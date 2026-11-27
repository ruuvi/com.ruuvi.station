# Progressive current-cloud history (Android)

Branch `feature/progressive-cloud-history` starts from master `b9955dad51cadbe323f398c2a2967a1c16ccfcb3`.
This ports the current iOS working implementation described in
`ruuvi_main/docs/architecture/CurrentCloudGraphHistory.md`, rather than merging the old Android longer-history branch.

## Enable and select

The last two developer switches are **Enable longer history** (off by default) and
**Use preset history durations** (on by default). The first applies only to cloud sensors with a positive owner history allowance.
An account with no cloud history keeps the existing local-history controls and export.
Presets match iOS: 1/2/3/6/12 hours, 1/2/3/10 days, 1/2 weeks, 1/3/6 months, and 1/2/3 years.
Weeks/months/years use 7/14, 30/90/180, and 365/730/1096 days. Disabling presets lists
every day through the sensor owner's subscription allowance. Both modes retain All
and a local-calendar date range picker. Shared Web Station week/month/year translations
are included for all six app languages, using their new Android identifiers.

Local sensors show at most ten days, including when switching from a cloud sensor
with a longer saved selection. Ten days replaces All as the default; explicit saved
selections remain intact. Free accounts retain their existing local Bluetooth history.

SQL records and persisted raw/prepared pages follow the sensor owner's retention:
ten days for local sensors and Free local history, and the server-provided allowance
for cloud sensors. Unknown cloud allowances skip deletion until known. This policy
is independent of the selected graph duration. Pruning runs off the UI thread;
SQL uses 5,000-row transactions with current metadata resolved inside each delete.
Metadata sync and graph access schedule cleanup; Bluetooth reception also schedules
SQL cleanup, at most hourly. Cache maintenance runs hourly or when policies change,
trimming partial pages and their coverage across saved sessions. The latest reading
is separate from the historical rows. Downloaded archives persist across navigation and app restart; sensor retention removes old data.

The app uses its selected current-cloud server (`network.ruuvi.com` or `testnet.ruuvi.com`), with no native backend or graph
aggregation endpoint dependency. As on iOS, GET must propagate database failures;
a backend that disguises failed reads as successful empty pages cannot support a
reliable complete-history result. Deploy the existing GET failure-propagation fix
before relying on completeness in that environment.

## Cloud download feedback

Cloud history updates the existing Sync control to **Downloading (X%)**, then
restores **Sync**. The toolbar is always 36 dp high, with a fixed remaining-width
sync slot and a trailing duration/menu group. Progress changes never insert a row
or move the chart bounds; labels shrink to fit on narrow screens. Bluetooth sync
retains priority, its point-count text and its cancel control. The cloud label is
not a new Bluetooth action while downloading. Retry errors and export cancellation
use dialogs so they also leave the graph layout unchanged. Shared localization
key `TagCharts.Sync.downloading` / `history_downloading_progress` supplies all six
languages. Synthetic Compose checks verify stable plot/duration bounds at 320 and
390 dp, complete percentage labels, return to Sync, Bluetooth priority and actions.

## Data and interaction

* One active `/get` request, with one-page prefetch overlapping decoding/cache writes: 1,500 maximum, descending, dense newest 24 hours and sparse
  older history, on a fixed split per stream. Short pages continue; only a range
  boundary or valid empty result ends a lane. Responses are checked before coverage advances.
* Six attempts at most for transient errors, with cancellable backoff, jitter,
  Retry-After and retained request spacing after throttling. Authorization and
  malformed responses are not retried.
* Persistent raw-page archives under Android noBackupFilesDir, indexed separately
  by session/account/server scope and sensor. Production no longer has a shared
  128 MiB eviction budget. Opening and sensor switching reuse downloaded coverage;
  only new or missing intervals are requested. Dense pages remain reusable after
  aging into the sparse period. Prepared calibrated values avoid repeated decoding.
  Downloading progress covers network gaps only, not local file reads or rebuilding.
* Decoding, disk I/O, local keyset reads and statistics run off the UI thread.
  Conflated display updates retain at most 400 actual points per metric per overview/detail,
  using endpoints/extrema and original gap identity. These are not server averages.
  Raw min/max and time-weighted averages update progressively, dimmed until coverage is complete.
* Unknown intervals have separate datasets. The shipped MPAndroidChart AAR remains
  unchanged; an adapter supplies original gap identities to its existing renderer.
  Line/fill/alert/marker/dot styling stays shared. A single reading uses the existing
  small isolated-point stem style. Renderer bitmap caches are released on dataset replacement.
* Gestures pause backfill. Settled X zoom/pan prioritizes the visible window after
  200 ms. A retained completed overview avoids re-fetching on maximum zoom-out;
  unchanged completed views and Y-only gestures do not trigger page requests.
  Range changes, navigation, backgrounding, account/server changes and lost access cancel obsolete work.
* Local records merge in descending 1,500-row `(createdAt,id)` keyset pages using the
  existing sensor/time index. Cloud wins equal whole-second timestamps. Migration 46
  adds owner history allowance and preserves both master and prior experimental databases.

## Export

CSV and XLSX traverse the entire selected range, not the zoomed view or 400 plotted
points. They download only missing or new intervals, merge local readings,
and apply calibration once. Both writers stream bounded pages. XLSX uses inline
strings and another worksheet after 1,048,575 data rows plus the repeated header.
A completed, closed file is required before sharing. Failure/cancellation removes the
partial file. Export is cancelled on navigation/background/range/access changes,
and the screen stays awake only while exporting in the foreground.

## Validation

The final `withoutFileLogsDebug` unit suite passed 825 tests, with zero failures and one opt-in live test skipped. The normal APK build also passed. Retention checks cover local/Free/owner/unknown policies, plan downgrades, cutoff boundaries, batches larger than 5,000 rows, cache trimming and saved defaults.
It includes wire pagination, short pages, cache reuse/corruption/isolation/eviction,
raw statistics, retries/cancellation, range switching, retained overview, local/cloud
ties, indexed keyset reads, additive migration, duration limits and DST date conversion.
Renderer tests compare 16 combinations of gaps/dots/alerts pixel for pixel with the
bundled renderer, plus gap metadata and single-reading visibility checks.
The streaming writer test writes 1,578,240 synthetic minute readings across two XLSX
worksheets; this is a writer stress test, not a measured live download of that size.

Synthetic emulator tests use an opt-in separate application ID and a plain test
Application, so they do not start account or Bluetooth services:

```sh
./gradlew :app:assembleWithoutFileLogsDebug :app:assembleWithoutFileLogsDebugAndroidTest \
  -PisolatedHistoryTest -PtestRunner=com.ruuvi.station.history.HistoryTestRunner
adb -s <emulator> install -r -t app/build/outputs/apk/withoutFileLogs/debug/app-withoutFileLogs-debug.apk
adb -s <emulator> install -r -t app/build/outputs/apk/androidTest/withoutFileLogs/debug/app-withoutFileLogs-debug-androidTest.apk
adb -s <emulator> shell am instrument -w -r \
  -e class com.ruuvi.station.history.HistoryGraphJourneyTest \
  com.ruuvi.station.historytest.test/com.ruuvi.station.history.HistoryTestRunner
```

Normal builds omit those properties. The dynamic FileProvider authority preserves
`com.ruuvi.station.fileprovider` for the shipping app and allows isolated testing.
Physical-device performance remains a separate acceptance check. MPAndroidChart
uses Float coordinates; markers use the original timestamp rather than the rounded
plot coordinate. No claim of one-second plot precision over a full three-year axis is made.

The explicit instrumentation runner reported **OK (1 test)** on the existing Android 15
Pixel Tablet emulator: 1,578,241 synthetic minute readings, 1,053 pages, 46.503 seconds
for the overview, 61.572 seconds for the full zoom/pan/return journey. This uses a local
synthetic page source and simplified decoding; it is not a server download benchmark
or a physical-device frame-rate measurement. At most 400 overview/800 combined
entries were plotted; an additional outward pinch at the full range made no request.

Lint completed but reports 13 existing errors in unchanged master files (permissions,
older Compose patterns and LiveData). No remaining error is in a changed history file.
The history controls reuse shared duration translations; four new history/export
messages have local translations in all six app languages pending localization review.

A deployed Test run is explicitly opt-in with instrumentation argument `liveHistory=true`.
Its `files/history-live-private.json` must be supplied privately to the isolated test app:
Test origin, synthetic sensor, fixture bounds and a separately authorized Test session.
It is never an asset or Git file. The test refuses other origins, validates the exact
eligible seeded timestamps, exercises the same gestures, checks cache-only reopening,
and streams the full raw range to CSV. Remove the private file afterward.


### Read-only Test verification, 2026-11-26

The actual Android transport, BLE decoder, page stream, disk cache and CSV export
passed a host-side Robolectric live test against the reserved Test environment's
synthetic fixture: **104,638 expected timestamps, 104,638 observed, no missing or
extra readings**. There were 75 HTTP attempts and three recovered HTTP 500s.
Cold loading/preparation/cache writes took 99.395 seconds, including 28.786 seconds
inside HTTP calls; retry backoff and retained pacing are included in the total.
Warm reading took 327 ms and complete CSV export 339 ms, with no additional HTTP.
This is the eligible sparse/dense API result, not 1.58 million raw minute readings.

The live emulator attempt did not complete reliably: the emulator had repeated
ADB/lifecycle stalls. Do not present the host-side live pass or the separate
synthetic gesture pass as a successful live emulator journey or device FPS test.
The test is opt-in via `RUUVI_ANDROID_HISTORY_LIVE_CONFIG` for `HistoryLiveTest`;
normal unit runs skip it. Temporary session copies and both isolated test packages
were removed, and the Test reservation was released. No backend changes were made.

### Download pipeline and live refresh, 2026-11-27

Ordinary transient errors use bounded retry backoff on the failed page only. Only
HTTP 429 retains pacing across later successful pages; Retry-After is still honored.
The next page starts while the current page is decoded, merged and saved. There is
one HTTP request at a time and at most one prefetched page; cancellation joins it.
Disposable cache writes retain atomic replacement/reopen validation without forcing
a storage flush for both the page and index on every request.

Automatic refresh uses a last-successful-window cursor and requests only times after
that cursor. It accepts already-cached dense readings even after they age beyond the
24-hour server split. New delta requests still select dense/sparse using the current
server time window. An evicted archive page cannot trigger a background redownload.
The completed plot stays visible while the delta is prepared. If raw cache eviction
prevents a full statistics reduction, bounded retained drawing samples preserve the
old graph alongside new tail readings; statistics remain dimmed and are
never computed from those drawing samples. User-selected uncached ranges and exports
may still download their missing raw pages. The navigation follow-up below supersedes
the earlier opening freshness and disposable-cache policies.

Statistics retain their last values dimmed while moving, then display dimmed raw
partial reductions as data arrives. Full cached coverage can show ready statistics
before network refresh finishes. Empty completed ranges clear old values; failed
updates keep values dimmed; sensor changes cannot reuse another sensor's values.
The one-line row has fixed height. No status words are appended; their temporary
iOS/Android identifiers were removed from both apps and shared localization.

Synthetic pipeline test: ten pages with 100 ms network + 100 ms processing per page
finish in 1,100 ms virtual time versus 2,000 ms serial. This is a controlled concurrency
test, not a production phone benchmark. Tests also cover throttle/transient retries,
cancelled prefetch, three incremental refreshes with a zero-byte cache, raw statistics
states, graph bounds, and previously existing pagination/rendering regressions.

Final checks for this change: 48 selected history/navigation tests, zero failures,
one opt-in live test skipped; the normal debug APK build and signature verification
passed. MockK tests on this Mac require `JAVA_TOOL_OPTIONS=-Djdk.attach.allowAttachSelf=true`.
Lint reports 12 errors, all in files unchanged from this branch's HEAD; it reports
no error in the history changes. No phone or production API was used for this run.

### Navigation persistence follow-up, 2026-11-27

The v4 archive imports existing v3 files and their coverage on first access, retaining
its old copy so an interrupted migration can resume. Already-evicted pages cannot be
recovered without downloading those gaps once. No measurement database or login is
reset. Pages retain at most 1,500 readings, and small touching delta pages compact
without creating coverage across gaps. New page files and the index commit before
replaced files are removed. An unreadable index leaves the archive intact; a missing
or corrupt page causes only its own interval to be fetched again.

Eight sensor indexes stay in memory; leaving that metadata cache never evicts disk
history. Retention still follows the local/Free ten-day policy or owner subscription.
Files are backup-excluded and session/server scoped. Ordinary graph opening and export
no longer revalidate an arbitrary recent 48-hour window. Export follows updated file
references when its own gap writes compact an adjacent cached page.

Synthetic controller tests use two three-year histories with 105,217 readings per
sensor. A/B/A navigation and a fresh store/controller instance make zero history HTTP
requests. Advancing two days downloads only that new interval, and subsequent export
uses the complete stored range without HTTP. The initial host run rebuilt A in 85 ms;
this is synthetic host timing, not a phone or production-network benchmark. Further
checks cover quiet cache-only progress, twelve independent sensor indexes, isolated
missing-page repair, compaction, migration, and damaged-index preservation.

Final follow-up validation: 56 selected history/navigation/control tests, 55 passed
and one opt-in live test skipped; debug APK assembly passed. The broader MockK
run needed the existing Byte Buddy agent preloaded with a temporary Gradle init
script because dynamic attachment timed out on this Mac. This changes only the
test invocation, not the project. Lint completed and reports 13 existing errors,
all in files byte-identical to HEAD; no error is in a changed history file.
No phone, private payload or live API was used for this follow-up.

### Immediate graph preview, 2026-11-27

Returning to a previously prepared sensor restores its plotted points and statistics
from a bounded memory cache before raw-page work begins. A small saved preview also
provides the first plot after process restart. Existing installations prepare a
preview on the first completed opening of each sensor/range; no history reset or
redownload is needed to create it. Full raw readings remain in the persistent archive.

Previews keep the existing sampled points, timestamps and gap segments. They are
never inputs to export, exact statistics, zoom-detail reduction, coverage checks or
HTTP cursor decisions. Local replay and missing-tail download refresh the plot in
the background; intermediate reductions cannot replace the complete preview. The
previous statistics stay visible and dimmed until the fresh reduction completes.
No new progress row, status text or chart style is introduced.

The preview identity includes sensor, session/server scope, calibration, metrics and
selected duration or absolute range. Rolling previews clip their drawing points to
the current window. Retention removes expired points/coverage and clears statistics
that included them, retaining the valid drawing instead of forcing a blank reload.
Explicit history removal invalidates the matching preview. Bad or missing previews
fall back to raw history. At most 16 previews remain in memory, with 64 disk files
under a 32 MiB budget and a 2 MiB per-file limit; those limits never evict raw history.

Synthetic tests block raw/local-page processing and assert that the saved drawing
is already available, remains unchanged while blocked, then receives exact updated
statistics. They cover 10 days at one-minute spacing (14,401 readings), three years
at fifteen-minute spacing (105,217 readings), A/B/A navigation, restart, account and
calibration isolation, corruption, bounded preview eviction and retention. Measured
first-plotted-state timings exclude chart rendering and are not device benchmarks.

On Android, the first screen setup uses the already-known owner allowance. The
ordinary periodic loop rechecks it afterward; an unknown allowance still needs
verification before cloud history is enabled. Local authorization is checked before
every page. Dew-point previews and prepared values include the temperature unit
preference in their revision key.

Validation for this follow-up: 60 selected history/navigation/UI tests, 59 passed
and one opt-in live test skipped; the debug APK build passed. The final synthetic
run supplied the ten-day preview in 0.20 ms from memory / 3.16 ms from disk and the
three-year preview in 0.12 ms / 1.68 ms. These timings stop at controller data, not
screen paint. The lint engine stalled once; a fresh-process rerun completed and
reported 13 existing errors, all in files byte-identical to HEAD. No private phone
data or production API was accessed.


### Physical-phone navigation layout follow-up, 2026-11-27

Tested the real History screen using its previous/next arrows on a connected
Galaxy S25, with the instant-preview build installed in place and existing login
and downloaded history preserved. Four recorded alternating arrow presses between
two sensors reproduced a miniature grid below Sync. Scoped layout logs contained
five zero-height chart compositions, between normal approximately 194–202 dp slots.
Private screen recording was kept locally for analysis and deleted; no sensor
identifiers, readings or account data are included here.

The chart-slot count was previously updated in a LaunchedEffect. After an empty
sensor state, the next nonempty list could render for a frame with zero slots and
zero height. ChartsView now derives the slot count synchronously and uses its
current BoxWithConstraints height, removing the parent measurement/state round trip.
The progressive flow ignores states belonging to another sensor instead of clearing
the current sensor's containers during navigation. Existing chart-size rules,
rendering style and history APIs are unchanged.

The new Robolectric test lays out real LineChart views through empty/three-chart/
empty/five-chart/empty/three-chart transitions and asserts no attached chart has a
zero or miniature height. Validation: the existing 60 selected history/navigation
checks had 59 passes and one opt-in live check skipped; the added layout test passed
separately after correcting its test configuration. APK assembly and diff checks
passed. Build 20261129 was then installed in place on the same phone. Post-fix checks
included four alternating arrow presses at two years, four at one week and eight
rapid alternating presses at one week. All 64 observed layout samples had normal
height (minimum approximately 194 dp), with no zero-height composition. All four
sampled one-week visits displayed Sync; the two-year and rapid runs also ended at
Sync. The process stayed alive during the rapid sequence. These UI observations do
not count HTTP requests or prove that no incremental tail refresh occurred.

A local-only recording of the four two-year transitions showed approximately
107–192 ms without plotted lines while the new chart views were attached, with no
miniature-grid collapse. Those are video-frame intervals, not precise tap-to-render
measurements. The scoped Android frame statistics for that recorded debug run had
52 samples, median 17.2 ms, p95 53.8 ms, maximum 59.6 ms and none over 100 ms; this is
not a release-build frame-rate guarantee. The recording was deleted after analysis.
No private sensor payload, name, address or session was retained in the report.


### Ten-day preset and empty periods, 2026-11-27

The preset menu includes **10 days**, between one and two weeks, wherever the sensor
allowance permits it. Ten days remains the default for an unset preference; explicit
saved selections are preserved. Daily mode still includes the same ten-day option.

A successfully completed selected period with no displayable readings uses the
existing shared `empty_chart_message` translation instead of blank graphs and empty
statistics. Loading, failed/incomplete results and a zoomed detail window do not
replace the interactive chart with this full-screen empty state. Populated periods
restore the chart normally. No localization strings were added.
Validation: seven focused Compose/menu/layout tests passed, including completed
empty, loading, failed, incomplete, zoomed and populated states in both portrait
and landscape. APK build 20261130 was installed in place on the authorized phone,
preserving login and history. Empty-result checks use synthetic data.


### Statistics before the first readings, 2026-11-27

During the initial history load, a statistics row with no current or retained values
reserves its normal height but draws no placeholder text and exposes no placeholder
to accessibility. The first available statistics appear dimmed; completed statistics
use the normal styling. Previously available values remain visible and dimmed during
updates. The completed-empty-period message remains unchanged. No loading words or
new localization strings are introduced.
Six focused statistics/toolbar/empty-state Compose tests passed, including row
bounds and absence of placeholder text from the accessibility tree. The APK built
successfully; build 20261131 was installed in place with account/history preserved.
