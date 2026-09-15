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
Database queries and graph axes use the same resolved window. Zooming does not
fetch beyond that window. Mini-chart popups and exports read local data only.

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
