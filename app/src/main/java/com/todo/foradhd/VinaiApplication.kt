package com.todo.foradhd

import android.app.Application
import com.todo.foradhd.notifications.AlarmScheduler

class VinaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmScheduler.createNotificationChannel(this)
    }
}
