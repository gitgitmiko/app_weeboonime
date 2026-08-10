package com.webunime.mobile.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class PremiumPlan(
    val productId: String,
    val title: String,
    val subtitle: String,
    val days: Int,
    val bonusGems: Int,
    val priceLabel: String,
    val savePercent: Int? = null,
    val isBest: Boolean = false,
)

class BillingRepository(context: Context) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext

    private val _purchases = MutableSharedFlow<Purchase>(extraBufferCapacity = 8)
    val purchases: SharedFlow<Purchase> = _purchases

    val plans = listOf(
        PremiumPlan(
            productId = "webunime_premium_1m",
            title = "1 Bulan",
            subtitle = "30 hari premium",
            days = 30,
            bonusGems = 3_000,
            priceLabel = "Rp 12.000",
        ),
        PremiumPlan(
            productId = "webunime_premium_3m",
            title = "3 Bulan",
            subtitle = "90 hari premium",
            days = 90,
            bonusGems = 9_000,
            priceLabel = "Rp 30.000",
            savePercent = 17,
        ),
        PremiumPlan(
            productId = "webunime_premium_6m",
            title = "6 Bulan",
            subtitle = "180 hari premium",
            days = 180,
            bonusGems = 18_000,
            priceLabel = "Rp 55.000",
            savePercent = 24,
            isBest = true,
        ),
        PremiumPlan(
            productId = "webunime_premium_12m",
            title = "12 Bulan",
            subtitle = "360 hari premium",
            days = 360,
            bonusGems = 36_000,
            priceLabel = "Rp 99.000",
            savePercent = 31,
        ),
    )

    private var billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    private var productDetails: List<ProductDetails> = emptyList()

    fun start() {
        if (billingClient.isReady) {
            queryProducts()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryOwned()
                }
            }

            override fun onBillingServiceDisconnected() {
                // reconnect lazily on next purchase attempt
            }
        })
    }

    private fun queryProducts() {
        val products = plans.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsList
            }
        }
    }

    private fun queryOwned() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { _purchases.tryEmit(it) }
            }
        }
    }

    fun launchPlan(activity: Activity, productId: String): Boolean {
        val details = productDetails.firstOrNull { it.productId == productId } ?: return false
        // One-time (INAPP) — sesuai “bukan berlangganan”
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, flow)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    suspend fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        suspendCancellableCoroutine { cont ->
            billingClient.acknowledgePurchase(params) {
                cont.resume(Unit)
            }
        }
    }

    fun planForProduct(productId: String): PremiumPlan? =
        plans.firstOrNull { it.productId == productId }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { _purchases.tryEmit(it) }
        }
    }
}
