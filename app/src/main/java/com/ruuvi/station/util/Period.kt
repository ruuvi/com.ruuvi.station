package com.ruuvi.station.util

import com.ruuvi.station.R

sealed class Period(val value: Int, val stringResourceId: Int, val shouldPassValue: Boolean = false) {
    data object All: Period(0, R.string.all)
    data object Hour1: Period(1, R.string.hour_1)
    data object Hour2: Period(2, R.string.hour_2)
    data object Hour3: Period(3, R.string.hour_3)
    data object Hour6: Period(6, R.string.hour_6)
    data object Hour12: Period(12, R.string.hour_12)
    data object Day1: Period(1 * 24, R.string.day_1)
    data object Day2: Period(2 * 24, R.string.day_2)
    data object Day3: Period(3 * 24, R.string.day_3)
    data object Day4: Period(4 * 24, R.string.day_4)
    data object Day5: Period(5 * 24, R.string.day_5)
    data object Day6: Period(6 * 24, R.string.day_6)
    data object Day7: Period(7 * 24, R.string.day_7)
    data object Day8: Period(8 * 24, R.string.day_8)
    data object Day9: Period(9 * 24, R.string.day_9)
    data object Day10: Period(10 * 24, R.string.day_10)
    class HourX(value: Int): Period(value, R.string.hour_x, true)

    companion object {
        fun getInstance(value: Int): Period =
            when (value) {
                0 -> All
                1 -> Hour1
                2 -> Hour2
                3 -> Hour3
                6 -> Hour6
                12 -> Hour12
                1 * 24 -> Day1
                2 * 24 -> Day2
                3 * 24 -> Day3
                4 * 24 -> Day4
                5 * 24 -> Day5
                6 * 24 -> Day6
                7 * 24 -> Day7
                8 * 24 -> Day8
                9 * 24 -> Day9
                10 * 24 -> Day10
                else -> HourX(value)
            }
    }
}