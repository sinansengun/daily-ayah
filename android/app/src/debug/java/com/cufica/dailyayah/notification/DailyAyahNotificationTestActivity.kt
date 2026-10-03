package com.cufica.dailyayah.notification

import android.app.Activity
import android.os.Bundle

class DailyAyahNotificationTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DailyAyahNotificationPublisher.show(
            this,
            "Bu, Daily Ayah uygulamasından gönderilen bir test bildirimidir.",
            "Günün Ayeti"
        )
        finish()
    }
}