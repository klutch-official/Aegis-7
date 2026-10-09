package com.aegis7.app.bridge

object AiBridge {

    init {
        System.loadLibrary("aegis_native")
    }

    external fun nativeGetStatus(): String

    fun getStatus(): String {
        return nativeGetStatus()
    }
}
