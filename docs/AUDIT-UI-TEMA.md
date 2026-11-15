# Audit Tampilan (UI) Santoya RO

Tanggal: 15 November 2026

## Ringkasan

Fondasi tampilan Santoya RO sebenarnya sudah bagus: sudah punya warna merek,
komponen kartu bersama, dan ilustrasi sendiri. Masalahnya bukan "tidak ada tema",
tetapi **detail yang belum senada**. Yang paling terasa adalah font tebal yang
dibuat palsu oleh Android, kontras warna yang belum nyaman dibaca, ukuran sudut
dan jarak yang berbeda-beda, serta beberapa tombol yang terlalu kecil untuk jari.

Audit ini hanya mengubah tampilan. Tidak ada perubahan pada data, transaksi,
sinkronisasi, atau aturan akses.

## Temuan dan prioritas

### 1. Font tebal palsu (paling besar)

Aplikasi hanya membawa **satu file font, yaitu versi Regular**. Saat kode meminta
Bold, ExtraBold, atau Black, Android menggemukkan huruf Regular secara sintetis.
Hasilnya huruf terlihat gemuk berlebihan, jarak antarhuruf berantakan, dan kurang
tajam di layar kecil.

**Dampak:** seluruh judul, angka omzet, nama produk, dan tombol terlihat kurang
profesional.

### 2. Ada teks yang memakai font bawaan Android

Gaya `headlineLarge` dipakai di Dashboard, tetapi belum didefinisikan di tema.
Akibatnya teks itu memakai Roboto, bukan Plus Jakarta Sans. Satu teks yang beda
font sudah cukup membuat tampilan terasa tidak rapi.

### 3. Kontras warna belum lolos standar kenyamanan

Warna biru utama lama `#3B93E6` dengan teks putih hanya punya kontras 3,23:1.
Standar aksesibilitas untuk teks normal adalah minimal 4,5:1. Biru ini sering
dipakai untuk tulisan kecil dan tombol, jadi sebagian teks terasa tipis.

Warna hijau dan oranye untuk status juga berada di bawah 4,5:1.

### 4. Sudut dan jarak belum seragam

Ada 17 variasi sudut membulat (2, 3, 4, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16,
18, 20, 22 dp). Jarak kiri-kanan halaman juga campur: 12, 14, dan 16 dp.
Perbedaannya kecil, tetapi mata menangkapnya sebagai "kurang rapi".

### 5. Beberapa tombol terlalu kecil

Tombol ikon hapus, edit, telepon, cetak, dan tutup ada yang berukuran 32–42 dp.
Ukuran nyaman untuk jari adalah minimal 48 dp.

### 6. Judul dobel di Produk dan Karyawan

Header atas sudah menulis "Produk & Harga" dan "Karyawan & Komisi", lalu kartu
pertama di halaman menulis judul yang sama lagi. Informasi jadi berulang.

### 7. Warna status ditulis manual

Beberapa warna hijau, oranye, dan silver ditulis langsung di layar, bukan memakai
warna status tema. Kalau nanti tema diubah, bagian ini bisa tertinggal.

## Yang sudah diperbaiki pada paket ini

1. **Font asli lengkap**
   - Plus Jakarta Sans Regular, Medium, SemiBold, Bold, ExtraBold, dan Black.
   - Lisensi font resmi (OFL) disertakan di aplikasi.
2. **Skala huruf dirapikan**
   - Semua gaya huruf sekarang terisi, termasuk `headlineLarge`.
   - Bobot huruf tidak lagi berlebihan di judul.
3. **Warna lebih nyaman dibaca**
   - Biru utama menjadi `#1B6FC0` (kontras 5,15:1).
   - Hijau status `#2F7D3C` dan oranye status `#9A5A0B` (di atas 5:1).
   - Warna aksen logo yang terang tetap dipakai untuk hiasan, bukan teks kecil.
4. **Sudut membulat diseragamkan**
   - Seluruh layar kini memakai ukuran resmi: 6, 10, 12, 16, 20, dan 24 dp.
   - Sudut 8, 9, 11, 13, 14, 15, 18, dan 22 dp yang tadinya campur sudah
     disatukan ke ukuran terdekat.
   - Komponen yang tidak mengatur sudut sendiri otomatis mengikuti tema.
5. **Ukuran sentuh**
   - Tombol ikon hapus/edit/telepon/cetak/tutup menjadi 48 dp.
   - Pil filter tetap ringkas, tetapi area sentuhnya 48 dp.
6. **Jarak halaman**
   - Daftar Pelanggan, Pengeluaran, Perawatan, dan Riwayat kini memakai margin
     kiri-kanan 16 dp seperti layar lain.
7. **Judul dobel dihapus**
   - Produk menjadi "Daftar Produk".
   - Karyawan menjadi "Tim Pengantar".
8. **Angka uang lebih stabil**
   - Angka rupiah memakai lebar digit seragam, sehingga tidak "bergoyang" saat
     nominal berubah.
9. **Warna status memakai tema**
   - Hijau "Lunasi Nota", oranye kasbon, dan silver peringkat tidak lagi ditulis
     manual.

## Belum dikerjakan (usulan paket berikutnya)

1. Splash screen bermerek saat aplikasi dibuka. Saat ini masih memakai layar
   bawaan Android.
2. Merapikan header tiap layar agar benar-benar satu pola. Saat ini Dashboard,
   Laporan, dan Pengaturan punya gaya header sendiri-sendiri.
3. Mengurangi pemakaian huruf Bold berlebihan di kartu (saat ini ada 300+).
   Sebaiknya hanya judul yang tebal, isi tetap normal.
4. Mengganti teks yang masih tercampur istilah asing ("Cash", "DP") dengan
   padanan Indonesia yang konsisten.
5. Uji tampilan di HP layar kecil (lebar 360 dp), mode gelap, dan ukuran huruf
   sistem "Besar".

## Aturan desain untuk pengembangan berikutnya

- Jarak: 4, 8, 12, 16, 20, 24, 32 dp.
- Sudut: 6, 10, 12, 16, 20, 24 dp (lihat `SantoyaTokens.kt`).
- Area sentuh minimal 48 dp.
- Warna teks dan tombol harus memakai `MaterialTheme.colorScheme` atau
  `MaterialTheme.santoyaColors`, bukan angka warna langsung.
- Judul halaman cukup satu kali, di header atas.
- Isi teks memakai `bodyMedium`/`bodySmall`; jangan semuanya Bold.
