package com.dodhi.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.dodhi.data.model.Customer
import com.dodhi.data.model.DeliveryRecord
import com.dodhi.data.model.Payment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfManager(context: Context) {
    private val appContext = context.applicationContext

    fun generateCustomerReport(
        milkmanName: String,
        customer: Customer,
        records: List<DeliveryRecord>,
        payments: List<Payment>,
        month: Calendar,
        isUrdu: Boolean = false,
        previousBalance: Double = 0.0
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val accentColor = 0xFF4CAF50.toInt() // GrassGreen

        try {
            // Header background
            paint.color = accentColor
            canvas.drawRect(0f, 0f, 595f, 120f, paint)
            
            paint.color = Color.WHITE
            paint.textSize = 32f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(milkmanName.uppercase(), 40f, 65f, paint)
            
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            paint.letterSpacing = 0.1f
            val monthStr = SimpleDateFormat("MMMM yyyy", if (isUrdu) Locale("ur") else Locale.ENGLISH).format(month.time)
            val titleStr = if (isUrdu) "ماہانہ رپورٹ - $monthStr" else "MONTHLY MILK STATEMENT - $monthStr"
            canvas.drawText(titleStr, 40f, 95f, paint)
            paint.letterSpacing = 0f

            // Customer Info Card
            paint.color = Color.rgb(245, 245, 245)
            canvas.drawRoundRect(40f, 150f, 555f, 220f, 8f, 8f, paint)
            
            paint.color = Color.BLACK
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(customer.name, 60f, 185f, paint)
            
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.GRAY
            val localityLabel = if (isUrdu) "علاقہ: ${customer.locality}" else "Locality: ${customer.locality}"
            canvas.drawText(localityLabel, 60f, 205f, paint)
            
            paint.color = Color.rgb(100, 100, 100)
            val generatedLabel = if (isUrdu) "تاریخ اخراج: " else "Statement Date: "
            val genDate = SimpleDateFormat("dd MMM yyyy").format(Date())
            canvas.drawText(generatedLabel + genDate, 400f, 185f, paint)

            // Table Header
            var y = 250f
            paint.color = accentColor
            canvas.drawRect(40f, y, 555f, y + 35f, paint)
            
            paint.color = Color.WHITE
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            
            val headers = if (isUrdu) listOf("تاریخ", "تفصیل", "مقدار", "رقم") else listOf("DATE", "DESCRIPTION", "QUANTITY", "AMOUNT (Rs.)")
            canvas.drawText(headers[0], 60f, y + 22f, paint)
            canvas.drawText(headers[1], 150f, y + 22f, paint)
            canvas.drawText(headers[2], 350f, y + 22f, paint)
            canvas.drawText(headers[3], 450f, y + 22f, paint)
            
            y += 60f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.BLACK
            paint.textSize = 11f
            
            // Ledger logic
            val recordsInMonth = records.filter { isSameMonth(it.date, month) }
            val paymentsInMonth = payments.filter { isSameMonth(it.date, month) }
            
            val ledgerEntries = (recordsInMonth.map {Triple(it.date, it.type, it.quantity to it.amount)} + 
                                paymentsInMonth.map {Triple(it.date, "Payment", 0.0 to it.amount)})
                                .sortedBy { it.first }

            ledgerEntries.forEachIndexed { index, entry ->
                val (date, label, values) = entry
                if (y > 750f) {
                    // Simple page limit handling
                    return@forEachIndexed 
                }
                
                // Zebrawing
                if (index % 2 == 1) {
                    paint.color = Color.rgb(250, 250, 250)
                    canvas.drawRect(40f, y - 18f, 555f, y + 8f, paint)
                }
                
                paint.color = Color.BLACK
                val (qty, amount) = values
                val dateStr = SimpleDateFormat("dd MMM").format(Date(date))
                canvas.drawText(dateStr, 60f, y, paint)
                
                val typeLabel = when(label) {
                    "Delivered" -> if (isUrdu) "ڈیلیوری" else "Milk Delivery"
                    "Extra" -> if (isUrdu) "اضافی" else "Extra Milk"
                    "Naga" -> if (isUrdu) "ناغہ" else "Off Day (Naga)"
                    "Payment" -> if (isUrdu) "ادائیگی" else "Payment Received"
                    else -> label
                }
                
                if (label == "Payment") {
                    paint.color = Color.rgb(0, 100, 0) // Green for payments received
                    canvas.drawText(typeLabel, 150f, y, paint)
                    canvas.drawText("-", 350f, y, paint)
                    canvas.drawText("%.0f".format(amount), 450f, y, paint)
                } else {
                    paint.color = Color.BLACK
                    canvas.drawText(typeLabel, 150f, y, paint)
                    canvas.drawText("%.1f".format(qty), 350f, y, paint)
                    canvas.drawText("%.0f".format(amount), 450f, y, paint)
                }
                
                y += 24f
                paint.color = Color.rgb(235, 235, 235)
                canvas.drawLine(40f, y-12f, 555f, y-12f, paint)
                paint.color = Color.BLACK
            }

            // Footer Summary Section
            y += 30f
            if (y > 780f) y = 780f
            
            paint.color = Color.rgb(240, 245, 240)
            canvas.drawRect(350f, y - 20f, 555f, y + 60f, paint)
            
            paint.color = Color.BLACK
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            
            val currentBill = recordsInMonth.sumOf { it.amount }
            val currentPaid = paymentsInMonth.sumOf { it.amount }
            val totalPayable = previousBalance + currentBill - currentPaid
            
            val prevBalanceLabel = if (isUrdu) "پچھلا بقایا:" else "Prev. Balance:"
            val billLabel = if (isUrdu) "اس مہینے کا بل:" else "Monthly Bill:"
            val paidLabel = if (isUrdu) "ادائیگی:" else "Amt. Paid:"
            val balanceLabelLabel = if (isUrdu) "کل واجب الادا:" else "Total Payable:"
            
            canvas.drawText(prevBalanceLabel, 365f, y, paint)
            canvas.drawText("%.0f".format(previousBalance), 480f, y, paint)
            
            y += 18f
            canvas.drawText(billLabel, 365f, y, paint)
            canvas.drawText("%.0f".format(currentBill), 480f, y, paint)
            
            y += 18f
            canvas.drawText(paidLabel, 365f, y, paint)
            canvas.drawText("%.0f".format(currentPaid), 480f, y, paint)
            
            y += 25f
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = accentColor
            canvas.drawText(balanceLabelLabel, 365f, y, paint)
            canvas.drawText("%.0f".format(totalPayable), 480f, y, paint)

            // Final Stamp/Thank you
            paint.color = Color.rgb(180, 180, 180)
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            val footerMsg = if (isUrdu) "شکریہ! آپ کے تعاون کا شکریہ۔" else "Thank you for your business!"
            canvas.drawText(footerMsg, 40f, 810f, paint)

            pdfDocument.finishPage(page)

            val dir = File(appContext.cacheDir, "reports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "Report_${customer.id}_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(file.outputStream())
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private fun isSameMonth(date1: Long, cal2: Calendar): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
}
