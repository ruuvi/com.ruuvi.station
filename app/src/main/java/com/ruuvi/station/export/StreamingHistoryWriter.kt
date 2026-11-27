package com.ruuvi.station.export

import java.io.Closeable
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Streaming XLSX with inline strings. No retained rows/shared-string table; repeat headers per sheet. */
class StreamingHistoryXlsx(output: OutputStream, private val columns: List<ExportColumn>,
                           private val rowsPerSheet: Int = 1_048_576) : Closeable {
    private val zip = ZipOutputStream(output)
    private var writer = OutputStreamWriter(zip, Charsets.UTF_8)
    private var sheet = 0
    private var row = 0
    private var closed = false
    init { require(rowsPerSheet in 2..1_048_576); beginSheet() }
    private fun entry(name: String, content: String) {
        zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
    }
    private fun beginSheet() {
        sheet++; row = 0
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet$sheet.xml"))
        writer = OutputStreamWriter(zip, Charsets.UTF_8)
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols>")
        columns.forEachIndexed { index, column ->
            writer.write("<col min=\"${index + 1}\" max=\"${index + 1}\" width=\"${column.width ?: 18.0}\" customWidth=\"1\"/>")
        }
        writer.write("</cols><sheetData>")
        writeRow(columns.map { it.name })
    }
    fun writeRow(values: List<Any?>) {
        check(!closed)
        if (row == rowsPerSheet) { endSheet(); beginSheet() }
        row++
        writer.write("<row r=\"$row\">")
        values.forEachIndexed { index, value ->
            val ref = columnName(index) + row
            when {
                value == null -> Unit
                value is Number && value.toDouble().isFinite() -> writer.write("<c r=\"$ref\"><v>$value</v></c>")
                else -> writer.write("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(value.toString())}</t></is></c>")
            }
        }
        writer.write("</row>")
    }
    private fun endSheet() { writer.write("</sheetData></worksheet>"); writer.flush(); zip.closeEntry() }
    override fun close() {
        if (closed) return
        closed = true
        try {
            endSheet()
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
                (1..sheet).joinToString("") { "<Override PartName=\"/xl/worksheets/sheet$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" } + "</Types>")
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            entry("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""" +
                (1..sheet).joinToString("") { "<sheet name=\"History $it\" sheetId=\"$it\" r:id=\"rId$it\"/>" } + "</sheets></workbook>")
            entry("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                (1..sheet).joinToString("") { "<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$it.xml\"/>" } + "</Relationships>")
        } finally { zip.close() }
    }
    companion object {
        private fun columnName(index: Int): String {
            var n = index + 1; var name = ""
            while (n > 0) { n--; name = ('A'.code + n % 26).toChar() + name; n /= 26 }
            return name
        }
        private fun xml(value: String) = value.filter { it >= ' ' || it == '\n' || it == '\r' || it == '\t' }
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    }
}

fun writeHistoryCsvRow(writer: java.io.Writer, row: List<Any?>) {
    row.forEachIndexed { index, value ->
        if (index > 0) writer.append(',')
        val text = value?.toString().orEmpty()
        if (text.any { it in ",\"\r\n" }) writer.append('"').append(text.replace("\"", "\"\"")).append('"') else writer.append(text)
    }
    writer.append('\n')
}
