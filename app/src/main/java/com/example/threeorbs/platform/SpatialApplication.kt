package com.example.threeorbs.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.threeorbs.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
