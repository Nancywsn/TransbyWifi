package com.example.transbywifi

import android.util.Log

object Logger {

    fun log(any: Any?) {
        Log.e("WifiP2P", any?.toString() ?: "null")
    }

}