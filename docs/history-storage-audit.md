# Longer history: download and storage audit

Audit date: 2026-09-16. Baseline: `d1af058a`, covering the longer-history,
viewport-sampling, and graph-crash changes since `ed019e2a`.

## Assessment

The app does not automatically download historical records for all account sensors.
It downloads history for the resumed full history screen's settled sensor and the
selected date range. At 15-minute resolution, 50 fully cached sensors over 100 days
are a reasonable indexed SQLite workload, with about 81–99 MiB of history storage
in the synthetic measurements below. Three years would approach 0.9–1.1 GiB before
denser data, transaction journals, and other app storage. Raising the retention
constant alone is therefore not the recommended next step.

The 400-point cap controls chart rendering. It does not cap stored measurements,
the rows inspected for statistics, or export memory.

## Download and persistence paths

| Path | Historical requests | Local persistence |
| --- | --- | --- |
| Login, dashboard, automatic account refresh | None; one account-wide latest-value/metadata request remains | Latest sensor entity updated. For subscriptions with zero cloud history, the existing behavior also saves fresh latest readings locally. |
| Full history screen | Only the settled sensor; selected dates intersected with retention and subscription allowance | Completed pages, up to 5,000 measurements each; data and coverage commit together. |
| Composed neighboring sensor pages | None | No history prefetch. |
| Zoom and pan | None | Read and resample the selected sensor's local visible range. |
| Mini-chart popup | None | Read local history only. |
| CSV/XLSX export | None | Read locally stored history only. |
| Background, history closed, sensor changed, sign-out | Active history job cancelled | Previously committed pages remain, subject to retention/deletion. Sign-out clears coverage and removes network sensors and their history. |
| BLE and GATT | No cloud request | Shared timestamp-based 100-day retention; full accepted readings retained. |

Every historical request has `since`, `until`, ascending sort, and a 5,000-result
limit. Pagination checks sensor identity and advancing timestamps, and the storage
boundary accepts only records inside the page's interval. Empty responses establish
coverage too. Coverage is scoped to sensor, account, and backend. Clearing history
invalidates pages already in flight.

Opening a sensor with **All** selected requests up to the full 100-day allowance.
Opening it with **Last week** selected requests only that week. Changing to a wider
selection requests missing portions, subject to the freshness policy below. Closing
the screen does not discard downloaded readings: they remain available offline and
for export until the rolling retention cutoff. Merely owning 50 sensors does not
populate 50 complete histories; opening all 50 with All selected can do so.

### Resolution and freshness still matter

