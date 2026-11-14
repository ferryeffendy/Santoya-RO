package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object FormatUtils {

    private val idLocale = Locale("id", "ID")
    private val idSymbols = java.text.DecimalFormatSymbols(idLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val integerNumberFormatter = object : ThreadLocal<java.text.DecimalFormat>() {
        override fun initialValue(): java.text.DecimalFormat =
            java.text.DecimalFormat("#,##0", idSymbols)
    }

    private val singleDecimalFormatter = object : ThreadLocal<java.text.DecimalFormat>() {
        override fun initialValue(): java.text.DecimalFormat =
            java.text.DecimalFormat("#,##0.0", idSymbols)
    }

    private val dateUiFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("dd MMM yyyy", idLocale)
    }

    private val dateTimeUiFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("dd MMM yyyy, HH:mm", idLocale)
    }

    private val dateTableFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("dd/MM/yyyy", idLocale)
    }

    private val dateTimeTableFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("dd/MM/yyyy HH:mm", idLocale)
    }

    private val timeOnlyFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("HH:mm", idLocale)
    }

    private val shortDayMonthFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("dd MMM", idLocale)
    }

    /**
     * Standard Indonesian Rupiah currency format: "Rp 15.000" or "-Rp 15.000".
     * Guaranteed deterministic separator (dot for thousands, no decimals).
     */
    fun formatRupiah(amount: Double): String {
        val isNegative = amount < 0
        val absVal = kotlin.math.abs(amount)
        val numStr = integerNumberFormatter.get()!!.format(absVal)
        return if (isNegative) "-Rp $numStr" else "Rp $numStr"
    }

    /**
     * Standard Indonesian number grouping: "1.250"
     */
    fun formatNumber(number: Number): String {
        return integerNumberFormatter.get()!!.format(number)
    }

    /**
     * Standard Indonesian percentage with comma decimal separator: "15,5%"
     */
    fun formatPercent(percent: Double, decimals: Int = 1): String {
        val formatted = when {
            decimals <= 0 -> integerNumberFormatter.get()!!.format(percent)
            decimals == 1 -> singleDecimalFormatter.get()!!.format(percent)
            else -> {
                val pattern = "#,##0." + "0".repeat(decimals)
                java.text.DecimalFormat(pattern, idSymbols).format(percent)
            }
        }
        return "$formatted%"
    }

    /**
     * Standard UI date: "13 Nov 2026"
     */
    fun formatDateUi(timestamp: Long): String {
        return dateUiFormatter.get()!!.format(Date(timestamp))
    }

    /**
     * Standard UI date with time: "13 Nov 2026, 14:30"
     */
    fun formatDateTimeUi(timestamp: Long): String {
        return dateTimeUiFormatter.get()!!.format(Date(timestamp))
    }

    /**
     * Standard Table / Receipt date: "13/11/2026"
     */
    fun formatDateTable(timestamp: Long): String {
        return dateTableFormatter.get()!!.format(Date(timestamp))
    }

    /**
     * Standard Table / Receipt date with time: "13/11/2026 14:30"
     */
    fun formatDateTimeTable(timestamp: Long): String {
        return dateTimeTableFormatter.get()!!.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return formatDateTimeUi(timestamp)
    }

    fun formatDateTime(timestamp: Long): String {
        return formatDateTimeUi(timestamp)
    }

    fun formatDateShort(timestamp: Long): String {
        return formatDateTable(timestamp)
    }

    fun formatTimeOnly(timestamp: Long): String {
        return timeOnlyFormatter.get()!!.format(Date(timestamp))
    }

    fun daysSince(timestamp: Long): Long {
        val diffMillis = System.currentTimeMillis() - timestamp
        return if (diffMillis <= 0) 0 else TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    fun formatPhoneForWhatsApp(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.length < 6) return ""
        return when {
            digits.startsWith("0") -> "62" + digits.drop(1)
            digits.startsWith("8") -> "62$digits"
            else -> digits
        }
    }

    fun buildEmployeeWhatsAppMessage(
        customerName: String,
        sisaKasbon: Double,
        unpaidCount: Int,
        isKasbonReminder: Boolean
    ): String {
        val cleanName = customerName.trim().ifBlank { "Bapak/Ibu" }
        return if (isKasbonReminder && sisaKasbon > 0.0) {
            val notaText = if (unpaidCount > 0) " ($unpaidCount nota)" else ""
            "Halo $cleanName, izin mengingatkan kasbon galon Santoya RO masih ${formatRupiah(sisaKasbon)}$notaText ya. Terima kasih."
        } else {
            "Halo $cleanName, dari Santoya RO. Galonnya mau diantar hari ini?"
        }
    }

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfMonth(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfYear(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getDaysInCurrentMonth(): Int {
        val cal = Calendar.getInstance()
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getCurrentDayOfMonth(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * O(K log K) CRM prediction operating directly on a single customer's transactions
     * (where K is the number of transactions for that customer), avoiding O(N) full-table scans.
     */
    fun calculateCrmPredictionForCustomer(
        customerTransaksiList: List<com.example.data.entity.TransaksiDetail>
    ): CrmOrderPrediction {
        if (customerTransaksiList.size < 2) {
            return CrmOrderPrediction(
                status = CrmOrderStatus.INSUFFICIENT_DATA,
                badgeText = "Belum terjadwal"
            )
        }
        val zone = java.time.ZoneId.systemDefault()
        val distinctDates = customerTransaksiList
            .asSequence()
            .map {
                java.time.Instant.ofEpochMilli(it.transaksi.tanggal)
                    .atZone(zone)
                    .toLocalDate()
            }
            .distinct()
            .sorted()
            .toList()

        if (distinctDates.size < 2) {
            return CrmOrderPrediction(
                status = CrmOrderStatus.INSUFFICIENT_DATA,
                badgeText = "Belum terjadwal"
            )
        }

        val differences = mutableListOf<Long>()
        for (i in 0 until distinctDates.size - 1) {
            val diff = java.time.temporal.ChronoUnit.DAYS.between(distinctDates[i], distinctDates[i + 1])
            if (diff > 0) differences.add(diff)
        }

        if (differences.isEmpty()) {
            return CrmOrderPrediction(
                status = CrmOrderStatus.INSUFFICIENT_DATA,
                badgeText = "Belum terjadwal"
            )
        }

        val avgIntervalDays = kotlin.math.round(differences.average()).toInt().coerceAtLeast(1)
        val lastOrderDate = distinctDates.last()
        val nextExpectedDate = lastOrderDate.plusDays(avgIntervalDays.toLong())
        val today = java.time.LocalDate.now(zone)
        val daysUntilExpected = java.time.temporal.ChronoUnit.DAYS.between(today, nextExpectedDate).toInt()

        val nextExpectedMillis = nextExpectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val formattedDate = shortDayMonthFormatter.get()!!.format(Date(nextExpectedMillis))

        val status = when {
            daysUntilExpected > 1 -> CrmOrderStatus.ACTIVE
            daysUntilExpected in 0..1 -> CrmOrderStatus.DUE_SOON
            else -> CrmOrderStatus.OVERDUE
        }

        val badgeText = when (status) {
            CrmOrderStatus.ACTIVE -> "Prediksi: $formattedDate"
            CrmOrderStatus.DUE_SOON -> "Waktunya isi ulang"
            CrmOrderStatus.OVERDUE -> "Lewat ${-daysUntilExpected} hari"
            CrmOrderStatus.INSUFFICIENT_DATA -> "Belum terjadwal"
        }

        return CrmOrderPrediction(
            status = status,
            averageIntervalDays = avgIntervalDays,
            nextExpectedDateMillis = nextExpectedMillis,
            nextExpectedDateFormatted = formattedDate,
            daysUntilExpected = daysUntilExpected,
            badgeText = badgeText
        )
    }

    fun calculateCrmPrediction(
        customerId: Int,
        transaksiList: List<com.example.data.entity.TransaksiDetail>
    ): CrmOrderPrediction {
        val customerTxs = transaksiList.filter { it.transaksi.id_pelanggan == customerId }
        return calculateCrmPredictionForCustomer(customerTxs)
    }

    fun calculateEstimatedIntervalDays(
        customerId: Int,
        transaksiList: List<com.example.data.entity.TransaksiDetail>
    ): Int? {
        return calculateCrmPrediction(customerId, transaksiList).averageIntervalDays
    }

    fun calculateEmployeeInsightForCustomer(
        prediction: CrmOrderPrediction,
        customerTxs: List<com.example.data.entity.TransaksiDetail>
    ): EmployeeCustomerInsight {
        val lastOrderMillis = customerTxs.maxOfOrNull { it.transaksi.tanggal }
        val daysSinceLastOrder = lastOrderMillis?.let { daysSince(it) }
        val avgGallons = if (customerTxs.isNotEmpty()) {
            kotlin.math.round(customerTxs.map { it.transaksi.jumlah }.average()).toInt().coerceAtLeast(1)
        } else {
            null
        }

        val bucket = when (prediction.status) {
            CrmOrderStatus.OVERDUE -> EmployeeScheduleBucket.TERLAMBAT
            CrmOrderStatus.DUE_SOON -> {
                if ((prediction.daysUntilExpected ?: 0) <= 0) {
                    EmployeeScheduleBucket.HARI_INI
                } else {
                    EmployeeScheduleBucket.BESOK
                }
            }
            CrmOrderStatus.ACTIVE -> EmployeeScheduleBucket.JADWAL_NANTI
            CrmOrderStatus.INSUFFICIENT_DATA -> EmployeeScheduleBucket.BELUM_TERPREDIKSI
        }

        val badgeLabel = when (bucket) {
            EmployeeScheduleBucket.TERLAMBAT -> {
                val overdueDays = -(prediction.daysUntilExpected ?: -1)
                "Lewat $overdueDays hari"
            }
            EmployeeScheduleBucket.HARI_INI -> "Jadwal Hari Ini"
            EmployeeScheduleBucket.BESOK -> "Jadwal Besok"
            EmployeeScheduleBucket.JADWAL_NANTI -> "Jadwal: ${prediction.nextExpectedDateFormatted ?: "-"}"
            EmployeeScheduleBucket.BELUM_TERPREDIKSI -> {
                if (customerTxs.isNotEmpty()) "Baru ${customerTxs.size}x order" else "Langganan Baru"
            }
        }

        return EmployeeCustomerInsight(
            daysSinceLastOrder = daysSinceLastOrder,
            lastOrderMillis = lastOrderMillis,
            avgGallonsPerOrder = avgGallons,
            scheduleBucket = bucket,
            badgeLabel = badgeLabel
        )
    }

    fun calculateEmployeeInsight(
        customerId: Int,
        prediction: CrmOrderPrediction,
        transaksiList: List<com.example.data.entity.TransaksiDetail>
    ): EmployeeCustomerInsight {
        val customerTxs = transaksiList.filter { it.transaksi.id_pelanggan == customerId }
        return calculateEmployeeInsightForCustomer(prediction, customerTxs)
    }
}

enum class CrmOrderStatus {
    ACTIVE,
    DUE_SOON,
    OVERDUE,
    INSUFFICIENT_DATA
}

enum class EmployeeScheduleBucket {
    TERLAMBAT,
    HARI_INI,
    BESOK,
    JADWAL_NANTI,
    BELUM_TERPREDIKSI
}

data class EmployeeCustomerInsight(
    val daysSinceLastOrder: Long? = null,
    val lastOrderMillis: Long? = null,
    val avgGallonsPerOrder: Int? = null,
    val scheduleBucket: EmployeeScheduleBucket = EmployeeScheduleBucket.BELUM_TERPREDIKSI,
    val badgeLabel: String = "Pelanggan Baru"
)

data class CrmOrderPrediction(
    val status: CrmOrderStatus,
    val averageIntervalDays: Int? = null,
    val nextExpectedDateMillis: Long? = null,
    val nextExpectedDateFormatted: String? = null,
    val daysUntilExpected: Int? = null,
    val badgeText: String = "Belum terjadwal"
)

