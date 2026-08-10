package com.webunime.mobile.data.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.webunime.mobile.BuildConfig
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    val isFirebaseReady: Boolean get() = auth != null

    val currentUser get() = auth?.currentUser

    fun isSignedIn(): Boolean = currentUser != null

    fun googleSignInClient(activity: Activity): GoogleSignInClient? {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (webClientId.isEmpty()) return null
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun signInIntent(activity: Activity): Intent? =
        googleSignInClient(activity)?.signInIntent

    /** Hapus sesi Google lama supaya picker + token selalu fresh. */
    suspend fun prepareSignIn(activity: Activity) {
        runCatching { googleSignInClient(activity)?.signOut()?.await() }
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Result<Unit> {
        val a = auth ?: return Result.failure(
            IllegalStateException("Firebase Auth belum siap. Cek google-services.json."),
        )
        return try {
            if (data == null) {
                return Result.failure(IllegalStateException("Login dibatalkan atau data kosong."))
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                Log.e(TAG, "idToken null — Web Client ID salah atau bukan tipe Web")
                return Result.failure(
                    IllegalStateException(
                        "ID token kosong. Pastikan GOOGLE_WEB_CLIENT_ID adalah " +
                            "OAuth client tipe Web (bukan Android) dari Firebase/Google Cloud.",
                    ),
                )
            }
            val credential = GoogleAuthProvider.getCredential(token, null)
            a.signInWithCredential(credential).await()
            Log.i(TAG, "Firebase login OK uid=${a.currentUser?.uid}")
            Result.success(Unit)
        } catch (e: ApiException) {
            Log.e(TAG, "GoogleSignIn ApiException status=${e.statusCode}", e)
            Result.failure(IllegalStateException(humanizeApiException(e), e))
        } catch (e: Exception) {
            Log.e(TAG, "GoogleSignIn failed", e)
            Result.failure(
                IllegalStateException(e.message ?: e.javaClass.simpleName, e),
            )
        }
    }

    suspend fun signOut(activity: Activity) {
        auth?.signOut()
        runCatching { googleSignInClient(activity)?.signOut()?.await() }
    }

    private fun humanizeApiException(e: ApiException): String {
        return when (e.statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR, 10 ->
                "Konfigurasi OAuth salah (kode 10). Cek SHA-1 debug + Web Client ID di Firebase."
            CommonStatusCodes.NETWORK_ERROR ->
                "Jaringan error saat login Google."
            CommonStatusCodes.CANCELED, 12501 ->
                "Login Google dibatalkan."
            12500 ->
                "Login Google gagal (12500). Coba lagi / restart app."
            else ->
                "Login Google gagal (kode ${e.statusCode}): ${e.message ?: "-"}"
        }
    }

    companion object {
        private const val TAG = "WebunimeAuth"
    }
}
