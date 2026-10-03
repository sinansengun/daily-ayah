package com.cufica.dailyayah

import android.app.Application
import com.cufica.dailyayah.notification.DailyAyahNotificationScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DailyAyahApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		DailyAyahNotificationScheduler.schedule(this)
	}
}