The app requests `mixed` resolution. Ruuvi documents this as available dense data
plus sparse older data. Consequently, 15-minute intervals are a capacity scenario,
not an upper bound guaranteed by the current request mode.
[Ruuvi User API](https://docs.ruuvi.com/communicate-with-ruuvi-cloud/cloud/user-api#get-sensor-data)

The accepted policy revalidates selected coverage older than 24 hours when the
history screen opens or the selection changes. This can deliberately download the
whole selected historical window again after a day, although duplicate readings
are not inserted again. An already-open live view checks once per minute, with a
one-minute overlap, without expiring its older coverage during that session.

Historical revalidation is an explicit freshness tradeoff: it catches late uploads
in old windows. It should be redesigned before three-year selections are enabled,
rather than silently removed. Cache old completed ranges until manual refresh or a
server invalidation/version signal, and automatically recheck a defined recent
arrival window. A manual refresh must be distinct from retrying a failed page.

## Findings corrected in this audit

1. **Retry downloaded completed pages again.** The screen's retry path previously
   bypassed all coverage. It now uses committed fresh coverage and resumes missing
   portions. A screen-to-network regression test reproduces the previous behavior.
2. **Revalidation accumulated obsolete coverage records.** Fresh writes previously
   merged only fresh rows, leaving overlapping stale rows behind. They now replace
   superseded coverage and split only unchecked remainders, preserving their age.
3. **Duplicate-only pages caused unnecessary graph scans.** Data revisions now
   advance only when bulk/cloud writes actually insert measurements.
4. **Duplicate checking could allocate a large history list.** A 5,000-point sparse
   page can span roughly 52 days of much denser BLE data. Existing timestamps are
   now streamed through the sensor/time index instead of loaded as models and
   boxed timestamps. This bounds memory; scan time still depends on overlap size.
5. **Persistence relied solely on the network layer's range filter.** The repository
   now also enforces the successfully fetched page interval before inserting data.

These changes preserve the 100-day horizon, subscription behavior, 24-hour
freshness policy, and full-resolution local measurements. No schema migration is
required. The documentation's obsolete 1,000-point reference is corrected to 400.

## Measured storage and query scope

Synthetic desktop SQLite databases, 4 KiB pages, production `TagSensorReading`
schema, both `TagId(sensor, timestamp)` and `HistoryTimestamp(timestamp)` indexes,
50 sensors, and exactly one reading every 15 minutes:

| History per sensor | Rows per sensor | Total rows | Tag-like fields | Air-like fields |
| --- | ---: | ---: | ---: | ---: |
| 7 days | 672 | 33,600 | 5.59 MiB | 6.80 MiB |
| 100 days | 9,600 | 480,000 | 81.08 MiB | 98.58 MiB |
| 1,095 days | 105,120 | 5,256,000 | 894.96 MiB | 1,092.64 MiB |

Three years is approximated here as 3 × 365 days. Leap years add another day's rows.
All measurement types share a reading row; there is not a separate row per chart.
The fixtures include realistic populated columns and fractional values, but they
are estimates, not a size guarantee. Coverage/metadata, fragmentation, journals,
images and exports are additional. Dense recent cloud records remain dense locally
until expiry; the app does not compact them when the backend later downsamples them.

The query planner selected `TagId` for every bounded sensor query. On this Mac,
iterating all columns for one sensor took about 0.6 ms for seven days, 8–9 ms for
100 days, and 92–99 ms for three years. The short-window timings stayed essentially
unchanged with 5.26 million total rows. These are desktop SQLite scans, not Android
graph-load timings: DBFlow models, unit conversion, statistics, allocation, and
chart rendering add work.

Reproduce after generating the debug DBFlow adapter:

```sh
python3 scripts/history_storage_benchmark.py --days 7 100 1095
```

The script creates synthetic temporary databases and deletes them afterward. It
never reads user sensor data or contacts the cloud.

## Work required before three years

1. **Separate cloud availability from device retention.** Introduce a cloud cache
   budget, range last-access metadata, and eviction of least recently viewed cloud
   ranges. Protect the active selection. Evict associated coverage atomically so
   missing readings can be downloaded again. Keep BLE/GATT retention a separate,
   explicit policy; they may have no recoverable cloud copy. The current shared
   table has no source provenance, so safe eviction needs a migration and a
   conservative treatment of existing unclassified rows.
2. **Choose long-range cloud resolution explicitly.** Prefer sparse requests for
   broad overview ranges and bounded dense detail when requested. Include resolution
   in coverage keys; sparse coverage must not suppress later dense requests. Decide
   whether zoom may trigger detail downloads, because the current contract makes
   zoom strictly local. Do not drop dense local data without an agreed policy.
3. **Replace whole-window daily revalidation.** Use recent-data refresh plus explicit
   old-range refresh or server change tracking, as described above. Repeatedly
   re-downloading three years would waste bandwidth even with perfect deduplication.
4. **Add stored graph summaries.** The current sampler scans every visible raw row
   for min/max/average and gap detection. Store hierarchical time-bucket summaries
   with extrema, endpoints, counts, gap information and statistics sufficient to
   preserve current time-weighted averages. Query raw readings for narrow windows.
   Invalidate/rebuild summaries when measurements or calibration change.
5. **Stream exports.** Export preparation currently materializes raw readings,
   calibrated copies, and output rows. Move large CSV/XLSX export processing to
   bounded batches before supporting dense multi-year histories.
6. **Bound maintenance work and disk use.** Retention is age-based, not a byte/row
   budget. Bulk expiry currently holds the shared history lock for one transaction.
   Use bounded cleanup transactions and measure lock duration on Android. Deleted
   SQLite pages can be reused without shrinking the file; plan controlled space
   reclamation separately rather than running a full VACUUM on every foreground
   entry. [SQLite auto-vacuum documentation](https://www.sqlite.org/pragma.html#pragma_auto_vacuum)
7. **Check long-axis precision and identifiers.** Range math already uses `Long`, but
   rendered X coordinates use `Float` relative to the selection start. At three
   years, precision near the end is about eight seconds, unsuitable for fine BLE
   zoom. Use a viewport-relative plotting origin and longer axis tick intervals.
   Review the reading model's 32-bit ID for very dense long-lived databases. Update
   calendar limits, shortcuts, labels and boundary/DST tests together.
8. **Set a device acceptance budget.** Measure cold/warm overview, zoom, cancellation,
   ingestion, cleanup, export peak memory, and disk use with 50 sensors on a slower
   Android device. Include dense BLE and mixed cloud histories, not only 15-minute
   fixtures. The desktop benchmark establishes scale and index use only.

## Validation

Five new regression tests failed against the audit baseline and passed after the
fixes. Coverage includes retry from the actual history view model, repeated and
partial revalidation, duplicate-only writes, and page-range enforcement. Existing
tests additionally exercise a 144,000-row local range with sparse cloud overlap,
account refresh with 50 sensors, local-only zoom/mini charts, lifecycle cancellation,
subscription bounds, rollback, deletion, retention and database migration.

Final validation: all 189 unit/database/Robolectric UI tests pass; both
`assembleWithoutFileLogsDebug` and `assembleWithFileLogsDebug` succeed. No live
account history was downloaded during this audit. The three-year scenario was
tested with synthetic desktop databases, not enabled in the app.
