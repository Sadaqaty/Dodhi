package com.dodhi.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.dodhi.data.model.Customer
import com.dodhi.data.model.DeliveryRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    fun generateBill(context: Context, customer: Customer, records: List<DeliveryRecord>): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        
        paint.color = Color.BLACK
        paint.textSize = 16f
        canvas.drawText("Dodhi Bill", 100f, 50f, paint)
        
        paint.textSize = 12f
        canvas.drawText("Customer: ${customer.name}", 20f, 100f, paint)
        canvas.drawText("Date: ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())}", 20f, 120f, paint)
        
        var y = 160f
        records.forEach { record ->
            canvas.drawText("${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(record.date))}: ${record.quantity} L - ${record.type}", 20f, y, paint)
            y += 20f
        }
        
        canvas.drawText("Total: ${records.sumOf { it.amount }} PKR", 20f, y + 20f, paint)
        
        pdfDocument.finishPage(page)
        
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "bill_${customer.id}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
        return file
    }
}
