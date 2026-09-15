# History storage and cloud loading

The app retains measurements for a rolling 100 × 24 hours, regardless of whether
they arrived through BLE advertisements, GATT logs, or cloud history. Retention is
based on the measurement timestamp. App startup/resume and throttled ingestion
cleanup expire old rows; Bluetooth activity is not required.
Bulk GATT imports commit in pages of at most 5,000 readings, with timestamp-based
duplicate checks across page boundaries.

## Selected range

`HistorySelection` holds a rolling preset or custom calendar dates. `HistoryRange`
uses millisecond timestamps with an inclusive start and exclusive end. Calendar
dates are interpreted in the device timezone, including daylight-saving changes.
The end date includes the whole day, capped at now. The oldest day may be partial
because retention is a rolling duration.

All defaults to 100 days. Shortcuts remain saved globally; a custom calendar range
lasts for the current sensor-card session and survives rotation/sensor switching.
The selected range bounds graph axes and cloud downloads. The visible viewport
within that selection controls local queries, with a 100 ms debounce during
gestures and an immediate request at gesture end. Zooming only loads local detail.
Mini-chart popups and exports read local data only.

Full graphs and mini charts render at most 1,000 actual measurements per graph.
A streaming sampler preserves endpoints and time-bucket extrema, while computing
statistics from every raw measurement in the viewport. Gap segments are identified
before sampling; wide sample spacing does not create false outages. The old
"Show all collected measurements" setting no longer overrides the limit. Stored
measurements and exports retain their full resolution. Cached chart data is reused
until the viewport, measurement revision, units, offsets, or limits change; a live
overview also advances once per minute without rebuilding every second.

History loading uses the existing top-center cloud spinner, shared with account
refresh. Download errors remain retryable and subscription restrictions remain
visible.

## Cloud loading

Account refresh retrieves metadata and latest values but never sensor history.
The resumed full history view owns the history job for the selected pager sensor.
Changing selection, leaving history, or backgrounding cancels that job.

`NetworkHistoryInteractor` intersects the selection with the 100-day horizon and
the sensor subscription allowance, then sends bounded `since`/`until` requests in
ascending order with mixed resolution and a maximum of 5,000 results per page.
API timestamps use whole seconds. Each page commits its readings and successfully
checked interval together. Empty results also establish coverage; failures do not.
Repeated/non-advancing pages fail rather than spinning or claiming missing data.

Coverage is isolated by account, backend, and sensor. Opening/changing history
rechecks coverage older than 24 hours. An open live window refreshes once a minute,
including a one-minute overlap. Continuous live refresh does not re-fetch older
covered intervals. Clear history/sensor removal invalidate in-flight pages;
sign-out cancels history work and clears all coverage.

Schema migration 42 → 43 adds `HistoryCoverage`, `SensorSettings.cloudHistoryDays`,
and a timestamp index. Existing measurements remain; old sync timestamps are not
treated as proof of complete interval coverage.

## Verification

History tests cover calendar/DST boundaries, retention for every ingestion path,
SQLite queries and migration, transactional rollback, cache isolation,
pagination/cancellation, and calendar/lifecycle UI behavior. A 144,000-row fixture
checks sampling over 100 days at one-minute resolution.
Emulator visual checks use the same dense fixture in portrait, landscape,
a tablet-sized viewport, and with enlarged text. Live cloud access and physical
BLE/GATT hardware still require a device acceptance pass.

Run `./gradlew :app:testWithoutFileLogsDebugUnitTest` and both debug builds:
`./gradlew :app:assembleWithFileLogsDebug :app:assembleWithoutFileLogsDebug`.
On a JVM that prevents dynamic MockK agent attachment, start the test JVM with the
resolved `byte-buddy-agent` JAR using `JAVA_TOOL_OPTIONS=-javaagent:/path/to/jar`.

New UI strings use the English resources until translations are supplied. Sensor
firmware history capacity is independent of the app's 100-day local retention.
