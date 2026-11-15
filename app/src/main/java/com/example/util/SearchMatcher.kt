package com.example.util

import java.text.Normalizer
import java.util.Locale

/**
 * Representasi satu field teks milik item yang dapat dicari beserta bobot relevansinya.
 */
data class SearchField(
    val text: String,
    val weight: Int = 10,
    val isPrimaryName: Boolean = false,
    val isPhone: Boolean = false
)

/**
 * Mesin pencarian pintar Santoya RO.
 *
 * Mendukung:
 * - Pemeringkatan relevansi bertingkat (persis > awalan nama > awal kata > substring > fuzzy typo).
 * - Pencarian multi-kata bebas lintas field (AND logic per kata, mis. "budi mawar").
 * - Normalisasi tanda baca, spasi, diakritik, serta sinonim singkatan umum depot (Bp/Pak, Ibu/Bu, Jl/Jalan, dll).
 * - Normalisasi format nomor HP Indonesia (08... <-> +628... / 628...).
 * - Toleransi typo ringan adaptif (Damerau-Levenshtein) hanya untuk kata >= 3 huruf agar query pendek tidak berisik.
 * - Pencarian rentang indeks untuk penyorotan (highlight) pada teks asli UI.
 */
object SearchMatcher {

    private val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val PUNCTUATION_REGEX = "[^\\p{L}\\p{N}]+".toRegex()
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /**
     * Kelompok sinonim singkatan umum Indonesia untuk nama & alamat pelanggan depot air.
     */
    private val SYNONYM_GROUPS: List<Set<String>> = listOf(
        setOf("bp", "bpk", "bapak", "pak"),
        setOf("bu", "ibu"),
        setOf("jl", "jln", "jalan"),
        setOf("gg", "gang"),
        setOf("ds", "desa"),
        setOf("dsn", "dusun"),
        setOf("kp", "kmp", "kampung"),
        setOf("blk", "blok"),
        setOf("no", "nomor"),
        setOf("sdr", "saudara"),
        setOf("tk", "toko"),
        setOf("wr", "wrg", "warung"),
        setOf("rm", "rumah"),
        setOf("kntr", "kantor"),
        setOf("ms", "mas"),
        setOf("mb", "mbk", "mbak")
    )

    private val SYNONYM_LOOKUP: Map<String, Set<String>> = buildMap {
        SYNONYM_GROUPS.forEach { group ->
            group.forEach { token -> put(token, group) }
        }
    }

