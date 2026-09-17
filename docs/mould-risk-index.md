# Mould risk index

The optional `MOULD_INDEX` measurement describes current conditions at the sensor.
It is available in Visible measurements when both temperature and relative
humidity are present, for RuuviTag and Ruuvi Air. It is never added to the default
display order. Hiding the input measurements does not disable the index.

## Calculation and interpretation

`MouldRiskCalculator` accepts calibrated Celsius and relative humidity in percent
from the same reading. Input units selected for display do not affect the result.
The supported domain is `0 < T < 50` and `0 <= RH <= 100`. Missing, non-finite or
out-of-domain values return an explicit unavailable reason, not a score of zero.

For `T < 20`, the reference humidity is
`max(80, -0.00267*T^3 + 0.160*T^2 - 3.13*T + 100)`; otherwise it is 80.
The score is `clamp(50 + 2.5*(RH-referenceRH), 0, 100)`.
The reference curve is from [Ojanen et al. (2007), equation 1](https://bwk.kuleuven.be/bwf/projects/annex41/protected/data/VTT%20Oct%202007%20Paper%20A41-T4-Fin-07-1.pdf).
The 80% floor, normalization and categories are Ruuvi product choices, not a
validated probability scale. The model assumes conditions at the sensor; it
does not estimate colder wall-surface conditions.

Scores retain full precision in graphs. Text and categories use the same positive
half-up integer rounding:

| Displayed score | Category | Colour |
| --- | --- | --- |
| 0–9 | Very low | `#4BC8B9` |
| 10–24 | Low | `#96CC48` |
| 25–49 | Elevated | `#F7E13E` |
| 50–74 | High | `#F79C21` |
| 75–100 | Very high | `#ED5021` |

This indicator does not detect mould, assess health effects, or estimate
accumulated growth. A falling score does not mean existing mould has disappeared.
It must not be confused with the [Finnish mould growth model](https://research.tuni.fi/buildingphysics/finnish-mould-growth-model/),
which uses a 0–6 index, material characteristics and exposure history.

## Integration and compatibility

The live conversion and both graph paths share the calculator. Existing
calibration offsets are applied once before calculation. Historical scores are
derived on demand; there are no new database columns or backend measurement
fields. Graphs use a fixed 0–100 range and break at invalid readings and gaps
longer than one hour. No smoothing or collection period is used.

MPAndroidChart keeps the original double score alongside its float drawing
coordinate, so headings and markers round identically to live values even near
category boundaries. An invalid newest history reading makes the heading
unavailable instead of repeating an older valid score. Entirely unavailable
history uses the existing no-data state.

`EnvironmentValue.isAvailable` distinguishes unavailable presentation values from
numeric values. Its numeric placeholder is never classified or plotted.
The current value stays in the user's selected position when inputs disappear.

The stable `MOULD_INDEX` code uses existing display-order storage and cloud
settings synchronization. Older clients ignore unknown codes and may discard
this selection if the display order is edited there. Widgets, exports, alerts
and accumulated-growth modelling are intentionally not included.

Strings are in `mould.xml` in the six existing language resource directories so
the localization CI's regeneration of `strings.xml` preserves them. When these
keys move into `station.localization`, remove the matching local resources in
the same update to avoid duplicate names.

## Verification

Calculator tests cover the reference examples, temperature limits, invalid
inputs, rounding boundaries and monotonicity. Integration tests cover sensor
eligibility, opt-in defaults, unit independence, calibration, historical/live
agreement, unavailable values, code persistence and IAQ presentation. History
tests cover missing points, invalid inputs, long gaps and unrounded values.

Run:

```sh
./gradlew :app:testWithoutFileLogsDebugUnitTest :app:assembleWithoutFileLogsDebug :app:assembleWithFileLogsDebug
```

Implementation validation: 149 unit tests passed, and both debug variants built.
Synthetic RuuviTag and Ruuvi Air readings were checked on a temporary Pixel 8
emulator. Checks covered primary and secondary cards, existing IAQ presentation,
the popup, fixed graph limits and missing-data gaps, enabling/disabling,
drag-to-reorder, and persistence after restarting the app. The popup was also
checked in dark mode at 150% text size. Accessibility nodes expose the index
name, score and category.

The second review added regression coverage for both actual history-rendering
paths, sampled/all-point history, precise graph values, unavailable latest and
all-unavailable history, and the existing settings-sync upload payload. Emulator
checks verified identical live/header/marker rounding just below 49.5, the
unavailable popup and history states, and scrolling a 12-measurement legacy card
at 150% text size. Both card layouts share the same explanatory popup.

Main-app lint analysis ran; its error-level findings are in unchanged files.
The project still has existing lint findings, so this is not a clean full-project
lint result.

Authenticated cloud synchronization and spoken TalkBack navigation still need
an account/device check. The localized text should receive the usual translation
review when the keys are added to `station.localization`.
