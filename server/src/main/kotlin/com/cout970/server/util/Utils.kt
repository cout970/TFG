package com.cout970.server.util


inline fun (() -> Unit).ifFail(func: () -> Unit){
    try {
        this()
    }catch (e: Exception){
        e.printStackTrace()
        func()
    }
}