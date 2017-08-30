package ru.edustor.pdfgen.internal

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.color.Color
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants
import org.springframework.stereotype.Component
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId

@Component
open class PdfGenerator {


    private val fontBytes = this.javaClass.getResource("/fonts/Proxima Nova Thin.ttf").readBytes()
    private val PAGE_SIZE = PageSize.A4

    fun makePdf(outputStream: OutputStream,
                filename: String,
                authorName: String,
                subjectName: String,
                courseName: String,
                copyrightString: String,
                contactsString: String,
                drawCornell: Boolean = true,
                generateTitle: Boolean = true) {
        val pdfWriter = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(pdfWriter)
        pdfDocument.documentInfo.title = filename

        val now = LocalDateTime.now(ZoneId.of("Europe/Moscow")).withNano(0)
        val academicYear = when {
            now.month > Month.JUNE -> "${now.year}-${now.year + 1}"
            else -> "${now.year - 1}-${now.year}"
        }

//        Looks like it's necessary to create new PdfFont instance for each document
        val proximaNovaFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, true, false)

        if (generateTitle) {
            drawTitlePage(pdfDocument, proximaNovaFont, authorName, subjectName, courseName, copyrightString, contactsString, academicYear)
        }
        drawRegularPage(pdfDocument, proximaNovaFont, authorName, subjectName, courseName, copyrightString, contactsString, academicYear, drawCornell)

