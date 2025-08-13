package com.esurvey.esurvey_sdk_demo

import android.app.Application


/**
 * @author: Jokerliang
 * @Desc:
 * @create: 2024-10-31 15:58
 **/
class App: Application() {

    override fun onCreate() {
        super.onCreate()

    }


}


object AppFlavor {
    const val HU_NAN = "HU_NAN"
    const val GUANG_DONG = "GUANG_DONG"
}