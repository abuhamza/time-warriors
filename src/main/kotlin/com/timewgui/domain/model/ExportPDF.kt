package com.timewgui.domain.model


import com.timewgui.ui.screens.ReportRange
import com.timewgui.viewmodel.AppState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.time.temporal.IsoFields

public class ExportPDF {

    companion object {
        fun exportReportToPdf(
            appState: AppState,
            range: ReportRange,
            startDate: LocalDate,
            endDate: LocalDate,
            rows: List<String>,
            totalLine: String,
            tagLines: List<String>
        ): File? {
            val baseDir = File(appState.pdfReportDir.ifBlank {
                System.getProperty("user.home") ?: "."
            })
            baseDir.mkdirs()

            val fileName = when (range) {
                is ReportRange.Year -> {
                    "timew_summary_${startDate.year}.pdf"
                }
                is ReportRange.Month -> {
                    val month = "%02d".format(startDate.monthNumber)
                    "timew_summary_${startDate.year}-$month.pdf"
                }
                is ReportRange.Today -> {
                    val month = "%02d".format(startDate.monthNumber)
                    val day = "%02d".format(startDate.dayOfMonth)
                    "timew_summary_${startDate.year}-$month-$day.pdf"
                }
                is ReportRange.Week -> {
                    val javaDate = startDate.toJavaLocalDate()
                    val week = javaDate.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                    val weekStr = "%02d".format(week)
                    "timew_summary_${startDate.year}-WK$weekStr.pdf"
                }
                else -> {
                    throw IllegalArgumentException("Unsupported report range: $range")
                }
            }

            val file = File(baseDir, fileName)

            return try {
                PDDocument().use { document ->
                    val page = PDPage(PDRectangle.A4)
                    document.addPage(page)

                    val margin = 40f
                    val pageHeight = page.mediaBox.height
                    var y = pageHeight - margin
                    val leading = 14f

                    PDPageContentStream(document, page).use { content ->
                        // Title
                        content.beginText()
                        content.setFont(PDType1Font.HELVETICA_BOLD, 14f)
                        content.newLineAtOffset(margin, y)
                        content.showText("Report $startDate — $endDate")
                        content.endText()
                        y -= 2 * leading

                        // Header
                        content.beginText()
                        content.setFont(PDType1Font.HELVETICA_BOLD, 10f)
                        content.newLineAtOffset(margin, y)
                        content.showText("Date | Tags | Duration")
                        content.endText()
                        y -= leading

                        // Rows
                        content.setFont(PDType1Font.HELVETICA, 10f)
                        for (line in rows) {
                            if (y < margin) break
                            content.beginText()
                            content.newLineAtOffset(margin, y)
                            content.showText(line)
                            content.endText()
                            y -= leading
                        }

                        y -= leading

                        // Total
                        content.beginText()
                        content.newLineAtOffset(margin, y)
                        content.showText("Total: $totalLine")
                        content.endText()
                        y -= 2 * leading

                        // Time by Tag
                        content.beginText()
                        content.newLineAtOffset(margin, y)
                        content.showText("Time by Tag:")
                        content.endText()
                        y -= leading

                        for (line in tagLines) {
                            if (y < margin) break
                            content.beginText()
                            content.newLineAtOffset(margin + 20f, y)
                            content.showText(line)
                            content.endText()
                            y -= leading
                        }
                    }

                    document.save(file)
                }

                println("PDF exported to: ${file.absolutePath}")
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