        pdfDocument.close()
    }

    private fun drawTitlePage(pdfDocument: PdfDocument,
                              proximaNovaFont: PdfFont,
                              authorName: String,
                              subjectName: String,
                              courseName: String,
                              copyrightString: String,
                              contactsString: String,
                              academicYear: String) {
        val page = pdfDocument.addNewPage(PAGE_SIZE)
        val canvas = PdfCanvas(page)
                .setLineWidth(0.1f)
                .setStrokeColor(Color.GRAY)
                .setLineJoinStyle(PdfCanvasConstants.LineJoinStyle.MITER)

        val edustorStr = "Edustor Digital"
        showText(canvas, calculateCenteredTextX(PAGE_SIZE.width, edustorStr, proximaNovaFont, 18f),
                PAGE_SIZE.top - 50.0, edustorStr, proximaNovaFont, 18f)


        showText(canvas, calculateCenteredTextX(PAGE_SIZE.width, courseName, proximaNovaFont, 20f),
                PAGE_SIZE.top - 365.0, courseName, proximaNovaFont, 20f)

        showText(canvas, calculateCenteredTextX(PAGE_SIZE.width, subjectName, proximaNovaFont, 30f),
                PAGE_SIZE.top - 400.0, subjectName, proximaNovaFont, 30f)

        showText(canvas, calculateCenteredTextX(PAGE_SIZE.width, authorName, proximaNovaFont, 18f),
                PAGE_SIZE.bottom + 100.0, authorName, proximaNovaFont, 18f)

        showText(canvas, calculateCenteredTextX(PAGE_SIZE.width, contactsString, proximaNovaFont, 10f),
                PAGE_SIZE.bottom + 80.0, contactsString, proximaNovaFont, 10f)

        val finalCopyrightString = "© $copyrightString $academicYear"
        showText(canvas, calculateCenteredTextX(PAGE_SIZE.width, finalCopyrightString, proximaNovaFont, 10f),
                PAGE_SIZE.bottom + 20.0, finalCopyrightString, proximaNovaFont, 10f)


    }

    private fun drawRegularPage(pdfDocument: PdfDocument,
                                proximaNovaFont: PdfFont,
                                authorName: String,
                                subjectName: String,
                                courseName: String,
                                copyrightString: String,
                                contactsString: String,
                                academicYear: String,
                                drawCornell: Boolean = true) {

        val page = pdfDocument.addNewPage(PAGE_SIZE)
        val canvas = PdfCanvas(page)
                .setLineWidth(0.1f)
                .setStrokeColor(Color.GRAY)
                .setLineJoinStyle(PdfCanvasConstants.LineJoinStyle.MITER)

        val TOP_FONT_SIZE = 11f
        val BOTTOM_FONT_SIZE = 8f
        val CELL_SIDE = 5 / 25.4f * 72

//            Draw grid
        val gridArea = drawGrid(canvas, PAGE_SIZE, CELL_SIDE, 40, 56, drawCornell)

//            Print top row
        val titleRow = when {
            authorName != "" -> "Edustor Digital: $authorName"
            else -> "Edustor Digital"
        }
        val topRowY = gridArea.top.toDouble() + 3
        canvas.beginText()
                .setFontAndSize(proximaNovaFont, TOP_FONT_SIZE)
                .moveText(gridArea.left.toDouble(), topRowY)
                .showText(titleRow)
                .endText()

        val topRightString = when {
            subjectName != "" -> "$subjectName, $courseName"
            else -> courseName
        }

        canvas.beginText()
                .moveText(gridArea.right.toDouble() - proximaNovaFont.getWidth(topRightString, TOP_FONT_SIZE),
                        topRowY)
                .showText(topRightString)
                .endText()

        canvas.setFontAndSize(proximaNovaFont, BOTTOM_FONT_SIZE)

//            Print bottom row
        val bottomRowY = gridArea.bottom.toDouble() - 9
        canvas.beginText()
                .moveText(gridArea.left.toDouble(), bottomRowY)
                .showText("© $copyrightString $academicYear")
                .endText()

        canvas.beginText()
                .moveText(gridArea.right.toDouble() - proximaNovaFont.getWidth(contactsString, BOTTOM_FONT_SIZE),
                        bottomRowY)
                .showText(contactsString)
                .endText()
    }

    /**
     * Draws grid on canvas
     * @param canvas Target canvas
     * @param pageSize Page size used to calculate margins
     * @param cellSide Cell side length, in pt
     * @return Grid borders rectangle
     */
    private fun drawGrid(canvas: PdfCanvas, pageSize: Rectangle, cellSide: Float,
                         xCells: Int, yCells: Int, drawCornell: Boolean): Rectangle {
        val xMargin = (pageSize.width - (xCells * cellSide)) / 2
        val yMargin = (pageSize.height - (yCells * cellSide)) / 2
        if (xMargin < 0 || yMargin < 0) {
            throw IllegalArgumentException("Can't fit ${xCells}x$yCells grid to specified page size")
        }

        val gridBorders = Rectangle(xMargin, yMargin, cellSide * xCells, cellSide * yCells)

        (0..xCells)
                .map { (gridBorders.left + it * cellSide).toDouble() }
                .forEach {
                    canvas.moveTo(it, gridBorders.top.toDouble())
                            .lineTo(it, gridBorders.bottom.toDouble())
                            .stroke()
                }

        (0..yCells)
                .map { (gridBorders.bottom + it * cellSide).toDouble() }
                .forEach {
                    canvas.moveTo(gridBorders.left.toDouble(), it)
                            .lineTo(gridBorders.right.toDouble(), it)
                            .stroke()
                }

//        Cornell template
        if (drawCornell) {
            canvas.saveState()
                    .setLineWidth(1f)

            val titleLineY = (gridBorders.top - 3 * cellSide).toDouble()
            canvas.moveTo(gridBorders.left.toDouble(), titleLineY)
                    .lineTo(gridBorders.right.toDouble(), titleLineY)
                    .stroke()

            val summaryLineY = (gridBorders.bottom + 5 * cellSide).toDouble()
            canvas.moveTo(gridBorders.left.toDouble(), summaryLineY)
                    .lineTo(gridBorders.right.toDouble(), summaryLineY)
                    .stroke()

            val notesLineX = (gridBorders.left + 8 * cellSide).toDouble()
            canvas.moveTo(notesLineX, gridBorders.top.toDouble())
                    .lineTo(notesLineX, (gridBorders.bottom + 5 * cellSide).toDouble())
                    .stroke()

            canvas.restoreState()
        }
        return gridBorders
    }

    private fun calculateCenteredTextX(width: Float, string: String, font: PdfFont, fontSize: Float): Double {
        return (width - font.getWidth(string, fontSize)) / 2.0
    }

    private fun showText(canvas: PdfCanvas, x: Double, y: Double, text: String, font: PdfFont, fontSize: Float) {
        canvas.beginText()
                .setFontAndSize(font, fontSize)
                .moveText(x, y)
                .showText(text)
                .endText()
    }
}
