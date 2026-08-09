package com.webunime.mobile.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LegalDoc {
    Privacy,
    Terms,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    doc: LegalDoc,
    onBack: () -> Unit,
) {
    val title = when (doc) {
        LegalDoc.Privacy -> "Privacy Policy"
        LegalDoc.Terms -> "Terms of Service"
    }
    val body = when (doc) {
        LegalDoc.Privacy -> privacyPolicyBody
        LegalDoc.Terms -> termsOfServiceBody
    }

    Scaffold(
        containerColor = Color(0xFF151719),
        topBar = {
            TopAppBar(
                title = {
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF151719),
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Terakhir diperbarui: 10 Agustus 2026",
                color = Color(0xFFA0A0A1),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text(
                text = body,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

private val privacyPolicyBody = """
1. Pendahuluan
Weeboonime ("kami") menghormati privasi pengguna. Kebijakan ini menjelaskan data apa yang kami kumpulkan, bagaimana kami menggunakannya, dan pilihan yang kamu miliki.

2. Data yang Dikumpulkan
• Informasi akun (jika login), seperti nama tampilan dan email.
• Data penggunaan aplikasi: riwayat menonton, preferensi, perangkat, dan log error.
• Data teknis: jenis perangkat, versi OS, dan identitas instalasi untuk update aplikasi.

3. Penggunaan Data
Data digunakan untuk:
• menyediakan dan memperbaiki layanan streaming katalog anime;
• menyimpan riwayat menonton dan preferensi lokal;
• mengirim pembaruan aplikasi (OTA);
• mencegah penyalahgunaan dan menganalisis kinerja aplikasi.

4. Penyimpanan & Keamanan
Sebagian data disimpan di perangkat (mis. riwayat menonton). Data yang dikirim ke server katalog diproses sebatas yang diperlukan untuk layanan. Kami menerapkan langkah wajar untuk melindungi data, namun tidak ada metode transmisi yang 100% aman.

5. Berbagi ke Pihak Ketiga
Kami tidak menjual data pribadi. Data dapat diproses oleh penyedia infrastruktur (hosting API, distribusi update) hanya untuk menjalankan layanan.

6. Hak Pengguna
Kamu dapat meminta akses, koreksi, atau penghapusan data akun dengan menghubungi pengelola aplikasi. Kamu juga dapat menghapus data lokal dengan menghapus data aplikasi / uninstall.

7. Anak di Bawah Umur
Layanan tidak ditujukan untuk anak di bawah usia yang diizinkan hukum setempat tanpa pengawasan orang tua/wali.

8. Perubahan Kebijakan
Kami dapat memperbarui kebijakan ini. Perubahan material akan ditandai dengan tanggal pembaruan di halaman ini.

9. Kontak
Untuk pertanyaan privasi, hubungi pengelola Weeboonime melalui kanal resmi proyek.
""".trimIndent()

private val termsOfServiceBody = """
1. Penerimaan Syarat
Dengan mengunduh, menginstal, atau menggunakan Weeboonime, kamu menyetujui Terms of Service ini. Jika tidak setuju, hentikan penggunaan aplikasi.

2. Deskripsi Layanan
Weeboonime adalah klien mobile untuk menelusuri katalog anime dan memutar episode berdasarkan sumber data yang tersedia. Fitur sosial (Subscribe, Timeline) dapat bersifat pratinjau/dummy.

3. Akun & Login
Login (termasuk Google atau Tester Login) digunakan untuk mengidentifikasi sesi. Kamu bertanggung jawab menjaga keamanan perangkat dan akunmu. Satu akun dapat dibatasi pada sejumlah perangkat tertentu.

4. Penggunaan yang Diizinkan
Kamu setuju untuk:
• menggunakan aplikasi hanya untuk keperluan pribadi yang sah;
• tidak menyalahgunakan, merusak, atau mencoba mengakses sistem secara tidak sah;
• tidak mendistribusikan ulang konten dilindungi hak cipta di luar ketentuan yang berlaku.

5. Konten & Sumber
Katalog dan tautan pemutar dapat berasal dari sumber pihak ketiga. Kami tidak menjamin ketersediaan, kelengkapan, atau legalitas setiap sumber di semua yurisdiksi. Pengguna bertanggung jawab mematuhi hukum setempat.

6. Pembaruan Aplikasi
Aplikasi dapat memeriksa dan menawarkan pembaruan (OTA). Memasang update berarti menerima versi terbaru beserta syarat yang berlaku.

7. Penafian
Layanan disediakan "sebagaimana adanya". Sepanjang diizinkan hukum, kami tidak bertanggung jawab atas kerugian tidak langsung, kehilangan data, atau gangguan layanan.

8. Penghentian
Kami dapat menangguhkan atau menghentikan akses jika terjadi pelanggaran syarat, penyalahgunaan, atau alasan keamanan.

9. Perubahan Syarat
Syarat dapat diperbarui dari waktu ke waktu. Penggunaan berkelanjutan setelah pembaruan dianggap sebagai persetujuan.

10. Hukum yang Berlaku
Syarat ini ditafsirkan sesuai hukum yang berlaku di yurisdiksi pengelola proyek, tanpa mengesampingkan hak konsumen yang wajib.
""".trimIndent()
