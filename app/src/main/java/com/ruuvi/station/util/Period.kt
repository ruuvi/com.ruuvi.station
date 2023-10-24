package com.ruuvi.station.util

import com.ruuvi.station.R

sealed class Period(val value: Int, val stringResourceId: Int, val shouldPassValue: Boolean = false) {
    class All: Period(0, R.string.all)
    class Hour1: Period(1, R.string.hour_1)
    class Hour2: Period(2, R.string.hour_2)
    class Hour3: Period(3, R.string.hour_3)
    class Hour12: Period(12, R.string.hour_12)
    class Day1: Period(1 * 24, R.string.day_1)
    class Day2: Period(2 * 24, R.string.day_2)
    class Day3: Period(3 * 24, R.string.day_3)
    class Day4: Period(4 * 24, R.string.day_4)
    class Day5: Period(5 * 24, R.string.day_5)
    class Day6: Period(6 * 24, R.string.day_6)
    class Day7: Period(7 * 24, R.string.day_7)
    class Day8: Period(8 * 24, R.string.day_8)
    class Day9: Period(9 * 24, R.string.day_9)
    class Day10: Period(10 * 24, R.string.day_10)
    class HourX(value: Int): Period(value, R.string.hour_x, true)

    companion object {
        fun getInstance(value: Int): Period =
            when (value) {
                0 -> All()
                1 -> Hour1()
                2 -> Hour2()
                3 -> Hour3()
                12 -> Hour12()
                1 * 24 -> Day1()
                2 * 24 -> Day2()
                3 * 24 -> Day3()
                4 * 24 -> Day4()
                5 * 24 -> Day5()
                6 * 24 -> Day6()
                7 * 24 -> Day7()
                8 * 24 -> Day8()
                9 * 24 -> Day9()
                10 * 24 -> Day10()
                else -> HourX(value)
            }
    }
}