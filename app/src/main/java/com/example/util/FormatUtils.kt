package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object FormatUtils {

    private val idLocale = Locale("id", "ID")

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(idLocale)
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        return formatter.format(amount).replace("Rp", "Rp ").replace('\u00A0', ' ')
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", idLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", idLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", idLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", idLocale)
        return sdf.format(Date(timestamp))
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
            "Permisi $cleanName, izin mengingatkan tagihan kasbon galon Santoya RO masih ${formatRupiah(sisaKasbon)}$notaText. Terima kasih."
        } else {
            "Permisi $cleanName, mau konfirmasi pengantaran galon Santoya RO hari ini. Apakah perlu isi ulang?"
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

    fun calculateCrmPrediction(
        customerId: Int,
        transaksiList: List<com.example.data.entity.TransaksiDetail>
    ): CrmOrderPrediction {
        val zone = java.time.ZoneId.systemDefault()
        val distinctDates = transaksiList
            .asSequence()
            .filter { it.transaksi.id_pelanggan == customerId }
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
                badgeText = "Belum Terprediksi"
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
                badgeText = "Belum Terprediksi"
            )
        }

        val avgIntervalDays = kotlin.math.round(differences.average()).toInt().coerceAtLeast(1)
        val lastOrderDate = distinctDates.last()
        val nextExpectedDate = lastOrderDate.plusDays(avgIntervalDays.toLong())
        val today = java.time.LocalDate.now(zone)
        val daysUntilExpected = java.time.temporal.ChronoUnit.DAYS.between(today, nextExpectedDate).toInt()

        val nextExpectedMillis = nextExpectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val shortDateFormatter = SimpleDateFormat("dd MMM", idLocale)
        val formattedDate = shortDateFormatter.format(Date(nextExpectedMillis))

        val status = when {
            daysUntilExpected > 1 -> CrmOrderStatus.ACTIVE
            daysUntilExpected in 0..1 -> CrmOrderStatus.DUE_SOON
            else -> CrmOrderStatus.OVERDUE
        }

        val badgeText = when (status) {
            CrmOrderStatus.ACTIVE -> "Prediksi: $formattedDate"
            CrmOrderStatus.DUE_SOON -> "Waktunya Order"
            CrmOrderStatus.OVERDUE -> "Terlambat ${-daysUntilExpected} Hari"
            CrmOrderStatus.INSUFFICIENT_DATA -> "Belum Terprediksi"
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

    fun calculateEstimatedIntervalDays(
        customerId: Int,
        transaksiList: List<com.example.data.entity.TransaksiDetail>
    ): Int? {
        return calculateCrmPrediction(customerId, transaksiList).averageIntervalDays
    }

    fun calculateEmployeeInsight(
        customerId: Int,
        prediction: CrmOrderPrediction,
        transaksiList: List<com.example.data.entity.TransaksiDetail>
    ): EmployeeCustomerInsight {
        val customerTxs = transaksiList.filter { it.transaksi.id_pelanggan == customerId }
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
                "Lewat $overdueDays Hari"
            }
            EmployeeScheduleBucket.HARI_INI -> "Jadwal Hari Ini"
            EmployeeScheduleBucket.BESOK -> "Jadwal Besok"
            EmployeeScheduleBucket.JADWAL_NANTI -> "Jadwal: ${prediction.nextExpectedDateFormatted ?: "-"}"
            EmployeeScheduleBucket.BELUM_TERPREDIKSI -> {
                if (customerTxs.isNotEmpty()) "Baru ${customerTxs.size}x Antar" else "Langganan Baru"
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
    val badgeText: String = "Belum Terprediksi"
)

