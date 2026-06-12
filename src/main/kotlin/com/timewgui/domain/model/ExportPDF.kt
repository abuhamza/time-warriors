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

    data class ReportRow(
        val date: String,
        val start: String,
        val end: String,
        val tags: String,
        val duration: String
    )

    companion object {
        fun exportReportToPdf(
            appState: AppState,
            range: ReportRange,
            startDate: LocalDate,
            endDate: LocalDate,
            rows: List<ReportRow>,
            totalLine: String,
            tagLines: List<String>,
            overtimeLines: List<String> = emptyList()
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

                        // Compute column positions across full width
                        val pageWidth = page.mediaBox.width
                        val tableWidth = pageWidth - 2 * margin

                        val colDateWidth = 70f
                        val colStartWidth = 50f
                        val colEndWidth = 50f
                        val colDurationWidth = 60f
                        val colTagsWidth = tableWidth - colDateWidth - colStartWidth - colEndWidth - colDurationWidth

                        val xDate = margin
                        val xStart = xDate + colDateWidth
                        val xEnd = xStart + colStartWidth
                        val xTags = xEnd + colEndWidth
                        val xDuration = xTags + colTagsWidth

// Header
                        content.setFont(PDType1Font.HELVETICA_BOLD, 10f)

                        content.beginText()
                        content.newLineAtOffset(xDate, y)
                        content.showText("Date")
                        content.endText()

                        content.beginText()
                        content.newLineAtOffset(xStart, y)
                        content.showText("Start")
                        content.endText()

                        content.beginText()
                        content.newLineAtOffset(xEnd, y)
                        content.showText("End")
                        content.endText()

                        content.beginText()
                        content.newLineAtOffset(xTags, y)
                        content.showText("Tags")
                        content.endText()

                        content.beginText()
                        content.newLineAtOffset(xDuration, y)
                        content.showText("Duration")
                        content.endText()

                        y -= leading

                        // Rows
                        content.setFont(PDType1Font.HELVETICA, 10f)
                        var lastDate: String? = null

                        for (row in rows) {
                            if (y < margin) break

                            val dateToShow = if (row.date == lastDate) "" else row.date

                            // Date (blank if same as previous)
                            if (dateToShow.isNotEmpty()) {
                                content.beginText()
                                content.newLineAtOffset(xDate, y)
                                content.showText(dateToShow)
                                content.endText()
                            }

                            // Start
                            content.beginText()
                            content.newLineAtOffset(xStart, y)
                            content.showText(row.start)
                            content.endText()

                            // End
                            content.beginText()
                            content.newLineAtOffset(xEnd, y)
                            content.showText(row.end)
                            content.endText()

                            // Tags
                            content.beginText()
                            content.newLineAtOffset(xTags, y)
                            content.showText(row.tags)
                            content.endText()

                            // Duration
                            content.beginText()
                            content.newLineAtOffset(xDuration, y)
                            content.showText(row.duration)
                            content.endText()

                            lastDate = row.date       // always update with the real date
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

                        // Overtime (if any lines provided)
                        if (overtimeLines.isNotEmpty()) {
                            y -= leading

                            content.beginText()
                            content.setFont(PDType1Font.HELVETICA_BOLD, 10f)
                            content.newLineAtOffset(margin, y)
                            content.showText("Overtime")
                            content.endText()
                            y -= leading

                            content.setFont(PDType1Font.HELVETICA, 10f)
                            for (line in overtimeLines) {
                                if (y < margin) break
                                content.beginText()
                                content.newLineAtOffset(margin + 20f, y)
                                content.showText(line)
                                content.endText()
                                y -= leading
                            }
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
