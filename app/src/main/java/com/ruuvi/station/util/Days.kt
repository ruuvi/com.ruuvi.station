package com.ruuvi.station.util

import com.ruuvi.station.R

sealed class Days(val value: Int, val stringResourceId: Int, val shouldPassValue: Boolean = false) {
    class Day1: Days(1, R.string.day_1)
    class Day2: Days(2, R.string.day_2)
    class Day3: Days(3, R.string.day_3)
    class Day4: Days(4, R.string.day_4)
    class Day5: Days(5, R.string.day_5)
    class Day6: Days(6, R.string.day_6)
    class Day7: Days(7, R.string.day_7)
    class Day8: Days(8, R.string.day_8)
    class Day9: Days(9, R.string.day_9)
    class Day10: Days(10, R.string.day_10)
    class DayX(value: Int): Days(value, R.string.day_x, true)

    companion object {
        fun getInstance(value: Int): Days =
            when (value) {
                1 -> Day1()
                2 -> Day2()
                3 -> Day3()
                4 -> Day4()
                5 -> Day5()
                6 -> Day6()
                7 -> Day7()
                8 -> Day8()
                9 -> Day9()
                10 -> Day10()
                else -> DayX(value)
            }
    }
}