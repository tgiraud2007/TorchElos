package com.torchelos.app

import android.app.Application
import com.torchelos.app.core.TorchManager
import com.topjohnwu.superuser.Shell

class TorchApp : Application() {

    lateinit var torchManager: TorchManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )

        torchManager = TorchManager(this)
        torchManager.init()
    }

    companion object {
        lateinit var instance: TorchApp
            private set
    }
}
