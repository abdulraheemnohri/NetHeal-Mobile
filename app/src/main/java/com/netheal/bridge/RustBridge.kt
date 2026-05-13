package com.netheal.bridge

object RustBridge {
    init {
        System.loadLibrary("netheal")
    }

    external fun analyze(domain: String, requests: Int): Boolean
    external fun heal()
}
