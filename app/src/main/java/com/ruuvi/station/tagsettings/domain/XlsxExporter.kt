package com.ruuvi.station.tagsettings.domain

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.units.domain.UnitsConverter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class XlsxExporter (
    private val context: Context,
    private val repository: TagRepository,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val unitsConverter: UnitsConverter
){
//    fun exportToXlsx(sensorId: String): Uri? {
//        val cacheDir = File(context.cacheDir.path + "/export/")
//        cacheDir.mkdirs()
//        val fileName = "sample.xlsx"
//        val filePath = File("$cacheDir/$fileName")
//
//        try {
//
//            val fileOut = FileOutputStream(filePath)
//            val workbook: WritableWorkbook = Workbook.createWorkbook(fileOut)
//            val sheet = workbook.createSheet("Sample Sheet", 0)
//
//            val header1 = Label(0, 0, "Header 1")
//            val header2 = Label(1, 0, "Header 2")
//            sheet.addCell(header1)
//            sheet.addCell(header2)
//
//            val data1 = Label(0, 1, "Data 1")
//            val data2 = Label(1, 1, "Data 2")
//            sheet.addCell(data1)
//            sheet.addCell(data2)
//
//            workbook.write()
//            workbook.close()
//            fileOut.close()
//
//            return FileProvider.getUriForFile(context, "com.ruuvi.station.fileprovider", filePath)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return null
//            // Handle exceptions
//        }
//    }

        fun exportToXlsx(sensorId: String): Uri? {
            val cacheDir = File(context.cacheDir.path + "/export/")
            cacheDir.mkdirs()
            val fileName = "sample.xlsx"
            val filePath = File("$cacheDir/$fileName")

            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Sample Sheet")

                val headerRow = sheet.createRow(0)
                headerRow.createCell(0).setCellValue("Header 1")
                headerRow.createCell(1).setCellValue("Header 2")

                val dataRow = sheet.createRow(1)
                dataRow.createCell(0).setCellValue("Data 1")
                dataRow.createCell(1).setCellValue("Data 2")

                val fileOut = FileOutputStream(filePath)
                workbook.write(fileOut)
                fileOut.close()
                workbook.close()

                return FileProvider.getUriForFile(context, "com.ruuvi.station.fileprovider", filePath)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
                // Handle exceptions
            }
            return null
        }

}