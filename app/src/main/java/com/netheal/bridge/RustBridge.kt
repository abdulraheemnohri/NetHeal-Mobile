package com.netheal.bridge

object RustBridge {
    init {
        System.loadLibrary("netheal")
    }

    external fun analyze(domain: String, requests: Int, burst: Float): Boolean
    external fun heal()
}
