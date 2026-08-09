package com.webunime.mobile

import android.app.Application
import com.webunime.mobile.data.CatalogApi
import com.webunime.mobile.data.WatchHistoryStore

class WebunimeApp : Application() {
    lateinit var catalogApi: CatalogApi
        private set
    lateinit var watchHistory: WatchHistoryStore
        private set

    override fun onCreate() {
        super.onCreate()
        catalogApi = CatalogApi(BuildConfig.CATALOG_API_BASE)
        watchHistory = WatchHistoryStore(this)
    }
}
