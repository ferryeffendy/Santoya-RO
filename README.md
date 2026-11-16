# Santoya RO

Aplikasi kasir dan pencatatan depot air minum (galon) untuk Android.
Data disimpan lokal di Room dan disinkronkan ke Cloud Firestore agar bisa dipakai
beberapa perangkat dalam satu depot.

## Fitur utama

- Kasir cepat (tunai / kasbon) dan riwayat nota.
- Pelanggan, harga khusus, kasbon, dan pelunasan.
- Kurir, komisi per galon, dan laporan komisi historis.
- Produk, pengeluaran, laporan omzet/laba, serta ekspor CSV/JSON.
- Multi-perangkat dengan sinkronisasi real-time dan antrean offline.
- Peran **Pemilik** dan **Karyawan**; karyawan hanya melihat data depot yang ditautkan.

## Prasyarat

- Android Studio (JDK 17) atau JDK 17 + Android SDK 36.
- JDK 21 diperlukan hanya untuk menjalankan unit test Robolectric dengan SDK 36.
- Proyek Firebase dengan **Authentication (Google)** dan **Cloud Firestore** aktif.
- File `app/google-services.json` dari Firebase Console.

## Setup lokal

1. Clone repo, lalu buka di Android Studio.
2. Salin `app/google-services.json` dari Firebase Console ke folder `app/`.
   File ini **tidak boleh** di-commit.
3. Salin `.env.example` menjadi `.env` bila perlu mengubah konfigurasi publik.
4. Build:

   ```bash
   ./gradlew assembleDebug
   ```

5. Jalankan di perangkat/emulator. Login memakai akun Google yang sudah
   didaftarkan sebagai pemilik atau karyawan depot.

## Setup Firebase

1. Aktifkan provider **Google** di Authentication.
2. Buat database Firestore (mode production).
3. Deploy aturan keamanan:

   ```bash
   firebase deploy --only firestore:rules
   ```

   Aturan ada di `firestore.rules`. Pemilik depot hanya bisa mengakses
   `depots/{depotId}` miliknya; karyawan hanya bisa mengakses depot yang
   ditautkan lewat `staff_links/{email}`.

4. Struktur data utama:

   ```text
   staff_links/{email}
   depots/{depotId}
     produk/{syncId}
     kurir/{syncId}
     pelanggan/{syncId}
     transaksi/{syncId}
     kasbon_logs/{syncId}
     pengeluaran/{syncId}
   ```

## Peran dan keamanan

- Pemilik: akses semua layar, termasuk pengeluaran, laporan, produk, dan karyawan.
- Karyawan: hanya dashboard, transaksi, dan pelanggan yang ditugaskan.
- Jika pemilik menonaktifkan karyawan (`active=false`), aplikasi karyawan
  terkunci, data lokal dibersihkan, dan transaksi baru diblokir.
- Saat akun Google berganti di perangkat yang sama, seluruh data lokal dihapus
  sebelum depot baru dimuat.

## Build APK lewat GitHub Actions

Workflow `.github/workflows/BuildAPK.yml` membuat APK debug. Tambahkan secret
berikut di **Settings > Secrets and variables > Actions**:

| Secret | Isi |
| --- | --- |
| `GOOGLE_SERVICES_JSON` | Isi lengkap file `google-services.json` |

Setelah secret diisi, jalankan workflow **Build Android APK** dari tab
**Actions** (atau push ke `main`). Unduh APK dari bagian **Artifacts**.

Workflow `.github/workflows/CompileCheck.yml` berjalan otomatis pada setiap
push/PR. Workflow ini membangun APK debug dengan konfigurasi Firebase dummy
dan menjalankan unit test terpilih, sehingga error kompilasi cepat terdeteksi
tanpa memerlukan secret.

## Impor ke AI Studio

1. Buka AI Studio Build, lalu pilih **Import from GitHub**.
2. Pilih repo `ferryeffendy/Santoya-RO` dan branch `main`.
3. AI Studio hanya mengimpor kode sumber; AI Studio tidak membangun APK.
   Untuk membuat APK, gunakan GitHub Actions atau Android Studio.
4. Jika AI Studio meminta konfigurasi Firebase, unggah
   `app/google-services.json` yang sesuai dengan project Firebase aplikasi.

## Catatan migrasi database

Database Room saat ini berada di versi 16 (`exportSchema = true` di `app/schemas/`). Semua perubahan skema harus disertai
`Migration` eksplisit di `AppDatabase.kt`; `fallbackToDestructiveMigration()`
sengaja tidak dipakai agar data pengguna tidak terhapus otomatis.

## Struktur singkat

```text
app/src/main/java/com/example/
  data/          entity, DAO, repository, database
  ui/            layar Compose dan komponen
  util/          sinkronisasi Firestore, ekspor, helper struk
  viewmodel/     state dan aksi aplikasi
firestore.rules  aturan keamanan Firestore
```