    /**
     * Menormalkan teks menjadi huruf kecil tanpa aksen, tanda baca diubah ke spasi, dan spasi dirapikan.
     */
    fun normalizeText(raw: String): String {
        if (raw.isBlank()) return ""
        val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFD)
        val noMarks = DIACRITICS_REGEX.replace(decomposed, "")
        val lower = noMarks.lowercase(Locale.ROOT)
        val cleaned = PUNCTUATION_REGEX.replace(lower, " ")
        return WHITESPACE_REGEX.replace(cleaned, " ").trim()
    }

    /**
     * Memecah query pencarian menjadi daftar kata (token) unik yang sudah dinormalkan.
     */
    fun tokenize(rawQuery: String): List<String> {
        val norm = normalizeText(rawQuery)
        if (norm.isBlank()) return emptyList()
        return norm.split(' ').filter { it.isNotBlank() }.distinct()
    }

    /**
     * Mengembalikan daftar variasi token termasuk sinonim singkatan umum jika ada.
     */
    fun expandToken(token: String): Set<String> {
        val norm = normalizeText(token)
        if (norm.isBlank()) return emptySet()
        return SYNONYM_LOOKUP[norm] ?: setOf(norm)
    }

    /**
     * Menghasilkan variasi digit nomor HP Indonesia (08... <-> 628...).
     */
    fun phoneDigitVariants(rawDigits: String): List<String> {
        if (rawDigits.isEmpty()) return emptyList()
        val variants = linkedSetOf(rawDigits)
        if (rawDigits.startsWith("62") && rawDigits.length > 2) {
            variants.add("0" + rawDigits.substring(2))
        } else if (rawDigits.startsWith("0") && rawDigits.length > 1) {
            variants.add("62" + rawDigits.substring(1))
        }
        return variants.toList()
    }

    /**
     * Menyaring dan mengurutkan daftar item berdasarkan skor relevansi tertinggi.
     * Ketika [query] kosong, urutan asli [items] dipertahankan sepenuhnya.
     * Item dengan skor sama tetap mengikuti urutan asli [items] (stable sort).
     */
    fun <T> rankAndFilter(
        items: List<T>,
        query: String,
        fieldsProvider: (T) -> List<SearchField>
    ): List<T> {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return items
        val normQuery = tokens.joinToString(" ")

        val scored = ArrayList<Pair<Int, Int>>(items.size)
        for (index in items.indices) {
            val score = scoreFieldsInternal(normQuery, tokens, fieldsProvider(items[index]))
            if (score > 0) {
                scored.add(score to index)
            }
        }
        if (scored.isEmpty()) return emptyList()
        scored.sortWith(
            Comparator { a, b ->
                val scoreCmp = b.first.compareTo(a.first)
                if (scoreCmp != 0) scoreCmp else a.second.compareTo(b.second)
            }
        )
        return scored.map { items[it.second] }
    }

    /**
     * Menghitung skor relevansi [query] terhadap kumpulan [fields].
     * Mengembalikan 0 jika tidak cocok.
     */
    fun scoreFields(query: String, fields: List<SearchField>): Int {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return 1
        val normQuery = tokens.joinToString(" ")
        return scoreFieldsInternal(normQuery, tokens, fields)
    }

    private fun scoreFieldsInternal(
        normQuery: String,
        tokens: List<String>,
        fields: List<SearchField>
    ): Int {
        if (fields.isEmpty()) return 0

        val preparedFields = fields.mapNotNull { field ->
            if (field.text.isBlank()) return@mapNotNull null
            val norm = normalizeText(field.text)
            val words = if (norm.isBlank()) emptyList() else norm.split(' ').filter { it.isNotBlank() }
            val digits = if (field.isPhone) field.text.filter { it.isDigit() } else ""
            val phoneVariants = if (field.isPhone && digits.isNotEmpty()) phoneDigitVariants(digits) else emptyList()
            PreparedField(
                raw = field,
                norm = norm,
                words = words,
                digits = digits,
                phoneVariants = phoneVariants
            )
        }
        if (preparedFields.isEmpty()) return 0

        var totalScore = 0

        // 1. Bonus kecocokan frasa utuh pada nama utama (menjamin nama pendek persis & awalan selalu di paling atas)
        for (pf in preparedFields) {
            if (!pf.raw.isPrimaryName) continue
            val phraseBonus = when {
                pf.norm == normQuery -> 12_000
                pf.norm.startsWith(normQuery) -> 7_500
                pf.words.any { it == normQuery } -> 5_000
                pf.words.any { it.startsWith(normQuery) } -> 3_500
                pf.norm.contains(normQuery) -> 1_600
                else -> 0
            }
            if (phraseBonus > 0) {
                totalScore += phraseBonus
                break
            }
        }

        // 2. Penilaian wajib per token (AND logic: setiap token harus cocok di minimal satu field)
        for (token in tokens) {
            val variants = expandToken(token)
            val tokenDigits = token.filter { it.isDigit() }
            val tokenDigitVariants = if (tokenDigits.length >= 2) phoneDigitVariants(tokenDigits) else emptyList()
            var bestTokenScore = 0

            for (pf in preparedFields) {
                val multiplier = pf.raw.weight.coerceAtLeast(1)
                var fieldScore = 0

                // 2a. Pencocokan langsung & sinonim
                for (variant in variants) {
                    val allowSubwordContains = pf.raw.isPrimaryName || variant.length >= 2
                    val directScore = when {
                        pf.norm == variant -> 120 * multiplier
                        pf.words.any { it == variant } -> 105 * multiplier
                        pf.norm.startsWith(variant) -> 95 * multiplier
                        pf.words.any { it.startsWith(variant) } -> 80 * multiplier
                        allowSubwordContains && pf.norm.contains(variant) -> 45 * multiplier
                        else -> 0
                    }
                    if (directScore > fieldScore) {
                        fieldScore = directScore
                    }
                }

                // 2b. Pencocokan nomor HP lintas format (08 <-> 628)
                if (pf.raw.isPhone && tokenDigitVariants.isNotEmpty() && pf.phoneVariants.isNotEmpty()) {
                    for (tv in tokenDigitVariants) {
                        for (pv in pf.phoneVariants) {
                            val phoneScore = when {
                                pv == tv -> 115 * multiplier
                                pv.startsWith(tv) -> 90 * multiplier
                                pv.contains(tv) -> 60 * multiplier
                                else -> 0
                            }
                            if (phoneScore > fieldScore) {
                                fieldScore = phoneScore
                            }
                        }
                    }
                }

                // 2c. Toleransi typo ringan adaptif (hanya untuk token huruf >= 3 karakter agar query pendek 1-2 huruf tetap presisi)
                if (fieldScore == 0 && token.length >= 3 && token.any { it.isLetter() }) {
                    val maxDist = if (token.length <= 4) 1 else 2
                    for (word in pf.words) {
                        if (word.length < 3) continue
                        val distFull = if (kotlin.math.abs(word.length - token.length) <= maxDist) {
                            damerauLevenshtein(token, word, maxDist)
                        } else {
                            Int.MAX_VALUE
                        }
                        val rawPrefixDist = if (token.length >= 4 && word.length > token.length) {
                            val prefixSlice = word.substring(0, token.length)
                            damerauLevenshtein(token, prefixSlice, 1)
                        } else {
                            Int.MAX_VALUE
                        }
                        val distPrefix = if (rawPrefixDist <= 1) rawPrefixDist else Int.MAX_VALUE
                        val bestDist = minOf(distFull, distPrefix)
                        if (bestDist <= maxDist) {
                            val fuzzyBase = when (bestDist) {
                                0 -> 50
                                1 -> 22
                                else -> 10
                            }
                            val fuzzyScore = fuzzyBase * multiplier
                            if (fuzzyScore > fieldScore) {
                                fieldScore = fuzzyScore
                            }
                        }
                    }
                }

                if (fieldScore > bestTokenScore) {
                    bestTokenScore = fieldScore
                }
            }

            if (bestTokenScore == 0) {
                return 0
            }
            totalScore += bestTokenScore
        }

        return totalScore
    }

    /**
     * Menghitung jarak edit Damerau-Levenshtein (mendukung sisip, hapus, ganti, dan tukar 2 huruf bersebelahan)
     * dengan early cutoff [maxDistance] agar tetap sangat cepat.
     */
    fun damerauLevenshtein(a: String, b: String, maxDistance: Int = 2): Int {
        val lenA = a.length
        val lenB = b.length
        if (kotlin.math.abs(lenA - lenB) > maxDistance) return maxDistance + 1
        if (a == b) return 0
        if (lenA == 0) return lenB
        if (lenB == 0) return lenA

        val dp = Array(lenA + 1) { IntArray(lenB + 1) }
        for (i in 0..lenA) dp[i][0] = i
        for (j in 0..lenB) dp[0][j] = j

        for (i in 1..lenA) {
            var rowMin = Int.MAX_VALUE
            for (j in 1..lenB) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = minOf(value, dp[i - 2][j - 2] + 1)
                }
                dp[i][j] = value
                if (value < rowMin) rowMin = value
            }
            if (rowMin > maxDistance) {
                return maxDistance + 1
            }
        }
        return dp[lenA][lenB]
    }

    /**
     * Menemukan daftar rentang indeks `[start, endExclusive)` pada [text] asli yang cocok dengan [query]
     * untuk kebutuhan penyorotan (highlight) di UI.
     */
    fun findHighlightRanges(text: String, query: String): List<IntRange> {
        if (text.isBlank() || query.isBlank()) return emptyList()
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return emptyList()

        val ranges = mutableListOf<IntRange>()
        val lowerText = text.lowercase(Locale.ROOT)

        // Bangun peta huruf-huruf alfanumerik (tanpa tanda baca) ke indeks karakter asli
        val alphaChars = StringBuilder()
        val alphaToOriginalIndex = ArrayList<Int>(text.length)
        val digitsOnly = StringBuilder()
        val digitToOriginalIndex = ArrayList<Int>(text.length)

        for (i in text.indices) {
            val ch = text[i]
            val folded = foldSingleChar(ch)
            if (folded != null) {
                alphaChars.append(folded)
                alphaToOriginalIndex.add(i)
            }
            if (ch.isDigit()) {
                digitsOnly.append(ch)
                digitToOriginalIndex.add(i)
            }
        }
        val compactAlpha = alphaChars.toString()
        val compactDigits = digitsOnly.toString()

        // Ekstrak batas kata asli untuk fuzzy highlight
        val originalWords = extractOriginalWords(text)

        for (token in tokens) {
            var tokenMatchedDirect = false
            val variants = expandToken(token)

            for (variant in variants) {
                // 1. Cari kemunculan langsung pada lowerText
                var searchFrom = 0
                while (searchFrom < lowerText.length) {
                    val idx = lowerText.indexOf(variant, searchFrom)
                    if (idx < 0) break
                    val end = (idx + variant.length).coerceAtMost(text.length)
                    if (idx < end) {
                        ranges.add(idx until end)
                        tokenMatchedDirect = true
                    }
                    searchFrom = idx + variant.length.coerceAtLeast(1)
                }

                // 2. Jika belum ketemu (mis. ada tanda baca/spasi/format angka seperti 0812-3456 atau 15.000),
                // cari di string alfanumerik ringkas dan petakan ke indeks asli.
                if (!tokenMatchedDirect && variant.isNotEmpty() && compactAlpha.isNotEmpty()) {
                    var alphaFrom = 0
                    while (alphaFrom < compactAlpha.length) {
                        val idx = compactAlpha.indexOf(variant, alphaFrom)
                        if (idx < 0) break
                        val endAlphaIdx = idx + variant.length - 1
                        if (endAlphaIdx < alphaToOriginalIndex.size) {
                            val origStart = alphaToOriginalIndex[idx]
                            val origEnd = (alphaToOriginalIndex[endAlphaIdx] + 1).coerceAtMost(text.length)
                            if (origStart < origEnd) {
                                ranges.add(origStart until origEnd)
                                tokenMatchedDirect = true
                            }
                        }
                        alphaFrom = idx + variant.length.coerceAtLeast(1)
                    }
                }
            }

            // 3. Pencocokan digit terpisah tanda hubung/titik (nomor HP 08<->62 atau nominal 15000 <-> 15.000)
            val tokenDigits = token.filter { it.isDigit() }
            if (!tokenMatchedDirect && tokenDigits.length >= 2 && compactDigits.isNotEmpty()) {
                val digitVariants = phoneDigitVariants(tokenDigits)
                for (dv in digitVariants) {
                    var dFrom = 0
                    while (dFrom < compactDigits.length) {
                        val idx = compactDigits.indexOf(dv, dFrom)
                        if (idx < 0) break
                        val endDIdx = idx + dv.length - 1
                        if (endDIdx < digitToOriginalIndex.size) {
                            val origStart = digitToOriginalIndex[idx]
                            val origEnd = (digitToOriginalIndex[endDIdx] + 1).coerceAtMost(text.length)
                            if (origStart < origEnd) {
                                ranges.add(origStart until origEnd)
                                tokenMatchedDirect = true
                            }
                        }
                        dFrom = idx + dv.length.coerceAtLeast(1)
                    }
                }
            }

            // 4. Fallback sorotan untuk hasil fuzzy typo (kata >= 3 huruf)
            if (!tokenMatchedDirect && token.length >= 3 && token.any { it.isLetter() }) {
                val maxDist = if (token.length <= 4) 1 else 2
                var bestWordRange: IntRange? = null
                var bestDist = Int.MAX_VALUE
                for ((normWord, origRange) in originalWords) {
                    if (normWord.length < 3) continue
                    val distFull = if (kotlin.math.abs(normWord.length - token.length) <= maxDist) {
                        damerauLevenshtein(token, normWord, maxDist)
                    } else {
                        Int.MAX_VALUE
                    }
                    val rawPrefixDist = if (token.length >= 4 && normWord.length > token.length) {
                        damerauLevenshtein(token, normWord.substring(0, token.length), 1)
                    } else {
                        Int.MAX_VALUE
                    }
                    val distPrefix = if (rawPrefixDist <= 1) rawPrefixDist else Int.MAX_VALUE
                    val dist = minOf(distFull, distPrefix)
                    if (dist <= maxDist && dist < bestDist) {
                        bestDist = dist
                        bestWordRange = origRange
                    }
                }
                if (bestWordRange != null) {
                    ranges.add(bestWordRange)
                }
            }
        }

        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedWith(compareBy<IntRange> { it.first }.thenBy { it.last })
        val merged = ArrayList<IntRange>(sorted.size)
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= current.last + 1) {
                current = current.first..maxOf(current.last, next.last)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun foldSingleChar(ch: Char): Char? {
        if (!ch.isLetterOrDigit()) return null
        val decomposed = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)
        for (c in decomposed) {
            if (Character.isLetterOrDigit(c)) {
                return c.lowercaseChar()
            }
        }
        return null
    }

    private fun extractOriginalWords(text: String): List<Pair<String, IntRange>> {
        val result = mutableListOf<Pair<String, IntRange>>()
        var start = -1
        for (i in text.indices) {
            val isWordChar = text[i].isLetterOrDigit()
            if (isWordChar && start < 0) {
                start = i
            } else if (!isWordChar && start >= 0) {
                val slice = text.substring(start, i)
                val norm = normalizeText(slice)
                if (norm.isNotEmpty()) {
                    result.add(norm to (start until i))
                }
                start = -1
            }
        }
        if (start >= 0) {
            val slice = text.substring(start, text.length)
            val norm = normalizeText(slice)
            if (norm.isNotEmpty()) {
                result.add(norm to (start until text.length))
            }
        }
        return result
    }

    private data class PreparedField(
        val raw: SearchField,
        val norm: String,
        val words: List<String>,
        val digits: String,
        val phoneVariants: List<String>
    )
}
