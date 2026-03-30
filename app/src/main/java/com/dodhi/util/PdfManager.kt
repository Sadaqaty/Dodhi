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
        isUrdu: Boolean = false
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
            canvas.drawRect(0f, 0f, 595f, 100f, paint)
            
            paint.color = Color.WHITE
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(milkmanName.uppercase(), 40f, 60f, paint)
            
            paint.textSize = 14f
            paint.typeface = Typeface.DEFAULT
            val monthStr = SimpleDateFormat("MMMM yyyy", if (isUrdu) Locale("ur") else Locale.ENGLISH).format(month.time)
            val titleStr = if (isUrdu) "ماہانہ رپورٹ - $monthStr" else "Monthly Milk Statement - $monthStr"
            canvas.drawText(titleStr, 40f, 85f, paint)

            // Customer Info Section
            paint.color = Color.BLACK
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(customer.name, 40f, 150f, paint)
            
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            val localityLabel = if (isUrdu) "علاقہ: ${customer.locality}" else "Locality: ${customer.locality}"
            canvas.drawText(localityLabel, 40f, 170f, paint)
            
            val generatedLabel = if (isUrdu) "تاریخ اخراج: " else "Generated: "
            canvas.drawText(generatedLabel + SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date()), 40f, 185f, paint)

            // Table Header
            var y = 220f
            paint.color = Color.rgb(240, 240, 240)
            canvas.drawRect(40f, y, 555f, y + 30f, paint)
            
            paint.color = Color.BLACK
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            
            val headers = if (isUrdu) listOf("تاریخ", "تفصیل", "مقدار", "رقم") else listOf("DATE", "DESCRIPTION", "QTY (L/S)", "AMOUNT (PKR)")
            canvas.drawText(headers[0], 50f, y + 20f, paint)
            canvas.drawText(headers[1], 150f, y + 20f, paint)
            canvas.drawText(headers[2], 350f, y + 20f, paint)
            canvas.drawText(headers[3], 450f, y + 20f, paint)
            
            y += 50f
            paint.typeface = Typeface.DEFAULT
            
            // Ledger logic
            val recordsInMonth = records.filter { isSameMonth(it.date, month) }
            val paymentsInMonth = payments.filter { isSameMonth(it.date, month) }
            
            val ledgerEntries = (recordsInMonth.map {Triple(it.date, it.type, it.quantity to it.amount)} + 
                               paymentsInMonth.map {Triple(it.date, "Payment", 0.0 to it.amount)}) // Amount is positive in DB for payments
                               .sortedBy { it.first }

            ledgerEntries.forEach { (date, label, values) ->
                if (y > 780f) {
                    // Page limit reached
                    return@forEach
                }
                val (qty, amount) = values
                val dateStr = SimpleDateFormat("dd/MM").format(Date(date))
                
                canvas.drawText(dateStr, 50f, y, paint)
                
                val typeLabel = when(label) {
                    "Delivered" -> if (isUrdu) "ڈیلیوری" else "Delivery"
                    "Extra" -> if (isUrdu) "اضافی" else "Extra"
                    "Naga" -> if (isUrdu) "ناغہ" else "Naga"
                    "Payment" -> if (isUrdu) "رقم" else "Payment"
                    else -> label
                }
                canvas.drawText(typeLabel, 150f, y, paint)
                
                if (label == "Payment") {
                    canvas.drawText("-", 350f, y, paint)
                    paint.color = Color.rgb(200, 0, 0)
                    canvas.drawText("%.1f".format(amount), 450f, y, paint)
                    paint.color = Color.BLACK
                } else {
                    canvas.drawText("%.1f".format(qty), 350f, y, paint)
                    canvas.drawText("%.1f".format(amount), 450f, y, paint)
                }
                
                y += 22f
                paint.color = Color.rgb(230, 230, 230)
                canvas.drawLine(40f, y-15f, 555f, y-15f, paint)
                paint.color = Color.BLACK
            }

            // Footer Summary
            y += 20f
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val totalBill = recordsInMonth.sumOf { it.amount }
            val totalPaid = paymentsInMonth.sumOf { it.amount }
            val netBalance = totalBill - totalPaid
            
            val billLabel = if (isUrdu) "کل بل:" else "TOTAL BILL:"
            val paidLabel = if (isUrdu) "کل ادائیگی:" else "TOTAL PAID:"
            
            canvas.drawText("$billLabel %.1f".format(totalBill), 40f, y, paint)
            canvas.drawText("$paidLabel %.1f".format(totalPaid), 250f, y, paint)
            
            y += 25f
            paint.color = accentColor
            val balanceLabel = if (isUrdu) "بقیہ رقم: %.1f روپیہ" else "NET BALANCE: %.1f PKR"
            canvas.drawText(balanceLabel.format(netBalance), 40f, y, paint)

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
