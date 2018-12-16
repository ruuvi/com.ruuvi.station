package com.ruuvi.station.util

class Constants {
    companion object {
        const val DEFAULT_SCAN_INTERVAL = 15 * 60
        const val RuuviV2and4_LAYOUT = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-21v"
        //const val RuuviV3_LAYOUT = "x,m:0-1=9904,m:2-2=03,i:3-15,d:3-3,d:4-4,d:5-5,d:6-7,d:8-9,d:10-11,d:12-13,d:14-15";
        //const val RuuviV5_LAYOUT = "x,m:0-1=9904,m:2-2=05,i:20-25,d:3-4,d:5-6,d:7-8,d:9-10,d:11-12,d:13-14,d:15-16,d:17-17,d:18-19,d:20-25";
        const val RuuviV3_LAYOUT = "x,m:0-2=990403,i:2-15,d:2-2,d:3-3,d:4-4,d:5-5,d:6-6,d:7-7,d:8-8,d:9-9,d:10-10,d:11-11,d:12-12,d:13-13,d:14-14,d:15-15"
        const val RuuviV5_LAYOUT = "x,m:0-2=990405,i:20-25,d:2-2,d:3-3,d:4-4,d:5-5,d:6-6,d:7-7,d:8-8,d:9-9,d:10-10,d:11-11,d:12-12,d:13-13,d:14-14,d:15-15,d:16-16,d:17-17,d:18-18,d:19-19,d:20-20,d:21-21,d:22-22,d:23-23,d:24-24,d:25-25"
        const val DATA_LOG_INTERVAL = 5
    }
}