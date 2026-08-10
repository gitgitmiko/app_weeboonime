package com.webunime.mobile.ui.auth

import androidx.compose.foundation.background
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
    val barTitle = when (doc) {
        LegalDoc.Privacy -> "Weeboonime - Privacy Policy"
        LegalDoc.Terms -> "Weeboonime - Terms of Service"
    }
    val title = when (doc) {
        LegalDoc.Privacy -> "Privacy Policy"
        LegalDoc.Terms -> "Terms of Service"
    }
    val updated = when (doc) {
        LegalDoc.Privacy -> "Updated at 10-08-2026"
        LegalDoc.Terms -> "Updated at 10-08-2026"
    }
    val body = when (doc) {
        LegalDoc.Privacy -> privacyPolicyBody
        LegalDoc.Terms -> termsOfServiceBody
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(barTitle, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = updated,
                color = Color.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
            )
            Text(
                text = body,
                color = Color.Black,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

private val privacyPolicyBody = """
Weeboonime menghormati privasi pengguna. Kebijakan ini menjelaskan data yang kami kumpulkan dan cara penggunaannya.

Definitions and key terms
• Cookie: File kecil yang dapat disimpan di perangkat untuk preferensi dan analitik ringan.
• Company: Merujuk pada pengelola proyek Weeboonime.
• Device: Ponsel/tablet yang menjalankan aplikasi.
• Personal Data: Informasi yang dapat mengidentifikasi kamu (mis. email Google saat login).

Data yang dikumpulkan
• Informasi akun Google (nama, email, foto profil) jika kamu login.
• Data penggunaan: riwayat menonton, kunci/gem/XP lokal, preferensi.
• Data teknis: jenis perangkat, versi OS, log error, identitas instalasi untuk OTA.

Penggunaan data
Data dipakai untuk menyediakan layanan katalog/streaming, menyimpan progres ekonomi akun, sinkronisasi cloud (Firestore), update aplikasi, dan mencegah penyalahgunaan.

Penyimpanan & keamanan
Sebagian data disimpan di perangkat. Data cloud diproses sebatas kebutuhan layanan. Kami menerapkan langkah wajar, namun tidak ada transmisi yang 100% aman.

Berbagi pihak ketiga
Kami tidak menjual data pribadi. Penyedia infrastruktur (Firebase, hosting API, distribusi APK) dapat memproses data hanya untuk menjalankan layanan.

Hak pengguna
Kamu dapat meminta akses, koreksi, atau penghapusan data akun. Data lokal dapat dihapus lewat clear data / uninstall.

Perubahan kebijakan
Kami dapat memperbarui kebijakan ini. Tanggal pembaruan tercantum di atas.

Kontak
admin@weeboonime.app
""".trimIndent()

private val termsOfServiceBody = """
General Terms
Dengan mengunduh atau menggunakan Weeboonime, kamu menyetujui Terms of Service ini. Jika tidak setuju, hentikan penggunaan aplikasi.

Weeboonime adalah klien mobile untuk menelusuri katalog anime dan memutar episode berdasarkan sumber data yang tersedia. Tim Weeboonime tidak bertanggung jawab atas kerusakan perangkat, kehilangan data, atau gangguan layanan sejauh diizinkan hukum.

Akun & Login
Login Google (atau Tester Login untuk pengujian) mengidentifikasi sesi. Kamu bertanggung jawab menjaga keamanan perangkat dan akun.

Penggunaan
Gunakan aplikasi untuk keperluan pribadi yang sah. Dilarang menyalahgunakan, merusak sistem, atau mendistribusikan ulang konten dilindungi hak cipta di luar ketentuan yang berlaku.

Konten & sumber
Katalog dan tautan pemutar dapat berasal dari sumber pihak ketiga. Kami tidak menjamin ketersediaan atau legalitas di setiap yurisdiksi. Pengguna wajib mematuhi hukum setempat.

Premium, kunci, dan iklan
Fitur ekonomi (kunci, gem, Premium, iklan) dapat berubah. Harga berlangganan mengikuti ketentuan Play Store bila tersedia.

Pembaruan
Aplikasi dapat menawarkan update OTA. Memasang update berarti menerima versi terbaru beserta syarat yang berlaku.

Perubahan syarat
Kami berhak mengubah harga dan kebijakan penggunaan sewaktu-waktu. Penggunaan berkelanjutan setelah pembaruan dianggap sebagai persetujuan.
""".trimIndent()
