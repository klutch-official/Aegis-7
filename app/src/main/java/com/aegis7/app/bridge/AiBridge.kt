package com.aegis7.app.bridge

object AiBridge {

    init {
        System.loadLibrary("aegis_native")
    }

    external fun nativeGetStatus(): String

    external fun nativeLoadModel(modelPath: String): String

    external fun nativeGenerate(prompt: String): String

    external fun nativeUnloadModel()

    fun getStatus(): String {
        return nativeGetStatus()
    }

    fun loadModel(modelPath: String): String {
        return nativeLoadModel(modelPath)
    }

    fun generate(prompt: String): String {
        return nativeGenerate(prompt)
    }

    fun unloadModel() {
        nativeUnloadModel()
    }
}
