package com.webunime.mobile

import android.app.Application

class WebunimeApp : Application() {
    lateinit var catalogApi: com.webunime.mobile.data.CatalogApi
        private set

    override fun onCreate() {
        super.onCreate()
        catalogApi = com.webunime.mobile.data.CatalogApi(BuildConfig.CATALOG_API_BASE)
    }
}
