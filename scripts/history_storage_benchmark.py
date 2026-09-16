#!/usr/bin/env python3
"""Synthetic SQLite storage/query benchmark; never opens the app's database.

Run after a debug build has generated the DBFlow table adapter. Results measure
desktop SQLite, not Android rendering, Kotlin conversion, network, or export cost.
Temporary databases are removed on exit. Sizes include both production indexes.
"""
import argparse
import json
from pathlib import Path
import re
import sqlite3
import statistics
import tempfile
import time


def benchmark(schema, days, sensors, profile):
    with tempfile.TemporaryDirectory(prefix="ruuvi-history-bench-") as directory:
        path = Path(directory) / "history.db"
        db = sqlite3.connect(path)
        db.execute(schema)
        db.execute("CREATE INDEX TagId ON TagSensorReading(ruuviTagId, createdAt)")
        db.execute("CREATE INDEX HistoryTimestamp ON TagSensorReading(createdAt)")
        now = 1_800_000_000_000
        count = days * 96
        columns = ["ruuviTagId", "createdAt", "temperature", "temperatureOffset",
                   "humidity", "humidityOffset", "pressure", "pressureOffset", "rssi",
                   "voltage", "dataFormat", "txPower", "movementCounter", "measurementSequenceNumber"]
        values = ["?", f"{now - days * 86400000} + n * 900000", "20.1 + (n % 31) / 100.0", "0",
                  "45.1 + (n % 17) / 100.0", "0", "101325.1 + n % 100", "0", "-60", "2.991", "5", "4", "n % 255", "n % 65536"]
        if profile == "tag":
            columns += ["accelX", "accelY", "accelZ"]
            values += ["0.012", "0.024", "0.998"]
        else:
            columns += ["pm1", "pm25", "pm4", "pm10", "co2", "voc", "nox", "luminosity", "dBaAvg", "dBaPeak"]
            values += ["1.1", "2.2", "3.3", "4.4", "500 + n % 100", "80 + n % 20", "1", "123.4", "45.6", "60.7"]
            values[columns.index("dataFormat")] = "225"
        insert = (f"WITH RECURSIVE ticks(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM ticks WHERE n < {count-1}) "
                  f"INSERT INTO TagSensorReading({','.join(columns)}) SELECT {','.join(values)} FROM ticks")
        for sensor in range(sensors):
            db.execute(insert, (f"AA:BB:CC:DD:EE:{sensor:02X}",))
            db.commit()
        query = "SELECT * FROM TagSensorReading WHERE ruuviTagId = ? AND createdAt >= ? AND createdAt < ? ORDER BY createdAt"
        scans = {}
        for window in sorted({7, min(100, days), days}):
            args = ("AA:BB:CC:DD:EE:00", now - window * 86400000, now)
            times = []
            for _ in range(3):
                start = time.perf_counter()
                rows = sum(1 for _ in db.execute(query, args))
                times.append((time.perf_counter() - start) * 1000)
            scans[str(window)] = {"rows": rows, "median_ms": round(statistics.median(times), 2)}
        result = {"profile": profile, "days": days, "sensors": sensors, "rows": count * sensors,
                  "bytes": path.stat().st_size, "MiB": round(path.stat().st_size / 1048576, 2),
                  "single_sensor_scans": scans,
                  "query_plan": [r[3] for r in db.execute("EXPLAIN QUERY PLAN " + query, args)]}
        db.close()
        return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--days", type=int, nargs="+", default=[100, 1095])
    parser.add_argument("--sensors", type=int, default=50)
    parser.add_argument("--profiles", nargs="+", choices=["tag", "air"], default=["tag", "air"])
    args = parser.parse_args()
    if args.sensors < 1 or any(day < 7 for day in args.days):
        parser.error("Use at least one sensor and windows of at least seven days")
    root = Path(__file__).resolve().parents[1]
    adapter = root / "app/build/generated/source/kapt/withoutFileLogsDebug/com/ruuvi/station/database/tables/TagSensorReading_Table.java"
    schema = re.search(r'return "(CREATE TABLE IF NOT EXISTS `TagSensorReading`[^\n]+)";', adapter.read_text()).group(1)
    for profile in args.profiles:
        for days in args.days:
            print(json.dumps(benchmark(schema, days, args.sensors, profile)), flush=True)
