@file:Suppress("unused")
package com.learnopengles

import android.app.Application
import timber.log.Timber

class LearnOpenglesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}