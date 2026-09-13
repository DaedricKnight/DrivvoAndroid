package com.artemkhateev.carlog

import android.app.Application
import com.artemkhateev.carlog.data.AppGraph

class CarLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
