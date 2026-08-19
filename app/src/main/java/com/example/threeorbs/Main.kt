package com.example.threeorbs

import com.example.threeorbs.ui.today.ThreeOrbsScreen
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme {
                ThreeOrbsScreen()
            }
        }
    }
