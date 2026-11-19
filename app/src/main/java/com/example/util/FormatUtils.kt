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
        val numStr = requireNotNull(integerNumberFormatter.get()).format(absVal)
        return if (isNegative) "-Rp $numStr" else "Rp $numStr"
    }

    /**
     * Standard Indonesian number grouping: "1.250"
     */
    fun formatNumber(number: Number): String {
        return requireNotNull(integerNumberFormatter.get()).format(number)
    }

    /**
     * Standard Indonesian percentage with comma decimal separator: "15,5%"
     */
    fun formatPercent(percent: Double, decimals: Int = 1): String {
        val formatted = when {
            decimals <= 0 -> requireNotNull(integerNumberFormatter.get()).format(percent)
            decimals == 1 -> requireNotNull(singleDecimalFormatter.get()).format(percent)
            else -> {
                val pattern = "#,##0." + "0".repeat(decimals)
                java.text.DecimalFormat(pattern, idSymbols).format(percent)
            }
        }
        return "$formatted%"
    }

    /**
     * Memformat label metode bayar untuk tampilan UI dalam Bahasa Indonesia.
     * Menjaga kompatibilitas data historis "Cash" agar tetap tampil sebagai "Tunai".
     */
    fun formatMetodeBayarLabel(metode: String?): String {
        val clean = metode?.trim().orEmpty()
        return when {
            clean.isBlank() || clean == "-" || clean.equals("Cash", ignoreCase = true) || clean.equals("Tunai", ignoreCase = true) -> "Tunai"
            clean.equals("Bayar Sebagian", ignoreCase = true) -> "Bayar Sebagian"
            else -> clean
        }
    }

    /**
     * Standard UI date: "13 Nov 2026"
     */
    fun formatDateUi(timestamp: Long): String {
        return requireNotNull(dateUiFormatter.get()).format(Date(timestamp))
    }

    /**
     * Standard UI date with time: "13 Nov 2026, 14:30"
     */
    fun formatDateTimeUi(timestamp: Long): String {
        return requireNotNull(dateTimeUiFormatter.get()).format(Date(timestamp))
    }

    /**
     * Standard Table / Receipt date: "13/11/2026"
     */
    fun formatDateTable(timestamp: Long): String {
        return requireNotNull(dateTableFormatter.get()).format(Date(timestamp))
    }

    /**
     * Standard Table / Receipt date with time: "13/11/2026 14:30"
     */
    fun formatDateTimeTable(timestamp: Long): String {
        return requireNotNull(dateTimeTableFormatter.get()).format(Date(timestamp))
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
        return requireNotNull(timeOnlyFormatter.get()).format(Date(timestamp))
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

    fun getStartOfWeek(timestamp: Long = System.currentTimeMillis()): Long {
        val zone = java.time.ZoneId.systemDefault()
        val date = java.time.Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val monday = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        return monday.atStartOfDay(zone).toInstant().toEpochMilli()
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
     * Menggunakan pembobotan interval terbaru, penjagaan outlier jeda libur panjang, serta skor prioritas.
     */
    fun calculateCrmPredictionForCustomer(
        customerTransaksiList: List<com.example.data.entity.TransaksiDetail>,
        today: java.time.LocalDate = java.time.LocalDate.now(java.time.ZoneId.systemDefault()),
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): CrmOrderPrediction {
        val validTxs = customerTransaksiList.filter {
            !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) && it.transaksi.jumlah > 0
        }
        val lastOrderMillis = validTxs.maxOfOrNull { it.transaksi.tanggal }
        val avgGallons = if (validTxs.isNotEmpty()) {
            kotlin.math.round(validTxs.map { it.transaksi.jumlah }.average()).toInt().coerceAtLeast(1)
        } else {
            null
        }

        val distinctDates = validTxs
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
                badgeText = "Belum terjadwal",
                priorityScore = 0,
                avgGallonsPerOrder = avgGallons,
                lastOrderMillis = lastOrderMillis,
                distinctOrderDays = distinctDates.size,
                isReliableCycle = false
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
                badgeText = "Belum terjadwal",
                priorityScore = 0,
                avgGallonsPerOrder = avgGallons,
                lastOrderMillis = lastOrderMillis,
                distinctOrderDays = distinctDates.size,
                isReliableCycle = false
            )
        }

        // Ambil hingga 6 interval terbaru agar adaptif terhadap perubahan pola konsumsi terkini
        val recentIntervals = differences.takeLast(6)
        val guardedIntervals = if (recentIntervals.size >= 3) {
            val sorted = recentIntervals.sorted()
            val mid = sorted.size / 2
            val median = if (sorted.size % 2 == 1) {
                sorted[mid].toDouble()
            } else {
                (sorted[mid - 1] + sorted[mid]) / 2.0
            }
            val upperCap = (median * 2.5).coerceAtLeast(median + 7.0)
            recentIntervals.map { it.toDouble().coerceAtMost(upperCap) }
        } else {
            recentIntervals.map { it.toDouble() }
        }

        // Rata-rata berbobot: interval terbaru memiliki bobot lebih besar (1, 2, ..., k)
        var weightedSum = 0.0
        var totalWeight = 0.0
        guardedIntervals.forEachIndexed { idx, intervalDays ->
            val weight = (idx + 1).toDouble()
            weightedSum += intervalDays * weight
            totalWeight += weight
        }

        val avgIntervalDays = kotlin.math.round(weightedSum / totalWeight.coerceAtLeast(1.0))
            .toInt()
            .coerceAtLeast(1)
        val lastOrderDate = distinctDates.last()
        val nextExpectedDate = lastOrderDate.plusDays(avgIntervalDays.toLong())
        val daysUntilExpected = java.time.temporal.ChronoUnit.DAYS.between(today, nextExpectedDate).toInt()

        val nextExpectedMillis = nextExpectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val formattedDate = requireNotNull(shortDayMonthFormatter.get()).format(Date(nextExpectedMillis))

        val status = when {
            daysUntilExpected > 1 -> CrmOrderStatus.ACTIVE
            daysUntilExpected in 0..1 -> CrmOrderStatus.DUE_SOON
            else -> CrmOrderStatus.OVERDUE
        }

        val badgeText = when (status) {
            CrmOrderStatus.ACTIVE -> "Prediksi: $formattedDate"
            CrmOrderStatus.DUE_SOON -> if (daysUntilExpected <= 0) "Isi ulang hari ini" else "Besok isi ulang"
            CrmOrderStatus.OVERDUE -> "Lewat ${-daysUntilExpected} hari"
            CrmOrderStatus.INSUFFICIENT_DATA -> "Belum terjadwal"
        }

        val isReliableCycle = distinctDates.size >= 3
        val urgencyScore = when (status) {
            CrmOrderStatus.OVERDUE -> {
                val ratio = (-daysUntilExpected).toDouble() / avgIntervalDays.coerceAtLeast(1).toDouble()
                55 + kotlin.math.round(ratio * 35.0).toInt().coerceIn(5, 45)
            }
            CrmOrderStatus.DUE_SOON -> if (daysUntilExpected <= 0) 50 else 36
            CrmOrderStatus.ACTIVE -> (22 - daysUntilExpected).coerceIn(1, 22)
            CrmOrderStatus.INSUFFICIENT_DATA -> 0
        }
        val volumeBonus = ((avgGallons ?: 1) * 3).coerceIn(3, 18)
        val reliabilityBonus = if (isReliableCycle) 8 else 0
        val priorityScore = if (status == CrmOrderStatus.INSUFFICIENT_DATA) {
            0
        } else {
            urgencyScore + volumeBonus + reliabilityBonus
        }

        return CrmOrderPrediction(
            status = status,
            averageIntervalDays = avgIntervalDays,
            nextExpectedDateMillis = nextExpectedMillis,
            nextExpectedDateFormatted = formattedDate,
            daysUntilExpected = daysUntilExpected,
            badgeText = badgeText,
            priorityScore = priorityScore,
            avgGallonsPerOrder = avgGallons,
            lastOrderMillis = lastOrderMillis,
            distinctOrderDays = distinctDates.size,
            isReliableCycle = isReliableCycle
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
        val validTxs = customerTxs.filter {
            !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) && it.transaksi.jumlah > 0
        }
        val lastOrderMillis = prediction.lastOrderMillis ?: validTxs.maxOfOrNull { it.transaksi.tanggal }
        val daysSinceLastOrder = lastOrderMillis?.let { daysSince(it) }
        val avgGallons = prediction.avgGallonsPerOrder ?: if (validTxs.isNotEmpty()) {
            kotlin.math.round(validTxs.map { it.transaksi.jumlah }.average()).toInt().coerceAtLeast(1)
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
                if (validTxs.isNotEmpty()) "Baru ${validTxs.size}x order" else "Langganan Baru"
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
    val badgeText: String = "Belum terjadwal",
    val priorityScore: Int = 0,
    val avgGallonsPerOrder: Int? = null,
    val lastOrderMillis: Long? = null,
    val distinctOrderDays: Int = 0,
    val isReliableCycle: Boolean = false
)

