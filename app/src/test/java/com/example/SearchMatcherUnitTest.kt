package com.example

import com.example.data.entity.Pelanggan
import com.example.util.SearchField
import com.example.util.SearchMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherUnitTest {

    private fun cust(
        id: Int,
        nama: String,
        alamat: String = "",
        noHp: String = ""
    ): Pelanggan = Pelanggan(
        id = id,
        nama = nama,
        alamat = alamat,
        no_hp = noHp
    )

    private fun rankPelanggan(list: List<Pelanggan>, query: String): List<Pelanggan> {
        return SearchMatcher.rankAndFilter(list, query) { p ->
            listOf(
                SearchField(p.nama, weight = 24, isPrimaryName = true),
                SearchField(p.alamat, weight = 10),
                SearchField(p.no_hp, weight = 14, isPhone = true)
            )
        }
    }

    @Test
    fun `short name exact and prefix matches always rank above substring matches`() {
        val noise = (1..40).map { i ->
            cust(id = i, nama = "Warung Kalimantan $i", alamat = "Jl. Galian Timur $i")
        }
        val exactShort = cust(id = 101, nama = "Al", alamat = "Blok C")
        val prefixShort = cust(id = 102, nama = "Ali", alamat = "Blok D")
        val wordStart = cust(id = 103, nama = "Bp. Alim", alamat = "Blok E")

        val all = noise + listOf(wordStart, prefixShort, exactShort)
        val results = rankPelanggan(all, "al")

        assertTrue(results.size >= 43)
        assertEquals("Al", results[0].nama)
        assertEquals("Ali", results[1].nama)
        assertEquals("Bp. Alim", results[2].nama)
    }

    @Test
    fun `single character query ranks exact and prefix names at top`() {
        val list = listOf(
            cust(1, "Budi", "Jl. Merdeka"),
            cust(2, "Hasan", "Jl. Mawar"),
            cust(3, "Ani", "Jl. Melati"),
            cust(4, "A", "Blok A")
        )
        val results = rankPelanggan(list, "a")

        assertEquals(listOf("A", "Ani", "Hasan"), results.map { it.nama })
    }

    @Test
    fun `multi-token search matches words across name and address in any order`() {
        val list = listOf(
            cust(1, "Bu Sari", "Jl. Anggrek No. 4"),
            cust(2, "Bu Sari", "Jl. Mawar No. 10"),
            cust(3, "Pak Budi", "Jl. Mawar No. 2")
        )
        val results = rankPelanggan(list, "mawar sari")

        assertEquals(1, results.size)
        assertEquals(2, results.first().id)
    }

    @Test
    fun `synonym and punctuation normalization matches abbreviations`() {
        val list = listOf(
            cust(1, "Bp. Ali", "Jl. Melati No. 5"),
            cust(2, "Ibu Rina", "Gg. Kenanga"),
            cust(3, "Toko Berkah", "Ds. Sukamaju")
        )

        assertEquals(listOf(1), rankPelanggan(list, "pak ali").map { it.id })
        assertEquals(listOf(1), rankPelanggan(list, "jalan melati").map { it.id })
        assertEquals(listOf(2), rankPelanggan(list, "bu rina gang kenanga").map { it.id })
        assertEquals(listOf(3), rankPelanggan(list, "tk berkah desa sukamaju").map { it.id })
    }

    @Test
    fun `phone number search normalizes 08 and 62 formats and hyphens`() {
        val list = listOf(
            cust(1, "Eko", noHp = "+62 812-3456-7890"),
            cust(2, "Dedi", noHp = "0857-9988-7766")
        )

        assertEquals(listOf(1), rankPelanggan(list, "08123456").map { it.id })
        assertEquals(listOf(2), rankPelanggan(list, "628579988").map { it.id })
    }

    @Test
    fun `adaptive typo tolerance matches misspelled longer names without polluting 2-char queries`() {
        val list = listOf(
            cust(1, "Slamet Riyadi", "Jl. Mawar"),
            cust(2, "Budi Santoso", "Jl. Melati"),
            cust(3, "Ali", "Blok A")
        )

        // Typo transposition "slmaet" -> Slamet, "mawr" -> Mawar
        assertEquals(listOf(1), rankPelanggan(list, "slmaet").map { it.id })
        assertEquals(listOf(1), rankPelanggan(list, "mawr").map { it.id })

        // 2-char query "ab" must NOT fuzzy-match "Ali"
        val shortNoise = rankPelanggan(list, "ab")
        assertTrue(shortNoise.isEmpty())
    }

    @Test
    fun `findHighlightRanges locates accurate ranges for direct, formatted, and fuzzy matches`() {
        val direct = SearchMatcher.findHighlightRanges("Bp. Ali Santoso", "ali")
        assertFalse(direct.isEmpty())
        assertEquals("Ali", "Bp. Ali Santoso".substring(direct.first()))

        val phone = SearchMatcher.findHighlightRanges("0812-3456-7890", "081234")
        assertFalse(phone.isEmpty())
        assertEquals("0812-34", "0812-3456-7890".substring(phone.first()))

        val nominal = SearchMatcher.findHighlightRanges("Rp 15.000", "15000")
        assertFalse(nominal.isEmpty())
        assertEquals("15.000", "Rp 15.000".substring(nominal.first()))

        val fuzzy = SearchMatcher.findHighlightRanges("Slamet Riyadi", "slmaet")
        assertFalse(fuzzy.isEmpty())
        assertEquals("Slamet", "Slamet Riyadi".substring(fuzzy.first()))
    }
}
