package com.ruuvi.station.export

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.StringWriter
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

class StreamingHistoryWriterTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun `CSV quotes commas quotes newlines and unicode`() {
        val output = StringWriter()
        writeHistoryCsvRow(output, listOf("a,b", "x\"y", "a\nb", "°C", null, 1.5))
        assertEquals("\"a,b\",\"x\"\"y\",\"a\nb\",°C,,1.5\n", output.toString())
    }
    @Test fun `XLSX splits sheets repeats headers and preserves numeric cells and XML text`() {
        val file = folder.newFile("history.xlsx")
        StreamingHistoryXlsx(file.outputStream(), listOf(ExportColumn("Date & time"), ExportColumn("T < °C")), 4).use { writer ->
            repeat(8) { writer.writeRow(listOf("x<&\"", it + .5)) }
        }
        ZipFile(file).use { zip ->
            assertNotNull(zip.getEntry("xl/worksheets/sheet3.xml"))
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val counts = (1..3).map { index ->
                val doc = zip.getInputStream(zip.getEntry("xl/worksheets/sheet$index.xml")).use { factory.newDocumentBuilder().parse(it) }
                assertTrue(doc.documentElement.textContent.contains("Date & time"))
                assertTrue(doc.documentElement.textContent.contains("x<&\""))
                doc.getElementsByTagName("row").length
            }
            assertEquals(listOf(4, 4, 3), counts)
            zip.entries().asSequence().forEach { entry -> zip.getInputStream(entry).use { factory.newDocumentBuilder().parse(it) } }
        }
    }
    @Test fun `full three years exceeds Excel limit and streams into two sheets`() {
        val file = folder.newFile("three-years.xlsx")
        val count = 1096 * 1440
        StreamingHistoryXlsx(file.outputStream(), listOf(ExportColumn("Timestamp"), ExportColumn("Temperature"))).use { writer ->
            repeat(count) { writer.writeRow(listOf(it * 60L, it % 100)) }
        }
        ZipFile(file).use { zip ->
            assertNotNull(zip.getEntry("xl/worksheets/sheet2.xml")); assertNull(zip.getEntry("xl/worksheets/sheet3.xml"))
            var rows = 0L
            (1..2).forEach { index ->
                zip.getInputStream(zip.getEntry("xl/worksheets/sheet$index.xml")).bufferedReader().use { input ->
                    // Streaming scanner: XML is intentionally one line; never read it all into memory.
                    var matched = 0; val pattern = "<row "
                    while (true) { val char = input.read(); if (char < 0) break
                        if (char.toChar() == pattern[matched]) { matched++; if (matched == pattern.length) { rows++; matched = 0 } }
                        else matched = if (char.toChar() == '<') 1 else 0
                    }
                }
            }
            assertEquals(count + 2L, rows)
        }
    }
}
